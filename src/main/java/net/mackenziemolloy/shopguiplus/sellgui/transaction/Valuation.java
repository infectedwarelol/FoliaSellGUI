package net.mackenziemolloy.shopguiplus.sellgui.transaction;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class Valuation {

    private final Payouts payouts = new Payouts();
    private final List<ReceiptLine> lines = new ArrayList<>();
    private final List<ItemStack> returnedItems = new ArrayList<>();

    private long soldCount;

    public @NotNull Payouts payouts() {
        return this.payouts;
    }

    public @NotNull List<ReceiptLine> lines() {
        return this.lines;
    }

    public @NotNull List<ItemStack> returnedItems() {
        return this.returnedItems;
    }

    public long soldCount() {
        return this.soldCount;
    }

    public void addSold(long amount) {
        this.soldCount += amount;
    }

    public void addLine(@NotNull ReceiptLine line) {
        this.lines.add(line);
    }

    public void addReturnedItem(@NotNull ItemStack item) {
        this.returnedItems.add(item);
    }

    public void merge(@NotNull Valuation other) {
        this.payouts.addAll(other.payouts);
        this.lines.addAll(other.lines);
        this.returnedItems.addAll(other.returnedItems);
        this.soldCount += other.soldCount;
    }

    public boolean hasSales() {
        return this.soldCount > 0 && !this.payouts.isEmpty();
    }

    public void scale(int factor) {
        if (factor <= 1) {
            return;
        }

        this.payouts.multiply(factor);
        this.soldCount *= factor;

        for (int index = 0; index < this.lines.size(); index++) {
            ReceiptLine line = this.lines.get(index);
            this.lines.set(index, new ReceiptLine(line.itemName(), line.amount() * factor,
                    line.price() * factor, line.provider()));
        }
    }
}
