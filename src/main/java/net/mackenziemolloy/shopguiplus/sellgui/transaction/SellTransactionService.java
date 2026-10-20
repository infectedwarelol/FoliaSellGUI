package net.mackenziemolloy.shopguiplus.sellgui.transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.brcdev.shopgui.provider.economy.EconomyProvider;
import net.brcdev.shopgui.shop.item.ShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import net.mackenziemolloy.shopguiplus.sellgui.config.PluginConfig;
import net.mackenziemolloy.shopguiplus.sellgui.config.ShulkerOptions;
import net.mackenziemolloy.shopguiplus.sellgui.gui.SellGuiHolder;
import net.mackenziemolloy.shopguiplus.sellgui.shop.EconomyService;
import net.mackenziemolloy.shopguiplus.sellgui.shop.ItemGroup;
import net.mackenziemolloy.shopguiplus.sellgui.shop.PricingContext;
import net.mackenziemolloy.shopguiplus.sellgui.shop.ShopService;
import net.mackenziemolloy.shopguiplus.sellgui.shulker.ShulkerService;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Colors;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Messages;
import net.mackenziemolloy.shopguiplus.sellgui.utility.NumberFormatter;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Sounds;
import net.mackenziemolloy.shopguiplus.sellgui.utility.TextUtility;
import net.mackenziemolloy.shopguiplus.sellgui.utility.TransactionLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SellTransactionService {

    private final Supplier<PluginConfig> configSupplier;
    private final ShopService shopService;
    private final EconomyService economyService;
    private final ShulkerService shulkerService;
    private final ItemReturnService itemReturnService;
    private final Messages messages;
    private final TransactionLogger transactionLogger;
    private final Logger logger;

    public SellTransactionService(@NotNull Supplier<PluginConfig> configSupplier, @NotNull ShopService shopService,
                                  @NotNull EconomyService economyService, @NotNull ShulkerService shulkerService,
                                  @NotNull ItemReturnService itemReturnService, @NotNull Messages messages,
                                  @NotNull TransactionLogger transactionLogger, @NotNull Logger logger) {
        this.configSupplier = configSupplier;
        this.shopService = shopService;
        this.economyService = economyService;
        this.shulkerService = shulkerService;
        this.itemReturnService = itemReturnService;
        this.messages = messages;
        this.transactionLogger = transactionLogger;
        this.logger = logger;
    }

    public void settle(@NotNull Player player, @NotNull SellGuiHolder holder,
                       @Nullable InventoryCloseEvent.Reason reason) {
        if (!holder.markSettled()) {
            return;
        }

        PluginConfig config = this.configSupplier.get();
        Inventory inventory = holder.getInventory();
        if (config == null || inventory == null) {
            return;
        }

        boolean forceDrop = (reason == InventoryCloseEvent.Reason.DEATH);
        List<ItemStack> unpaidItems = new ArrayList<>(inventory.getSize());
        boolean paid = false;

        try {
            extract(inventory, holder, unpaidItems);

            if (unpaidItems.isEmpty()) {
                Sounds.play(config, player, "failed");
                this.messages.send(player, "no_items_in_gui");
                return;
            }

            PricingContext context = this.shopService.newContext(player, config.shulkerOptions().maxScannedItems());
            Valuation valuation = appraise(unpaidItems, context, config);

            if (!valuation.hasSales()) {
                returnItems(player, unpaidItems, forceDrop);
                Sounds.play(config, player, "failed");
                this.messages.send(player, "no_items_sold");
                return;
            }

            paid = this.economyService.payout(player, valuation.payouts());
            if (!paid) {
                returnItems(player, unpaidItems, forceDrop);
                Sounds.play(config, player, "failed");
                this.messages.send(player, "transaction_failed");
                return;
            }

            unpaidItems.clear();
            completeSale(player, valuation, config, forceDrop);
        } catch (Throwable throwable) {
            this.logger.log(Level.SEVERE, "The sell transaction for " + player.getName() + " failed.", throwable);

            if (!paid) {
                try {
                    returnItems(player, unpaidItems, forceDrop);
                    this.messages.send(player, "transaction_failed");
                } catch (Throwable returnFailure) {
                    this.logger.log(Level.SEVERE, "Failed to return items to " + player.getName()
                            + " after a failed transaction.", returnFailure);
                }
            }
        } finally {
            try {
                player.updateInventory();
            } catch (Throwable ignored) {
            }
        }
    }

    public @Nullable ShulkerEstimate estimate(@NotNull Player player, @NotNull ItemStack box) {
        PluginConfig config = this.configSupplier.get();
        if (config == null) {
            return null;
        }

        ShulkerOptions options = config.shulkerOptions();
        if (!options.enabled() || !options.showEstimate() || !this.shulkerService.isShulkerBox(box)) {
            return null;
        }

        try {
            PricingContext context = this.shopService.newContext(player, options.maxScannedItems());
            Valuation valuation = this.shulkerService.appraise(box, context, options);
            if (valuation == null || !valuation.hasSales()) {
                return null;
            }

            String formatted = formatPayouts(valuation.payouts(), config.numberFormatter());
            return new ShulkerEstimate(formatted, valuation.soldCount());
        } catch (Throwable throwable) {
            this.logger.log(Level.WARNING, "Failed to estimate a shulker box value for " + player.getName() + ".",
                    throwable);
            return null;
        }
    }

    private void extract(@NotNull Inventory inventory, @NotNull SellGuiHolder holder,
                         @NotNull List<ItemStack> target) {
        ItemStack[] contents = inventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            if (holder.isLocked(slot)) {
                continue;
            }

            ItemStack stack = contents[slot];
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }

            target.add(stack.clone());
            inventory.setItem(slot, null);
        }
    }

    private @NotNull Valuation appraise(@NotNull List<ItemStack> extracted, @NotNull PricingContext context,
                                        @NotNull PluginConfig config) {
        ShulkerOptions shulkerOptions = config.shulkerOptions();
        Valuation total = new Valuation();
        Map<ItemStack, ItemGroup> looseGroups = new LinkedHashMap<>();

        for (ItemStack stack : extracted) {
            if (!context.consume(1)) {
                total.addReturnedItem(stack);
                continue;
            }

            if (shulkerOptions.enabled() && this.shulkerService.isShulkerBox(stack)) {
                Valuation boxValuation = this.shulkerService.appraise(stack, context, shulkerOptions);
                if (boxValuation != null) {
                    total.merge(boxValuation);
                    continue;
                }
            }

            ItemStack key = stack.asOne();
            looseGroups.computeIfAbsent(key, ItemGroup::new).add(stack);
        }

        for (ItemGroup group : looseGroups.values()) {
            ShopItem shopItem = context.shopItem(group.key());
            if (shopItem == null) {
                returnGroup(total, group);
                continue;
            }

            double price = context.sellPrice(shopItem, group.cappedAmount());
            if (price <= 0.0D) {
                returnGroup(total, group);
                continue;
            }

            EconomyProvider provider = this.shopService.provider(shopItem.getShop());
            if (provider == null) {
                returnGroup(total, group);
                continue;
            }

            total.payouts().add(provider, price);
            total.addSold(group.amount());
            total.addLine(new ReceiptLine(TextUtility.itemName(group.key()), group.amount(), price, provider));
        }

        return total;
    }

    private void returnGroup(@NotNull Valuation valuation, @NotNull ItemGroup group) {
        for (ItemStack source : group.sources()) {
            valuation.addReturnedItem(source);
        }
    }

    private void completeSale(@NotNull Player player, @NotNull Valuation valuation, @NotNull PluginConfig config,
                              boolean forceDrop) {
        try {
            returnItems(player, valuation.returnedItems(), forceDrop);
        } catch (Throwable throwable) {
            this.logger.log(Level.SEVERE, "Failed to return leftover items to " + player.getName() + ".", throwable);
        }

        try {
            announce(player, valuation, config);
        } catch (Throwable throwable) {
            this.logger.log(Level.SEVERE, "Failed to send the sell receipt to " + player.getName() + ".", throwable);
        }
    }

    private void returnItems(@NotNull Player player, @NotNull List<ItemStack> items, boolean forceDrop) {
        if (items.isEmpty()) {
            return;
        }

        boolean dropped = this.itemReturnService.returnItems(player, items, forceDrop);
        if (dropped && !forceDrop) {
            this.messages.send(player, "inventory_full");
        }
    }

    private void announce(@NotNull Player player, @NotNull Valuation valuation, @NotNull PluginConfig config) {
        Sounds.play(config, player, "success");

        NumberFormatter formatter = config.numberFormatter();
        String earnings = formatPayouts(valuation.payouts(), formatter);
        String soldCount = formatter.count(valuation.soldCount());

        boolean loggerEnabled = this.transactionLogger.isEnabled();
        boolean needsList = (config.receiptType() == 1) || loggerEnabled
                || this.messages.raw("items_sold").contains("{list}");

        List<String> receiptLines = new ArrayList<>();
        List<String> itemNames = new ArrayList<>();

        if (needsList) {
            for (ReceiptLine line : valuation.lines()) {
                String price = formatMoney(line.provider(), line.price(), formatter);
                String amount = formatter.count(line.amount());
                String itemName = line.itemName();

                receiptLines.add(this.messages.legacy("receipt_item_layout", message -> message
                        .replace("{amount}", amount)
                        .replace("{item}", itemName)
                        .replace("{price}", price)));
                itemNames.add(itemName);
            }
        }

        String itemList = String.join(", ", itemNames);
        UnaryOperator<String> soldReplacer = message -> message
                .replace("{earning}", earnings)
                .replace("{receipt}", "")
                .replace("{list}", itemList)
                .replace("{amount}", soldCount);

        if (config.receiptType() == 1) {
            sendReceiptMessage(player, soldReplacer, receiptLines);
        } else {
            this.messages.send(player, "items_sold", soldReplacer);
        }

        if (loggerEnabled) {
            String joined = Colors.stripColor(String.join(", ", receiptLines));
            this.transactionLogger.log(player.getName() + " (" + player.getUniqueId() + ") sold: {" + joined + "}");
        }

        UnaryOperator<String> summaryReplacer = message -> message
                .replace("{earning}", earnings)
                .replace("{amount}", soldCount);

        if (config.sellTitles() && (!this.messages.raw("sell_title").isEmpty()
                || !this.messages.raw("sell_subtitle").isEmpty())) {
            Component title = this.messages.component("sell_title", summaryReplacer);
            Component subtitle = this.messages.component("sell_subtitle", summaryReplacer);
            player.showTitle(Title.title(title, subtitle));
        }

        if (config.actionBarMessages() && !this.messages.raw("action_bar_items_sold").isEmpty()) {
            player.sendActionBar(this.messages.component("action_bar_items_sold", summaryReplacer));
        }
    }

    private void sendReceiptMessage(@NotNull Player player, @NotNull UnaryOperator<String> soldReplacer,
                                    @NotNull List<String> receiptLines) {
        boolean hasItemsSold = !this.messages.raw("items_sold").isEmpty();
        boolean hasReceiptText = !this.messages.raw("receipt_text").isEmpty();

        if (!hasItemsSold && !hasReceiptText) {
            return;
        }

        Component message = null;
        if (hasItemsSold) {
            message = this.messages.component("items_sold", soldReplacer);
        }

        if (hasReceiptText) {
            TextComponent.Builder hoverText = Component.text()
                    .append(Colors.fromLegacy(this.messages.legacy("receipt_title", null)));
            for (String receiptLine : receiptLines) {
                hoverText.append(Component.newline()).append(Colors.fromLegacy(receiptLine));
            }

            Component receipt = this.messages.component("receipt_text", null)
                    .hoverEvent(HoverEvent.showText(hoverText.build()));

            message = (message == null ? receipt : message.append(Component.space()).append(receipt));
        }

        player.sendMessage(message);
    }

    private @NotNull String formatPayouts(@NotNull Payouts payouts, @NotNull NumberFormatter formatter) {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<EconomyProvider, Double> entry : payouts.asMap().entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }

            builder.append(formatMoney(entry.getKey(), entry.getValue(), formatter));
        }

        return builder.toString();
    }

    private @NotNull String formatMoney(@NotNull EconomyProvider provider, double amount,
                                        @NotNull NumberFormatter formatter) {
        String prefix = "";
        String suffix = "";

        try {
            String providerPrefix = provider.getCurrencyPrefix();
            if (providerPrefix != null) {
                prefix = providerPrefix;
            }

            String providerSuffix = provider.getCurrencySuffix();
            if (providerSuffix != null) {
                suffix = providerSuffix;
            }
        } catch (Throwable ignored) {
        }

        return prefix + formatter.money(amount) + suffix;
    }
}
