package net.mackenziemolloy.shopguiplus.sellgui.gui;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import net.mackenziemolloy.shopguiplus.sellgui.config.PluginConfig;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SellGuiHolder implements InventoryHolder {

    private static final long PREVIEW_COOLDOWN_MILLIS = 1500L;

    private final UUID playerId;
    private final PluginConfig config;
    private final boolean[] lockedSlots;
    private final boolean hasLockedSlots;
    private final AtomicBoolean settled;

    private Inventory inventory;
    private ItemStack lastPreview;
    private long lastPreviewAt;

    public SellGuiHolder(@NotNull UUID playerId, @NotNull PluginConfig config) {
        this.playerId = playerId;
        this.config = config;
        this.lockedSlots = config.lockedSlots();
        this.hasLockedSlots = !config.decorations().isEmpty();
        this.settled = new AtomicBoolean();
    }

    public @NotNull UUID playerId() {
        return this.playerId;
    }

    public @NotNull PluginConfig config() {
        return this.config;
    }

    public void attach(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public boolean isLocked(int slot) {
        return slot >= 0 && slot < this.lockedSlots.length && this.lockedSlots[slot];
    }

    public boolean hasLockedSlots() {
        return this.hasLockedSlots;
    }

    public boolean markSettled() {
        return this.settled.compareAndSet(false, true);
    }

    public boolean isSettled() {
        return this.settled.get();
    }

    public boolean shouldPreview(@Nullable ItemStack stack) {
        if (stack == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (this.lastPreview != null && (now - this.lastPreviewAt) < PREVIEW_COOLDOWN_MILLIS
                && this.lastPreview.isSimilar(stack)) {
            return false;
        }

        this.lastPreview = stack.asOne();
        this.lastPreviewAt = now;
        return true;
    }
}
