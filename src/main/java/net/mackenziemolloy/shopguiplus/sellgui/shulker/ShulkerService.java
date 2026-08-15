package net.mackenziemolloy.shopguiplus.sellgui.shulker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.brcdev.shopgui.provider.economy.EconomyProvider;
import net.brcdev.shopgui.shop.item.ShopItem;
import net.mackenziemolloy.shopguiplus.sellgui.config.ShulkerOptions;
import net.mackenziemolloy.shopguiplus.sellgui.config.ShulkerOptions.ReceiptMode;
import net.mackenziemolloy.shopguiplus.sellgui.shop.ItemGroup;
import net.mackenziemolloy.shopguiplus.sellgui.shop.PricingContext;
import net.mackenziemolloy.shopguiplus.sellgui.shop.ShopService;
import net.mackenziemolloy.shopguiplus.sellgui.transaction.ReceiptLine;
import net.mackenziemolloy.shopguiplus.sellgui.transaction.Valuation;
import net.mackenziemolloy.shopguiplus.sellgui.utility.TextUtility;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ShulkerService {

    private static final Set<Material> SHULKER_BOXES = createShulkerBoxSet();

    private final ShopService shopService;

    public ShulkerService(@NotNull ShopService shopService) {
        this.shopService = shopService;
    }

    public boolean isShulkerBox(@NotNull Material material) {
        return SHULKER_BOXES.contains(material);
    }

    public boolean isShulkerBox(@Nullable ItemStack item) {
        return item != null && SHULKER_BOXES.contains(item.getType());
    }

    public @Nullable Valuation appraise(@NotNull ItemStack box, @NotNull PricingContext context,
                                        @NotNull ShulkerOptions options) {
        if (!options.enabled() || !isShulkerBox(box)) {
            return null;
        }

        return appraise(box, context, options, 1);
    }

    private @Nullable Valuation appraise(@NotNull ItemStack box, @NotNull PricingContext context,
                                         @NotNull ShulkerOptions options, int depth) {
        if (depth > options.maxDepth()) {
            return null;
        }

        ItemStack[] contents = readContents(box);
        if (contents == null) {
            return null;
        }

        int count = Math.max(1, box.getAmount());
        boolean detailed = (options.receiptMode() == ReceiptMode.DETAILED);

        Valuation valuation = new Valuation();
        Map<ItemStack, ItemGroup> groups = new LinkedHashMap<>();
        List<ItemStack> leftovers = new ArrayList<>();
        boolean sold = false;

        for (ItemStack content : contents) {
            if (content == null || content.getType().isAir() || content.getAmount() <= 0) {
                continue;
            }

            if (!context.consume(1)) {
                return null;
            }

            if (depth < options.maxDepth() && isShulkerBox(content)) {
                Valuation nested = appraise(content, context, options, depth + 1);
                if (nested != null) {
                    valuation.payouts().addAll(nested.payouts());
                    valuation.addSold(nested.soldCount());

                    if (detailed) {
                        for (ReceiptLine line : nested.lines()) {
                            valuation.addLine(line);
                        }
                    }

                    leftovers.addAll(nested.returnedItems());
                    sold = true;
                    continue;
                }
            }

            ItemStack key = content.asOne();
            ItemGroup group = groups.get(key);
            if (group == null) {
                group = new ItemGroup(key);
                groups.put(key, group);
            }

            group.add(content);
        }

        for (ItemGroup group : groups.values()) {
            ShopItem shopItem = context.shopItem(group.key());
            if (shopItem == null) {
                leftovers.addAll(group.sources());
                continue;
            }

            double price = context.sellPrice(shopItem, group.cappedAmount());
            if (price <= 0.0D) {
                leftovers.addAll(group.sources());
                continue;
            }

            EconomyProvider provider = this.shopService.provider(shopItem.getShop());
            if (provider == null) {
                leftovers.addAll(group.sources());
                continue;
            }

            valuation.payouts().add(provider, price);
            valuation.addSold(group.amount());
            sold = true;

            if (detailed) {
                valuation.addLine(new ReceiptLine(TextUtility.itemName(group.key()), group.amount(), price, provider));
            }
        }

        if (!sold) {
            return null;
        }

        switch (options.unsellablePolicy()) {
            case RETURN_BOX -> {
                if (!leftovers.isEmpty()) {
                    return null;
                }
            }
            case DISCARD -> leftovers.clear();
            case KEEP_ITEMS -> {
            }
        }

        ItemStack leftoverBox = null;
        if (!leftovers.isEmpty()) {
            if (count > 1) {
                return null;
            }

            leftoverBox = rebuild(box, leftovers);
            if (leftoverBox == null) {
                return null;
            }
        }

        if (leftoverBox == null && options.includeContainerPrice()) {
            appraiseContainer(box, context, valuation, detailed);
        }

        if (!detailed) {
            valuation.lines().clear();
            String boxName = TextUtility.itemName(box);

            for (Map.Entry<EconomyProvider, Double> entry : valuation.payouts().asMap().entrySet()) {
                valuation.addLine(new ReceiptLine(boxName, 1L, entry.getValue(), entry.getKey()));
            }
        }

        valuation.scale(count);

        if (leftoverBox != null) {
            leftoverBox.setAmount(count);
            valuation.addReturnedItem(leftoverBox);
        }

        return valuation;
    }

    private void appraiseContainer(@NotNull ItemStack box, @NotNull PricingContext context,
                                   @NotNull Valuation valuation, boolean detailed) {
        ItemStack container = stripContents(box);

        ShopItem shopItem = context.shopItem(container);
        if (shopItem == null) {
            return;
        }

        double price = context.sellPrice(shopItem, 1);
        if (price <= 0.0D) {
            return;
        }

        EconomyProvider provider = this.shopService.provider(shopItem.getShop());
        if (provider == null) {
            return;
        }

        valuation.payouts().add(provider, price);
        valuation.addSold(1L);

        if (detailed) {
            valuation.addLine(new ReceiptLine(TextUtility.materialName(box.getType()), 1L, price, provider));
        }
    }

    private static @Nullable ItemStack[] readContents(@NotNull ItemStack box) {
        Container container = readContainer(box);
        if (container == null) {
            return null;
        }

        try {
            return container.getInventory().getContents();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @Nullable Container readContainer(@NotNull ItemStack box) {
        ItemMeta meta = box.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return null;
        }

        BlockState state;
        try {
            state = blockStateMeta.getBlockState();
        } catch (Throwable ignored) {
            return null;
        }

        return (state instanceof Container container ? container : null);
    }

    private static @Nullable ItemStack rebuild(@NotNull ItemStack box, @NotNull List<ItemStack> leftovers) {
        ItemStack result = box.clone();
        result.setAmount(1);

        ItemMeta meta = result.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return null;
        }

        BlockState state;
        try {
            state = blockStateMeta.getBlockState();
        } catch (Throwable ignored) {
            return null;
        }

        if (!(state instanceof Container container)) {
            return null;
        }

        try {
            Inventory inventory = container.getInventory();
            int size = inventory.getSize();
            if (leftovers.size() > size) {
                return null;
            }

            ItemStack[] replacement = new ItemStack[size];
            for (int index = 0; index < leftovers.size(); index++) {
                replacement[index] = leftovers.get(index);
            }

            inventory.setContents(replacement);
            blockStateMeta.setBlockState(state);
            result.setItemMeta(blockStateMeta);
            return result;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static @NotNull ItemStack stripContents(@NotNull ItemStack box) {
        ItemStack stripped = box.asOne();

        ItemMeta meta = stripped.getItemMeta();
        if (meta instanceof BlockStateMeta blockStateMeta) {
            try {
                if (blockStateMeta.hasBlockState()) {
                    blockStateMeta.clearBlockState();
                    stripped.setItemMeta(blockStateMeta);
                }
            } catch (Throwable ignored) {
                return stripped;
            }
        }

        return stripped;
    }

    private static @NotNull Set<Material> createShulkerBoxSet() {
        EnumSet<Material> materials = EnumSet.noneOf(Material.class);

        for (Material material : Material.values()) {
            String name = material.name();
            if (name.startsWith("LEGACY_") || !name.endsWith("SHULKER_BOX")) {
                continue;
            }

            materials.add(material);
        }

        return Collections.unmodifiableSet(materials);
    }
}
