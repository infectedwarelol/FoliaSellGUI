package net.mackenziemolloy.shopguiplus.sellgui.transaction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.brcdev.shopgui.provider.economy.EconomyProvider;
import org.jetbrains.annotations.NotNull;

public final class Payouts {

    private final Map<EconomyProvider, Double> amounts = new LinkedHashMap<>(4);

    public void add(@NotNull EconomyProvider provider, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return;
        }

        this.amounts.merge(provider, amount, Double::sum);
    }

    public void addAll(@NotNull Payouts other) {
        for (Map.Entry<EconomyProvider, Double> entry : other.amounts.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    public void multiply(int factor) {
        if (factor == 1) {
            return;
        }

        this.amounts.replaceAll((provider, amount) -> amount * factor);
    }

    public double total() {
        double total = 0.0D;
        for (double amount : this.amounts.values()) {
            total += amount;
        }

        return total;
    }

    public boolean isEmpty() {
        return this.amounts.isEmpty();
    }

    public @NotNull Map<EconomyProvider, Double> asMap() {
        return Collections.unmodifiableMap(this.amounts);
    }
}
