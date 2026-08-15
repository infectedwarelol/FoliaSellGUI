package net.mackenziemolloy.shopguiplus.sellgui.utility;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import net.kyori.adventure.text.Component;
import net.mackenziemolloy.shopguiplus.sellgui.config.PluginConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Messages {

    private final Supplier<PluginConfig> configSupplier;

    public Messages(@NotNull Supplier<PluginConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    public @NotNull String raw(@NotNull String key) {
        PluginConfig config = this.configSupplier.get();
        return (config == null ? "" : config.rawMessage(key));
    }

    public boolean isBlank(@NotNull String key) {
        return raw(key).isEmpty();
    }

    public @NotNull String legacy(@NotNull String key, @Nullable UnaryOperator<String> replacer) {
        String message = raw(key);
        if (message.isEmpty()) {
            return "";
        }

        if (replacer != null) {
            message = replacer.apply(message);
        }

        return Colors.legacy(message);
    }

    public @NotNull Component component(@NotNull String key, @Nullable UnaryOperator<String> replacer) {
        String legacy = legacy(key, replacer);
        if (legacy.isEmpty()) {
            return Component.empty();
        }

        return Colors.fromLegacy(legacy);
    }

    public void send(@NotNull CommandSender sender, @NotNull String key) {
        send(sender, key, null);
    }

    public void send(@NotNull CommandSender sender, @NotNull String key, @Nullable UnaryOperator<String> replacer) {
        String legacy = legacy(key, replacer);
        if (legacy.isEmpty()) {
            return;
        }

        sender.sendMessage(Colors.fromLegacy(legacy));
    }
}
