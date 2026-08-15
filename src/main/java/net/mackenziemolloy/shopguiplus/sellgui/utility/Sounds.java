package net.mackenziemolloy.shopguiplus.sellgui.utility;

import net.mackenziemolloy.shopguiplus.sellgui.compat.SoundEffect;
import net.mackenziemolloy.shopguiplus.sellgui.config.PluginConfig;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Sounds {

    private Sounds() {
        throw new UnsupportedOperationException();
    }

    public static void play(@Nullable PluginConfig config, @NotNull Player player, @NotNull String event) {
        if (config == null || !config.soundsEnabled()) {
            return;
        }

        SoundEffect effect = config.sound(event);
        if (effect == null) {
            return;
        }

        try {
            effect.play(player, config.soundVolume(), config.soundPitch());
        } catch (Throwable ignored) {
        }
    }
}
