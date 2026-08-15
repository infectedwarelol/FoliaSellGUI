package net.mackenziemolloy.shopguiplus.sellgui.gui;

import net.mackenziemolloy.shopguiplus.sellgui.compat.Schedulers;
import net.mackenziemolloy.shopguiplus.sellgui.config.DecorationItem;
import net.mackenziemolloy.shopguiplus.sellgui.shulker.ShulkerService;
import net.mackenziemolloy.shopguiplus.sellgui.transaction.SellTransactionService;
import net.mackenziemolloy.shopguiplus.sellgui.transaction.ShulkerEstimate;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Messages;
import net.mackenziemolloy.shopguiplus.sellgui.utility.TextUtility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SellGuiListener implements Listener {

    private final SellGuiManager manager;
    private final SellTransactionService transactionService;
    private final ShulkerService shulkerService;
    private final Messages messages;
    private final Schedulers schedulers;

    public SellGuiListener(@NotNull SellGuiManager manager, @NotNull SellTransactionService transactionService,
                           @NotNull ShulkerService shulkerService, @NotNull Messages messages,
                           @NotNull Schedulers schedulers) {
        this.manager = manager;
        this.transactionService = transactionService;
        this.shulkerService = shulkerService;
        this.messages = messages;
        this.schedulers = schedulers;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof SellGuiHolder holder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)
                || !player.getUniqueId().equals(holder.playerId())
                || holder.isSettled()) {
            event.setCancelled(true);
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        int topSize = topInventory.getSize();
        int rawSlot = event.getRawSlot();
        InventoryAction action = event.getAction();
        ClickType click = event.getClick();

        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            if (holder.hasLockedSlots()
                    && Inventories.containsSimilarInLockedSlots(topInventory, holder, event.getCursor())) {
                event.setCancelled(true);
            }

            return;
        }

        boolean insideGui = (rawSlot >= 0 && rawSlot < topSize);

        if (insideGui && (click == ClickType.UNKNOWN || action == InventoryAction.UNKNOWN)) {
            event.setCancelled(true);
            return;
        }

        if (insideGui && holder.isLocked(rawSlot)) {
            event.setCancelled(true);
            runDecoration(player, holder, rawSlot);
            return;
        }

        if (insideGui) {
            preview(player, holder, event.getCursor());
            return;
        }

        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            moveIntoGui(player, holder, topInventory, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (!(topInventory.getHolder() instanceof SellGuiHolder holder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)
                || !player.getUniqueId().equals(holder.playerId())
                || holder.isSettled()) {
            event.setCancelled(true);
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        int topSize = topInventory.getSize();
        boolean touchesGui = false;

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= topSize) {
                continue;
            }

            if (holder.isLocked(rawSlot)) {
                event.setCancelled(true);
                return;
            }

            touchesGui = true;
        }

        if (touchesGui) {
            preview(player, holder, event.getOldCursor());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellGuiHolder holder)) {
            return;
        }

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        this.manager.handleClose(player, holder, event.getReason());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        this.manager.handleQuit(event.getPlayer());
    }

    private void moveIntoGui(@NotNull Player player, @NotNull SellGuiHolder holder, @NotNull Inventory topInventory,
                             @NotNull InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir() || current.getAmount() <= 0) {
            return;
        }

        ItemStack moving = current.clone();
        ItemStack leftover = Inventories.addToUnlockedSlots(topInventory, holder, moving);

        if (leftover != null && leftover.getAmount() == moving.getAmount()) {
            return;
        }

        event.setCurrentItem(leftover);
        preview(player, holder, moving);
        player.updateInventory();
    }

    private void runDecoration(@NotNull Player player, @NotNull SellGuiHolder holder, int slot) {
        DecorationItem decoration = holder.config().decorationAt(slot);
        if (decoration == null || !decoration.hasActions()) {
            return;
        }

        String playerName = player.getName();

        for (String consoleCommand : decoration.consoleCommands()) {
            String command = consoleCommand.replace("%PLAYER%", playerName);
            this.schedulers.global(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }

        for (String playerCommand : decoration.playerCommands()) {
            String command = playerCommand.replace("%PLAYER%", playerName);
            this.schedulers.onEntityNextTick(player, () -> player.performCommand(command));
        }

        if (decoration.sellInventory()) {
            this.manager.reopenLater(player);
        }
    }

    private void preview(@NotNull Player player, @NotNull SellGuiHolder holder, @Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !this.shulkerService.isShulkerBox(stack)) {
            return;
        }

        if (this.messages.isBlank("shulker_estimate") || !holder.shouldPreview(stack)) {
            return;
        }

        ShulkerEstimate estimate = this.transactionService.estimate(player, stack);
        if (estimate == null) {
            return;
        }

        String itemName = TextUtility.itemName(stack);
        String price = estimate.formattedPrice();
        String amount = Long.toString(estimate.itemCount());

        this.messages.send(player, "shulker_estimate", message -> message
                .replace("{item}", itemName)
                .replace("{price}", price)
                .replace("{amount}", amount));
    }
}
