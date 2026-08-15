package net.mackenziemolloy.shopguiplus.sellgui.shop;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ItemGroup {

    private final ItemStack key;
    private final List<ItemStack> sources = new ArrayList<>(2);

    private long amount;

    public ItemGroup(@NotNull ItemStack key) {
        this.key = key;
    }

    public void add(@NotNull ItemStack stack) {
        this.sources.add(stack);
        this.amount += stack.getAmount();
    }

    public @NotNull ItemStack key() {
        return this.key;
    }

    public long amount() {
        return this.amount;
    }

    public int cappedAmount() {
        return (int) Math.min(Integer.MAX_VALUE, this.amount);
    }

    public @NotNull List<ItemStack> sources() {
        return this.sources;
    }
}
