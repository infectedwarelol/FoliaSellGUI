package net.mackenziemolloy.shopguiplus.sellgui.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.mackenziemolloy.shopguiplus.sellgui.utility.sirblobman.HexColorUtility;
import net.mackenziemolloy.shopguiplus.sellgui.utility.sirblobman.MessageUtility;
import org.jetbrains.annotations.NotNull;

public final class Colors {

    private Colors() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull String legacy(@NotNull String message) {
        if (message.isEmpty()) {
            return message;
        }

        return MessageUtility.color(HexColorUtility.replaceHexColors('&', message));
    }

    public static @NotNull Component component(@NotNull String message) {
        if (message.isEmpty()) {
            return Component.empty();
        }

        return LegacyComponentSerializer.legacySection().deserialize(legacy(message));
    }

    public static @NotNull Component fromLegacy(@NotNull String message) {
        if (message.isEmpty()) {
            return Component.empty();
        }

        return LegacyComponentSerializer.legacySection().deserialize(message);
    }

    public static @NotNull String stripColor(@NotNull String message) {
        return HexColorUtility.purgeAllColor(message);
    }
}
