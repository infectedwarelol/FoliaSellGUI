package net.mackenziemolloy.shopguiplus.sellgui.compat;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public final class ServerVersion {

    private static final String MINECRAFT_VERSION;
    private static final int MAJOR;
    private static final int MINOR;
    private static final int PATCH;
    private static final boolean REGIONISED;
    private static final boolean CANVAS;

    static {
        String bukkitVersion = Bukkit.getBukkitVersion();
        int firstDash = bukkitVersion.indexOf('-');
        MINECRAFT_VERSION = (firstDash < 0 ? bukkitVersion : bukkitVersion.substring(0, firstDash));

        String[] parts = MINECRAFT_VERSION.split("\\.");
        MAJOR = parseLeadingInt(parts.length > 0 ? parts[0] : "");
        MINOR = parseLeadingInt(parts.length > 1 ? parts[1] : "");
        PATCH = parseLeadingInt(parts.length > 2 ? parts[2] : "");

        REGIONISED = isClassPresent("io.papermc.paper.threadedregions.RegionizedServer");
        CANVAS = isClassPresent("io.canvasmc.canvas.Config")
                || isClassPresent("io.canvasmc.canvas.configuration.CanvasConfig");
    }

    private ServerVersion() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull String getMinecraftVersion() {
        return MINECRAFT_VERSION;
    }

    public static int getMajorVersion() {
        return MAJOR;
    }

    public static int getMinorVersion() {
        return MINOR;
    }

    public static int getPatchVersion() {
        return PATCH;
    }

    public static boolean isAtLeast(int minor, int patch) {
        if (MAJOR != 1) {
            return MAJOR > 1;
        }

        if (MINOR != minor) {
            return MINOR > minor;
        }

        return PATCH >= patch;
    }

    public static boolean isRegionised() {
        return REGIONISED;
    }

    public static boolean isCanvas() {
        return CANVAS;
    }

    public static @NotNull String describePlatform() {
        StringBuilder builder = new StringBuilder(Bukkit.getName());
        builder.append(' ').append(MINECRAFT_VERSION);

        if (CANVAS) {
            builder.append(" (CanvasMC)");
        } else if (REGIONISED) {
            builder.append(" (regionised)");
        }

        return builder.toString();
    }

    private static boolean isClassPresent(@NotNull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static int parseLeadingInt(@NotNull String input) {
        int index = 0;
        int length = input.length();
        while (index < length && Character.isDigit(input.charAt(index))) {
            index++;
        }

        if (index == 0) {
            return 0;
        }

        return Integer.parseInt(input.substring(0, index));
    }
}
