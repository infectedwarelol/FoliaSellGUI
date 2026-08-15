package net.mackenziemolloy.shopguiplus.sellgui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mackenziemolloy.shopguiplus.sellgui.command.CommandSellGUI;
import net.mackenziemolloy.shopguiplus.sellgui.compat.Schedulers;
import net.mackenziemolloy.shopguiplus.sellgui.compat.ServerVersion;
import net.mackenziemolloy.shopguiplus.sellgui.config.PluginConfig;
import net.mackenziemolloy.shopguiplus.sellgui.gui.SellGuiListener;
import net.mackenziemolloy.shopguiplus.sellgui.gui.SellGuiManager;
import net.mackenziemolloy.shopguiplus.sellgui.shop.EconomyService;
import net.mackenziemolloy.shopguiplus.sellgui.shop.ShopReloadListener;
import net.mackenziemolloy.shopguiplus.sellgui.shop.ShopService;
import net.mackenziemolloy.shopguiplus.sellgui.shulker.ShulkerService;
import net.mackenziemolloy.shopguiplus.sellgui.transaction.ItemReturnService;
import net.mackenziemolloy.shopguiplus.sellgui.transaction.SellTransactionService;
import net.mackenziemolloy.shopguiplus.sellgui.utility.CommentedConfiguration;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Messages;
import net.mackenziemolloy.shopguiplus.sellgui.utility.TransactionLogger;
import net.mackenziemolloy.shopguiplus.sellgui.utility.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SellGUI extends JavaPlugin {

    private static final int BSTATS_SERVICE_ID = 9356;
    private static final int SPIGOT_RESOURCE_ID = 85170;

    private static SellGUI instance;

    private volatile CommentedConfiguration configuration;
    private volatile PluginConfig pluginConfig;
    private volatile boolean compatible;

    private Schedulers schedulers;
    private TransactionLogger transactionLogger;
    private ShopService shopService;
    private SellGuiManager guiManager;
    private Metrics metrics;

    @Override
    public void onEnable() {
        instance = this;

        Logger logger = getLogger();
        this.schedulers = new Schedulers(this);
        this.compatible = checkCompatibility();

        try {
            applyConfiguration(readConfiguration());
        } catch (IOException | InvalidConfigurationException exception) {
            logger.log(Level.SEVERE, "Failed to load the 'config.yml' file due to an error:", exception);
            applyConfiguration(new CommentedConfiguration());
        }

        Messages messages = new Messages(this::pluginConfig);
        this.shopService = new ShopService(logger);

        EconomyService economyService = new EconomyService(logger);
        ShulkerService shulkerService = new ShulkerService(this.shopService);
        ItemReturnService itemReturnService = new ItemReturnService(this.schedulers, logger);

        SellTransactionService transactionService = new SellTransactionService(this::pluginConfig, this.shopService,
                economyService, shulkerService, itemReturnService, messages, this.transactionLogger, logger);

        this.guiManager = new SellGuiManager(this::pluginConfig, this.schedulers, transactionService,
                this.shopService, messages, logger);

        Bukkit.getPluginManager().registerEvents(new SellGuiListener(this.guiManager, transactionService,
                shulkerService, messages, this.schedulers), this);

        try {
            Bukkit.getPluginManager().registerEvents(new ShopReloadListener(this.shopService), this);
        } catch (Throwable throwable) {
            logger.log(Level.WARNING, "Could not listen for ShopGUIPlus reloads, shop caches will only be cleared "
                    + "when this plugin reloads.", throwable);
        }

        new CommandSellGUI(this, this.guiManager, messages, this.schedulers).register();

        if (!this.compatible) {
            logger.severe("ShopGUIPlus 1.78.0 or newer is required. The Sell GUI has been disabled until you update.");
        }

        logger.info("Detected server platform: " + ServerVersion.describePlatform() + ".");

        setupMetrics();
        setupUpdates();

        logger.info("*-*");
        logger.info("ShopGUIPlus SellGUI");
        logger.info("Made by Mackenzie Molloy");
        logger.info("*-*");
    }

    @Override
    public void onDisable() {
        if (this.guiManager != null) {
            this.guiManager.shutdown();
        }

        if (this.transactionLogger != null) {
            this.transactionLogger.close();
        }

        if (this.metrics != null) {
            this.metrics.shutdown();
        }

        instance = null;
    }

    public @NotNull CommentedConfiguration readConfiguration() throws IOException, InvalidConfigurationException {
        saveDefaultConfig();

        File configFile = new File(getDataFolder(), "config.yml");
        CommentedConfiguration loaded = new CommentedConfiguration();
        loaded.load(configFile);

        try (InputStream jarConfig = getResource("config.yml")) {
            if (jarConfig != null) {
                loaded.syncWithConfig(configFile, jarConfig, "stupid_option");
            }
        }

        return loaded;
    }

    public void applyConfiguration(@NotNull CommentedConfiguration loaded) {
        this.configuration = loaded;
        this.pluginConfig = PluginConfig.load(loaded, getLogger());

        if (this.transactionLogger == null) {
            this.transactionLogger = new TransactionLogger(this);
        }

        this.transactionLogger.apply(this.pluginConfig);

        if (this.shopService != null) {
            this.shopService.invalidate();
        }
    }

    public @Nullable CommentedConfiguration getConfiguration() {
        return this.configuration;
    }

    public @Nullable PluginConfig pluginConfig() {
        return this.pluginConfig;
    }

    public boolean isCompatible() {
        return this.compatible;
    }

    public @NotNull Schedulers schedulers() {
        return this.schedulers;
    }

    public static SellGUI getInstance() {
        return instance;
    }

    private boolean checkCompatibility() {
        try {
            Class<?> apiClass = Class.forName("net.brcdev.shopgui.ShopGuiPlusApi");
            Class<?> playerClass = Class.forName("org.bukkit.entity.Player");
            Class<?> itemStackClass = Class.forName("org.bukkit.inventory.ItemStack");

            Method lookup = apiClass.getMethod("getItemStackShopItem", playerClass, itemStackClass);
            return (lookup != null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private void setupMetrics() {
        try {
            this.metrics = new Metrics(this, BSTATS_SERVICE_ID);
        } catch (Throwable throwable) {
            getLogger().log(Level.FINE, "Metrics could not be started.", throwable);
        }
    }

    private void setupUpdates() {
        PluginConfig config = this.pluginConfig;
        if (config != null && !config.updateCheck()) {
            return;
        }

        String localVersion = getPluginMeta().getVersion();
        String prefix = getPluginMeta().getLoggerPrefix();
        String pluginPrefix = (prefix == null || prefix.isBlank() ? getName() : prefix);

        UpdateChecker updateChecker = new UpdateChecker(getLogger(), this.schedulers, SPIGOT_RESOURCE_ID);
        updateChecker.check(latestVersion -> this.schedulers.global(() -> {
            if (localVersion.contains("dev")) {
                Bukkit.getConsoleSender().sendMessage(Component.text("[" + pluginPrefix + "] You are running a "
                        + "DEVELOPMENT build. This may contain bugs.", NamedTextColor.DARK_RED));
                return;
            }

            if (localVersion.equalsIgnoreCase(latestVersion)) {
                Bukkit.getConsoleSender().sendMessage(Component.text("[" + pluginPrefix + "] You are running the "
                        + "LATEST release.", NamedTextColor.GREEN));
                return;
            }

            Bukkit.getConsoleSender().sendMessage(Component.text("[" + pluginPrefix + "] The latest upstream release "
                    + "is " + latestVersion + " (you are running " + localVersion + "). Download: "
                    + "https://www.spigotmc.org/resources/85170/", NamedTextColor.DARK_RED));
        }));
    }
}
