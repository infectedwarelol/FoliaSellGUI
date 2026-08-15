package net.mackenziemolloy.shopguiplus.sellgui.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mackenziemolloy.shopguiplus.sellgui.SellGUI;
import net.mackenziemolloy.shopguiplus.sellgui.compat.Schedulers;
import net.mackenziemolloy.shopguiplus.sellgui.compat.ServerVersion;
import net.mackenziemolloy.shopguiplus.sellgui.gui.SellGuiManager;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Colors;
import net.mackenziemolloy.shopguiplus.sellgui.utility.CommentedConfiguration;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Hastebin;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Messages;
import net.mackenziemolloy.shopguiplus.sellgui.utility.Sounds;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public final class CommandSellGUI implements TabExecutor {

    private static final List<String> SUB_COMMANDS = List.of("rl", "reload", "debug", "dump");
    private static final String UPDATE_REQUIRED = "&7\n&7\n&a&lUPDATE REQUIRED \n&7\n&7Unfortunately &fSellGUI &7will "
            + "not work until you update &cShopGUIPlus&7 to version &c1.78.0&7 or above.\n&7\n&eDownload: "
            + "https://spigotmc.org/resources/6515/\n&7\n&7";

    private final SellGUI plugin;
    private final SellGuiManager guiManager;
    private final Messages messages;
    private final Schedulers schedulers;
    private final Logger logger;

    public CommandSellGUI(@NotNull SellGUI plugin, @NotNull SellGuiManager guiManager, @NotNull Messages messages,
                          @NotNull Schedulers schedulers) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.guiManager = guiManager;
        this.messages = messages;
        this.schedulers = schedulers;
        this.logger = plugin.getLogger();
    }

    public void register() {
        PluginCommand pluginCommand = this.plugin.getCommand("sellgui");
        if (pluginCommand == null) {
            this.logger.severe("The 'sellgui' command is missing from plugin.yml.");
            return;
        }

        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String label, String @NotNull [] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], SUB_COMMANDS, new ArrayList<>(SUB_COMMANDS.size()));
        }

        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             String @NotNull [] args) {
        if (!this.plugin.isCompatible()) {
            sender.sendMessage(Colors.component(UPDATE_REQUIRED));
            return true;
        }

        if (args.length == 0) {
            return commandBase(sender);
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "rl", "reload" -> commandReload(sender);
            case "debug", "dump" -> commandDebug(sender);
            default -> false;
        };
    }

    private boolean commandBase(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("sellgui.use")) {
            this.messages.send(player, "no_permission");
            return true;
        }

        this.guiManager.open(player);
        return true;
    }

    private boolean commandReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission("sellgui.reload")) {
            this.messages.send(sender, "no_permission");
            return true;
        }

        this.schedulers.async(() -> {
            CommentedConfiguration loaded;

            try {
                loaded = this.plugin.readConfiguration();
            } catch (Exception exception) {
                this.logger.log(Level.SEVERE, "Failed to reload the configuration.", exception);
                respond(sender, () -> sender.sendMessage(Component.text(
                        "An error occurred, please check the server console.", NamedTextColor.RED)));
                return;
            }

            this.schedulers.global(() -> {
                this.plugin.applyConfiguration(loaded);
                respond(sender, () -> {
                    this.messages.send(sender, "reloaded_config");
                    if (sender instanceof Player player) {
                        Sounds.play(this.plugin.pluginConfig(), player, "success");
                    }
                });
            });
        });

        return true;
    }

    private boolean commandDebug(@NotNull CommandSender sender) {
        if (!sender.hasPermission("sellgui.dump") && !sender.hasPermission("sellgui.debug")) {
            this.messages.send(sender, "no_permission");
            return true;
        }

        String dump = buildDump();

        this.schedulers.async(() -> {
            String url;

            try {
                url = new Hastebin().post(dump);
            } catch (Exception exception) {
                this.logger.log(Level.SEVERE, "Failed to upload the debug dump.", exception);
                respond(sender, () -> sender.sendMessage(Component.text(
                        "An error occurred, please check the server console.", NamedTextColor.RED)));
                return;
            }

            Component message = Component.text("[ShopGUIPlus-SellGUI] Successfully dumped server information here: "
                    + url, NamedTextColor.RED);

            respond(sender, () -> {
                Bukkit.getConsoleSender().sendMessage(message);

                if (sender instanceof Player player) {
                    player.sendMessage(message);
                    Sounds.play(this.plugin.pluginConfig(), player, "success");
                }
            });
        });

        return true;
    }

    private @NotNull String buildDump() {
        List<String> pluginInfoList = new ArrayList<>();

        for (Plugin other : Bukkit.getPluginManager().getPlugins()) {
            pluginInfoList.add(String.format(Locale.US, "- %s v%s by %s", other.getName(),
                    other.getPluginMeta().getVersion(), String.join(", ", other.getPluginMeta().getAuthors())));
        }

        CommentedConfiguration configuration = this.plugin.getConfiguration();

        return "| System Information\n\n"
                + "- OS Type: " + System.getProperty("os.name") + "\n"
                + "- OS Version: " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")\n"
                + "- Java Version: " + System.getProperty("java.version") + "\n"
                + "- Processor: " + System.getenv("PROCESSOR_IDENTIFIER") + "\n\n"
                + "| Server Information\n\n"
                + "- Platform: " + ServerVersion.describePlatform() + "\n"
                + "- Version: " + Bukkit.getBukkitVersion() + "\n"
                + "- Online Mode: " + Bukkit.getOnlineMode() + "\n"
                + "- Memory Usage: " + getMemoryUsage() + "\n\n"
                + "| Plugins\n" + String.join("\n", pluginInfoList) + "\n\n"
                + "| Plugin Configuration\n\n" + (configuration == null ? "<unavailable>"
                : configuration.saveToString());
    }

    private void respond(@NotNull CommandSender sender, @NotNull Runnable task) {
        if (sender instanceof Player player) {
            this.schedulers.onEntity(player, task);
            return;
        }

        this.schedulers.global(task);
    }

    private static @NotNull String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryMb = ((runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L);
        long maxMemoryMb = (runtime.maxMemory() / 1_048_576L);

        return String.format(Locale.US, "%s / %s MiB", usedMemoryMb, maxMemoryMb);
    }
}
