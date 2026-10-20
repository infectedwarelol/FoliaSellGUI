package net.mackenziemolloy.shopguiplus.sellgui.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import net.mackenziemolloy.shopguiplus.sellgui.compat.SoundEffect;
import net.mackenziemolloy.shopguiplus.sellgui.config.ShulkerOptions.ReceiptMode;
import net.mackenziemolloy.shopguiplus.sellgui.config.ShulkerOptions.UnsellablePolicy;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Colors;
import net.mackenziemolloy.shopguiplus.sellgui.utility.NumberFormatter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PluginConfig {

    private static final String[] SOUND_EVENTS = {"open", "success", "failed"};

    private final int receiptType;
    private final boolean sellTitles;
    private final boolean actionBarMessages;
    private final boolean updateCheck;
    private final boolean transactionLogEnabled;
    private final String transactionLogDateFormat;

    private final boolean soundsEnabled;
    private final boolean soundErrorNotification;
    private final float soundVolume;
    private final float soundPitch;
    private final Map<String, SoundEffect> sounds;

    private final int rows;
    private final Component guiTitle;
    private final List<DecorationItem> decorations;
    private final boolean[] lockedSlots;
    private final DecorationItem[] decorationBySlot;

    private final Map<String, String> messages;
    private final NumberFormatter numberFormatter;
    private final ShulkerOptions shulkerOptions;

    private PluginConfig(@NotNull ConfigurationSection configuration, @NotNull Logger logger) {
        this.receiptType = configuration.getInt("options.receipt_type", 0);
        this.sellTitles = configuration.getBoolean("options.sell_titles", false);
        this.actionBarMessages = configuration.getBoolean("options.action_bar_msgs", false);
        this.updateCheck = configuration.getBoolean("options.update_check", true);
        this.transactionLogEnabled = configuration.getBoolean("options.transaction_log.enabled", false);
        this.transactionLogDateFormat = configuration.getString("options.transaction_log.date_format",
                "yyyy/MM/dd HH:mm:ss");

        this.soundsEnabled = configuration.getBoolean("options.sounds.enabled", false);
        this.soundErrorNotification = configuration.getBoolean("options.sounds.error_notification", false);
        this.soundVolume = readFloat(configuration, "options.sounds.volume", 1.0F);
        this.soundPitch = readFloat(configuration, "options.sounds.pitch", 1.0F);

        Map<String, SoundEffect> soundMap = new HashMap<>(SOUND_EVENTS.length);
        for (String event : SOUND_EVENTS) {
            String path = ("options.sounds.events." + event);
            String value = configuration.getString(path);
            SoundEffect effect = SoundEffect.resolve(value);

            if (effect == null) {
                if (this.soundsEnabled && this.soundErrorNotification && value != null && !value.isBlank()) {
                    logger.warning("Invalid sound for '" + path + "': '" + value + "'.");
                }

                continue;
            }

            soundMap.put(event, effect);
        }

        this.sounds = Map.copyOf(soundMap);

        int configuredRows = configuration.getInt("options.rows", 6);
        this.rows = (configuredRows < 1 || configuredRows > 6) ? 6 : configuredRows;

        this.numberFormatter = new NumberFormatter(configuration.getBoolean("options.rounded_pricing", false),
                configuration.getBoolean("options.remove_trailing_zeros", true),
                configuration.getBoolean("options.abbreviate_numbers", false));

        Map<String, String> messageMap = new HashMap<>();
        ConfigurationSection messageSection = configuration.getConfigurationSection("messages");
        if (messageSection != null) {
            for (String key : messageSection.getKeys(true)) {
                if (messageSection.isConfigurationSection(key)) {
                    continue;
                }

                String value = messageSection.getString(key);
                if (value != null) {
                    messageMap.put(key, value);
                }
            }
        }

        this.messages = Map.copyOf(messageMap);
        this.guiTitle = colorComponent(messageMap.getOrDefault("sellgui_title", ""));
        this.shulkerOptions = readShulkerOptions(configuration);

        int size = (this.rows * 9);
        this.lockedSlots = new boolean[size];
        this.decorationBySlot = new DecorationItem[size];

        List<DecorationItem> decorationList = readDecorations(configuration, size, logger);
        for (DecorationItem decoration : decorationList) {
            this.lockedSlots[decoration.slot()] = true;
            this.decorationBySlot[decoration.slot()] = decoration;
        }

        this.decorations = List.copyOf(decorationList);
    }

    public static @NotNull PluginConfig load(@NotNull ConfigurationSection configuration, @NotNull Logger logger) {
        return new PluginConfig(configuration, logger);
    }

    public int receiptType() {
        return this.receiptType;
    }

    public boolean sellTitles() {
        return this.sellTitles;
    }

    public boolean actionBarMessages() {
        return this.actionBarMessages;
    }

    public boolean updateCheck() {
        return this.updateCheck;
    }

    public boolean transactionLogEnabled() {
        return this.transactionLogEnabled;
    }

    public @NotNull String transactionLogDateFormat() {
        return this.transactionLogDateFormat;
    }

    public boolean soundsEnabled() {
        return this.soundsEnabled;
    }

    public float soundVolume() {
        return this.soundVolume;
    }

    public float soundPitch() {
        return this.soundPitch;
    }

    public @Nullable SoundEffect sound(@NotNull String event) {
        return this.sounds.get(event);
    }

    public int rows() {
        return this.rows;
    }

    public int size() {
        return this.lockedSlots.length;
    }

    public @NotNull Component guiTitle() {
        return this.guiTitle;
    }

    public @NotNull List<DecorationItem> decorations() {
        return this.decorations;
    }

    public boolean[] lockedSlots() {
        return this.lockedSlots;
    }

    public @Nullable DecorationItem decorationAt(int slot) {
        if (slot < 0 || slot >= this.decorationBySlot.length) {
            return null;
        }

        return this.decorationBySlot[slot];
    }

    public @NotNull String rawMessage(@NotNull String key) {
        String message = this.messages.get(key);
        return (message == null ? "" : message);
    }

    public @NotNull NumberFormatter numberFormatter() {
        return this.numberFormatter;
    }

    public @NotNull ShulkerOptions shulkerOptions() {
        return this.shulkerOptions;
    }

    private static @NotNull ShulkerOptions readShulkerOptions(@NotNull ConfigurationSection configuration) {
        boolean enabled = configuration.getBoolean("options.shulker.enabled", true);
        boolean containerPrice = configuration.getBoolean("options.shulker.include_container_price", true);

        int maxDepth = configuration.getInt("options.shulker.max_depth", 3);
        maxDepth = Math.max(1, Math.min(16, maxDepth));

        int maxScanned = configuration.getInt("options.shulker.max_scanned_items", 4096);
        maxScanned = Math.max(64, Math.min(262144, maxScanned));

        UnsellablePolicy policy = UnsellablePolicy.parse(configuration.getString("options.shulker.unsellable_contents"),
                UnsellablePolicy.KEEP_ITEMS);
        ReceiptMode receiptMode = ReceiptMode.parse(configuration.getString("options.shulker.receipt_mode"),
                ReceiptMode.SUMMARY);
        boolean showEstimate = configuration.getBoolean("options.shulker.show_estimate", true);

        return new ShulkerOptions(enabled, containerPrice, maxDepth, maxScanned, policy, receiptMode, showEstimate);
    }

    @SuppressWarnings("deprecation")
    private static @NotNull List<DecorationItem> readDecorations(@NotNull ConfigurationSection configuration, int size,
                                                                 @NotNull Logger logger) {
        ConfigurationSection decorationSection = configuration.getConfigurationSection("options.decorations");
        if (decorationSection == null) {
            return Collections.emptyList();
        }

        Set<String> keys = decorationSection.getKeys(false);
        List<DecorationItem> decorationList = new ArrayList<>(keys.size());

        for (String key : keys) {
            ConfigurationSection section = decorationSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String materialName = section.getString("item.material");
            Material material = (materialName == null ? null : Material.matchMaterial(materialName));
            int slot = section.getInt("slot", -1);

            if (material == null || material.isAir() || !section.isInt("slot") || slot < 0 || slot >= size) {
                logger.warning("Failed to load decoration item with id '" + key + "'.");
                continue;
            }

            ItemStack item = new ItemStack(material);
            int quantity = section.getInt("item.quantity", 1);
            item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), quantity)));

            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta != null) {
                String displayName = section.getString("item.name");
                if (displayName != null) {
                    itemMeta.displayName(colorComponent(displayName));
                }

                List<String> loreList = section.getStringList("item.lore");
                if (!loreList.isEmpty()) {
                    List<Component> lore = new ArrayList<>(loreList.size());
                    for (String line : loreList) {
                        lore.add(colorComponent(line));
                    }

                    itemMeta.lore(lore);
                }

                int customModelData = section.getInt("item.customModelData", 0);
                if (customModelData != 0) {
                    itemMeta.setCustomModelData(customModelData);
                }

                int damage = section.getInt("item.damage", 0);
                if (damage > 0 && itemMeta instanceof Damageable damageable) {
                    damageable.setDamage(damage);
                }

                item.setItemMeta(itemMeta);
            }

            List<String> consoleCommands = List.copyOf(section.getStringList("commandsOnClickConsole"));
            List<String> playerCommands = List.copyOf(section.getStringList("commandsOnClick"));
            boolean sellInventory = section.getBoolean("item.sellinventory", false);

            decorationList.add(new DecorationItem(slot, item, consoleCommands, playerCommands, sellInventory));
        }

        return decorationList;
    }

    private static @NotNull Component colorComponent(@NotNull String message) {
        return Colors.component(message);
    }

    private static float readFloat(@NotNull ConfigurationSection configuration, @NotNull String path, float fallback) {
        if (configuration.isDouble(path) || configuration.isInt(path) || configuration.isLong(path)) {
            return (float) configuration.getDouble(path, fallback);
        }

        return fallback;
    }
}
