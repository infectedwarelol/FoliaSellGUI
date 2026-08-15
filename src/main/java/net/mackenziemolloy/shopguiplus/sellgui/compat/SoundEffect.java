package net.mackenziemolloy.shopguiplus.sellgui.compat;

import java.util.Locale;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SoundEffect {

    private final Sound sound;
    private final String key;

    private SoundEffect(@Nullable Sound sound, @Nullable String key) {
        this.sound = sound;
        this.key = key;
    }

    public static @Nullable SoundEffect resolve(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        boolean keyStyle = (trimmed.indexOf('.') >= 0 || trimmed.indexOf(':') >= 0);
        if (!keyStyle) {
            Sound constant = fromConstant(trimmed);
            if (constant != null) {
                return new SoundEffect(constant, null);
            }
        }

        Sound registered = fromRegistry(trimmed, keyStyle);
        if (registered != null) {
            return new SoundEffect(registered, null);
        }

        if (keyStyle) {
            return new SoundEffect(null, trimmed);
        }

        return null;
    }

    public void play(@NotNull Player player, float volume, float pitch) {
        if (this.sound != null) {
            player.playSound(player.getLocation(), this.sound, volume, pitch);
            return;
        }

        if (this.key != null) {
            player.playSound(player.getLocation(), this.key, volume, pitch);
        }
    }

    @SuppressWarnings("removal")
    private static @Nullable Sound fromConstant(@NotNull String value) {
        try {
            return Sound.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | LinkageError ignored) {
            return null;
        }
    }

    private static @Nullable Sound fromRegistry(@NotNull String value, boolean keyStyle) {
        String candidate = keyStyle
                ? value.toLowerCase(Locale.ROOT)
                : value.toLowerCase(Locale.ROOT).replace('_', '.');

        try {
            NamespacedKey key = NamespacedKey.fromString(candidate);
            if (key == null) {
                return null;
            }

            return Registry.SOUND_EVENT.get(key);
        } catch (IllegalArgumentException | LinkageError ignored) {
            return null;
        }
    }
}
