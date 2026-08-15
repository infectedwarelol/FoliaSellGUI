package net.mackenziemolloy.shopguiplus.sellgui.utility;

import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public final class TextUtility {

    private TextUtility() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull String capitalize(@NotNull String value) {
        if (value.isEmpty()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    public static @NotNull String materialName(@NotNull Material material) {
        String name = material.name()
                .replace("AETHER_LEGACY_", "")
                .replace("LOST_AETHER_", "")
                .replace("_", " ")
                .toLowerCase(Locale.ROOT);

        return capitalize(name);
    }

    public static @NotNull String itemName(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component displayName = meta.displayName();
            if (displayName != null) {
                String legacy = LegacyComponentSerializer.legacySection().serialize(displayName);
                if (!legacy.isEmpty()) {
                    return legacy;
                }
            }
        }

        return materialName(item.getType());
    }
}
