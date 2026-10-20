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
        if (meta != null) {
            if (meta.hasDisplayName()) {
                String displayName = toLegacy(meta.displayName());
                if (!displayName.isEmpty()) {
                    return displayName;
                }
            }

            if (meta.hasItemName()) {
                String itemName = toLegacy(meta.itemName());
                if (!itemName.isEmpty()) {
                    return itemName;
                }
            }
        }

        return materialName(item.getType());
    }

    private static @NotNull String toLegacy(Component component) {
        return component == null ? "" : LegacyComponentSerializer.legacySection().serialize(component);
    }
}
