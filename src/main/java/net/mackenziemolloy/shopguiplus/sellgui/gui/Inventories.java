package net.mackenziemolloy.shopguiplus.sellgui.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Inventories {

    private Inventories() {
        throw new UnsupportedOperationException();
    }

    public static @Nullable ItemStack addToUnlockedSlots(@NotNull Inventory inventory, @NotNull SellGuiHolder holder,
                                                         @NotNull ItemStack stack) {
        ItemStack[] contents = inventory.getContents();
        int remaining = stack.getAmount();
        int maxStackSize = Math.max(1, stack.getMaxStackSize());

        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            if (holder.isLocked(slot)) {
                continue;
            }

            ItemStack existing = contents[slot];
            if (existing == null || existing.getType().isAir() || !existing.isSimilar(stack)) {
                continue;
            }

            int space = (maxStackSize - existing.getAmount());
            if (space <= 0) {
                continue;
            }

            int transfer = Math.min(space, remaining);
            ItemStack updated = existing.clone();
            updated.setAmount(existing.getAmount() + transfer);

            inventory.setItem(slot, updated);
            remaining -= transfer;
        }

        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            if (holder.isLocked(slot)) {
                continue;
            }

            ItemStack existing = contents[slot];
            if (existing != null && !existing.getType().isAir()) {
                continue;
            }

            int transfer = Math.min(maxStackSize, remaining);
            ItemStack placed = stack.clone();
            placed.setAmount(transfer);

            inventory.setItem(slot, placed);
            remaining -= transfer;
        }

        if (remaining <= 0) {
            return null;
        }

        ItemStack leftover = stack.clone();
        leftover.setAmount(remaining);
        return leftover;
    }

    public static boolean containsSimilarInLockedSlots(@NotNull Inventory inventory, @NotNull SellGuiHolder holder,
                                                       @Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }

        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (!holder.isLocked(slot)) {
                continue;
            }

            ItemStack existing = contents[slot];
            if (existing != null && !existing.getType().isAir() && existing.isSimilar(stack)) {
                return true;
            }
        }

        return false;
    }
}
