package net.mackenziemolloy.shopguiplus.sellgui.utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class FileUtils {

    private FileUtils() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull File loadFile(@NotNull JavaPlugin plugin, @NotNull String name) throws IOException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs() && !dataFolder.isDirectory()) {
            throw new IOException("Failed to create the plugin data folder.");
        }

        File file = new File(dataFolder, name);
        if (file.exists()) {
            return file;
        }

        try (InputStream resource = plugin.getResource(name)) {
            if (resource == null) {
                if (!file.createNewFile() && !file.exists()) {
                    throw new IOException("Failed to create the file '" + name + "'.");
                }

                return file;
            }

            Files.copy(resource, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return file;
    }
}
