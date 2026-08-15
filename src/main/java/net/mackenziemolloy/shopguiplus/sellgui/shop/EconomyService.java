package net.mackenziemolloy.shopguiplus.sellgui.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.brcdev.shopgui.provider.economy.EconomyProvider;
import net.mackenziemolloy.shopguiplus.sellgui.transaction.Payouts;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class EconomyService {

    private final Logger logger;

    public EconomyService(@NotNull Logger logger) {
        this.logger = logger;
    }

    public boolean payout(@NotNull Player player, @NotNull Payouts payouts) {
        List<Map.Entry<EconomyProvider, Double>> completed = new ArrayList<>(payouts.asMap().size());

        for (Map.Entry<EconomyProvider, Double> entry : payouts.asMap().entrySet()) {
            EconomyProvider provider = entry.getKey();
            double amount = entry.getValue();

            if (!Double.isFinite(amount) || amount <= 0.0D) {
                continue;
            }

            try {
                provider.deposit(player, amount);
                completed.add(entry);
            } catch (Throwable throwable) {
                this.logger.log(Level.SEVERE, "Failed to deposit " + amount + " to " + player.getName()
                        + " via " + providerName(provider) + ". The sale was rolled back.", throwable);
                rollback(player, completed);
                return false;
            }
        }

        return true;
    }

    private void rollback(@NotNull Player player, @NotNull List<Map.Entry<EconomyProvider, Double>> completed) {
        for (int index = (completed.size() - 1); index >= 0; index--) {
            Map.Entry<EconomyProvider, Double> entry = completed.get(index);

            try {
                entry.getKey().withdraw(player, entry.getValue());
            } catch (Throwable throwable) {
                this.logger.log(Level.SEVERE, "Failed to roll back a deposit of " + entry.getValue() + " for "
                        + player.getName() + " via " + providerName(entry.getKey())
                        + ". This balance must be corrected manually.", throwable);
            }
        }
    }

    private @NotNull String providerName(@NotNull EconomyProvider provider) {
        try {
            String name = provider.getName();
            return (name == null ? provider.getClass().getName() : name);
        } catch (Throwable ignored) {
            return provider.getClass().getName();
        }
    }
}
