package net.mackenziemolloy.shopguiplus.sellgui.transaction;

import net.brcdev.shopgui.provider.economy.EconomyProvider;
import org.jetbrains.annotations.NotNull;

public record ReceiptLine(@NotNull String itemName, long amount, double price, @NotNull EconomyProvider provider) {
}
