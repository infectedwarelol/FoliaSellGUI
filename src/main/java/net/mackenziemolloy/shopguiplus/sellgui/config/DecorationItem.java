package net.mackenziemolloy.shopguiplus.sellgui.config;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public record DecorationItem(int slot, @NotNull ItemStack item, @NotNull List<String> consoleCommands,
                             @NotNull List<String> playerCommands, boolean sellInventory) {

    public boolean hasActions() {
        return this.sellInventory || !this.consoleCommands.isEmpty() || !this.playerCommands.isEmpty();
    }
}
