package net.mackenziemolloy.shopguiplus.sellgui.gui;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.ShopGuiPlugin;
import net.mackenziemolloy.shopguiplus.sellgui.compat.Schedulers;
import net.mackenziemolloy.shopguiplus.sellgui.config.DecorationItem;
import net.mackenziemolloy.shopguiplus.sellgui.config.PluginConfig;
import net.mackenziemolloy.shopguiplus.sellgui.shop.ShopService;
import net.mackenziemolloy.shopguiplus.sellgui.transaction.SellTransactionService;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Messages;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Sounds;
import net.mackenziemolloy.shopguiplus.sellgui.utility.TextUtility;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SellGuiManager {

    private final Supplier<PluginConfig> configSupplier;
    private final Schedulers schedulers;
    private final SellTransactionService transactionService;
    private final ShopService shopService;
    private final Messages messages;
    private final Logger logger;
    private final Map<UUID, SellGuiHolder> openGuis;

    public SellGuiManager(@NotNull Supplier<PluginConfig> configSupplier, @NotNull Schedulers schedulers,
                          @NotNull SellTransactionService transactionService, @NotNull ShopService shopService,
                          @NotNull Messages messages, @NotNull Logger logger) {
        this.configSupplier = configSupplier;
        this.schedulers = schedulers;
        this.transactionService = transactionService;
        this.shopService = shopService;
        this.messages = messages;
        this.logger = logger;
        this.openGuis = new ConcurrentHashMap<>();
    }

    public boolean open(@NotNull Player player) {
        PluginConfig config = this.configSupplier.get();
        if (config == null) {
            return false;
        }

        if (!isGameModeAllowed(player)) {
            return false;
        }

        if (!this.shopService.isPlayerDataReady(player)) {
            this.messages.send(player, "shop_not_ready");
            return false;
        }

        UUID playerId = player.getUniqueId();

        InventoryHolder currentHolder = player.getOpenInventory().getTopInventory().getHolder();
        if (currentHolder instanceof SellGuiHolder) {
            player.closeInventory();
        }

        SellGuiHolder stale = this.openGuis.remove(playerId);
        if (stale != null) {
            this.transactionService.settle(player, stale, InventoryCloseEvent.Reason.PLUGIN);
        }

        SellGuiHolder holder = new SellGuiHolder(playerId, config);
        Inventory inventory = Bukkit.createInventory(holder, config.size(), config.guiTitle());
        holder.attach(inventory);

        for (DecorationItem decoration : config.decorations()) {
            inventory.setItem(decoration.slot(), decoration.item());
        }

        this.openGuis.put(playerId, holder);

        try {
            player.openInventory(inventory);
        } catch (Throwable throwable) {
            this.openGuis.remove(playerId, holder);
            this.logger.log(Level.SEVERE, "Failed to open the Sell GUI for " + player.getName() + ".", throwable);
            return false;
        }

        Sounds.play(config, player, "open");
        return true;
    }

    public void handleClose(@NotNull Player player, @NotNull SellGuiHolder holder,
                            @Nullable InventoryCloseEvent.Reason reason) {
        if (!player.getUniqueId().equals(holder.playerId())) {
            return;
        }

        this.openGuis.remove(holder.playerId(), holder);
        this.transactionService.settle(player, holder, reason);
    }

    public void handleQuit(@NotNull Player player) {
        SellGuiHolder holder = this.openGuis.remove(player.getUniqueId());
        if (holder == null) {
            return;
        }

        this.transactionService.settle(player, holder, InventoryCloseEvent.Reason.DISCONNECT);
    }

    public void reopenLater(@NotNull Player player) {
        this.schedulers.onEntityNextTick(player, () -> {
            player.closeInventory();
            open(player);
        });
    }

    public void shutdown() {
        for (Map.Entry<UUID, SellGuiHolder> entry : this.openGuis.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }

            try {
                this.transactionService.settle(player, entry.getValue(), InventoryCloseEvent.Reason.PLUGIN);
            } catch (Throwable throwable) {
                this.logger.log(Level.SEVERE, "Failed to settle the Sell GUI of " + player.getName()
                        + " during shutdown.", throwable);
            }

            try {
                player.closeInventory();
            } catch (Throwable ignored) {
            }
        }

        this.openGuis.clear();
    }

    private boolean isGameModeAllowed(@NotNull Player player) {
        List<String> disabledGameModes;

        try {
            ShopGuiPlugin shopGui = ShopGuiPlusApi.getPlugin();
            if (shopGui == null) {
                return true;
            }

            FileConfiguration configuration = shopGui.getConfigMain().getConfig();
            disabledGameModes = configuration.getStringList("disableShopsInGamemodes");
        } catch (Throwable ignored) {
            return true;
        }

        if (disabledGameModes.isEmpty()) {
            return true;
        }

        GameMode gameMode = player.getGameMode();
        if (!disabledGameModes.contains(gameMode.name())) {
            return true;
        }

        String formatted = TextUtility.capitalize(gameMode.name().toLowerCase(Locale.ROOT));
        this.messages.send(player, "gamemode_not_allowed", message -> message.replace("{gamemode}", formatted));
        return false;
    }
}
