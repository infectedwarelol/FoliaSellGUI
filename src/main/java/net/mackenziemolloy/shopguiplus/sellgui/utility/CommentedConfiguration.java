/*******************************************************************************
 *
 *     CommentedConfiguration
 *     Developed by Ome_R
 *
 *     You may use the resource and/or modify it - but not
 *     claiming it as your own work. You are not allowed
 *     to remove this message, unless being permitted by
 *     the developer of the resource.
 *
 *     Spigot: https://www.spigotmc.org/resources/authors/ome_r.25629/
 *     MC-Market: https://www.mc-market.org/resources/authors/40228/
 *     GitHub: https://github.com/OmerBenGera?tab=repositories
 *     Website: https://bg-software.com/
 *
 *******************************************************************************/

package net.mackenziemolloy.shopguiplus.sellgui.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public final class CommentedConfiguration extends YamlConfiguration {

    private final Map<String, String> configComments = new HashMap<>();

    private boolean creationFailure = false;

    public CommentedConfiguration() {
        try {
            this.options().parseComments(false);
        } catch (Throwable ignored) {
        }
    }

    public void syncWithConfig(File file, InputStream resource, String... ignoredSections) throws IOException {
        if (this.creationFailure) {
            return;
        }

        CommentedConfiguration cfg = loadConfiguration(resource);
        if (syncConfigurationSection(cfg, cfg.getConfigurationSection(""), Arrays.asList(ignoredSections))
                && file != null) {
            save(file);
        }
    }

    public void setComment(String path, String comment) {
        if (comment == null) {
            this.configComments.remove(path);
        } else {
            this.configComments.put(path, comment);
        }
    }

    public String getComment(String path) {
        return getComment(path, null);
    }

    public String getComment(String path, String def) {
        return this.configComments.getOrDefault(path, def);
    }

    public boolean containsComment(String path) {
        return getComment(path) != null;
    }

    public boolean hasFailed() {
        return this.creationFailure;
    }

    @Override
    public void loadFromString(@NotNull String contents) throws InvalidConfigurationException {
        super.loadFromString(contents);

        String[] lines = contents.split("\n");
        int currentIndex = 0;

        StringBuilder comments = new StringBuilder();
        String currentSection = "";

        while (currentIndex < lines.length) {
            if (isComment(lines[currentIndex])) {
                comments.append(lines[currentIndex]).append("\n");
            } else if (isNewSection(lines[currentIndex])) {
                currentSection = getSectionPath(this, lines[currentIndex], currentSection);

                if (comments.length() > 1) {
                    setComment(currentSection, comments.substring(0, comments.length() - 1));
                }

                comments = new StringBuilder();
            }

            currentIndex++;
        }
    }

    @Override
    public @NotNull String saveToString() {
        try {
            this.options().setHeader(Collections.emptyList());
        } catch (Throwable ignored) {
        }

        List<String> lines = new ArrayList<>(Arrays.asList(super.saveToString().split("\n")));

        int currentIndex = 0;
        String currentSection = "";

        while (currentIndex < lines.size()) {
            String line = lines.get(currentIndex);

            if (isNewSection(line)) {
                currentSection = getSectionPath(this, line, currentSection);

                if (containsComment(currentSection)) {
                    lines.add(currentIndex, getComment(currentSection));
                    currentIndex++;
                }
            }

            currentIndex++;
        }

        StringBuilder contents = new StringBuilder();
        for (String line : lines) {
            contents.append("\n").append(line);
        }

        return contents.isEmpty() ? "" : contents.substring(1);
    }

    private boolean syncConfigurationSection(CommentedConfiguration commentedConfig, ConfigurationSection section,
                                             List<String> ignoredSections) {
        boolean changed = false;

        for (String key : section.getKeys(false)) {
            String path = section.getCurrentPath().isEmpty() ? key : section.getCurrentPath() + "." + key;

            if (section.isConfigurationSection(key)) {
                boolean isIgnored = ignoredSections.stream().anyMatch(path::contains);
                boolean containsSection = contains(path);

                if (!containsSection || !isIgnored) {
                    changed = syncConfigurationSection(commentedConfig, section.getConfigurationSection(key),
                            ignoredSections) || changed;
                }
            } else if (!contains(path)) {
                set(path, section.get(key));
                changed = true;
            }

            if (commentedConfig.containsComment(path)
                    && !commentedConfig.getComment(path).equals(getComment(path))) {
                setComment(path, commentedConfig.getComment(path));
                changed = true;
            }
        }

        if (changed) {
            correctIndexes(section, getConfigurationSection(section.getCurrentPath()));
        }

        return changed;
    }

    private CommentedConfiguration flagAsFailed() {
        this.creationFailure = true;
        return this;
    }

    public static CommentedConfiguration loadConfiguration(@NotNull File file) {
        try {
            FileInputStream stream = new FileInputStream(file);
            return loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (FileNotFoundException ex) {
            Bukkit.getLogger().warning("File " + file.getName() + " doesn't exist.");
            return new CommentedConfiguration().flagAsFailed();
        }
    }

    public static CommentedConfiguration loadConfiguration(InputStream inputStream) {
        return loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public static CommentedConfiguration loadConfiguration(@NotNull Reader reader) {
        CommentedConfiguration config = new CommentedConfiguration();

        try (BufferedReader bufferedReader = reader instanceof BufferedReader bufferedInput ? bufferedInput
                : new BufferedReader(reader)) {
            StringBuilder contents = new StringBuilder();

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                contents.append(line).append('\n');
            }

            config.loadFromString(contents.toString());
        } catch (IOException | InvalidConfigurationException ex) {
            config.flagAsFailed();
            Bukkit.getLogger().warning("Failed to read a configuration: " + ex.getMessage());
        }

        return config;
    }

    private static boolean isNewSection(String line) {
        String trimLine = line.trim();
        return trimLine.contains(": ") || trimLine.endsWith(":");
    }

    private static String getSectionPath(CommentedConfiguration commentedConfig, String line, String currentSection) {
        String newSection = line.trim().split(": ")[0];

        if (newSection.endsWith(":")) {
            newSection = newSection.substring(0, newSection.length() - 1);
        }
        if (newSection.startsWith(".")) {
            newSection = newSection.substring(1);
        }
        if (newSection.startsWith("'") && newSection.endsWith("'")) {
            newSection = newSection.substring(1, newSection.length() - 1);
        }

        if (!currentSection.isEmpty() && commentedConfig.contains(currentSection + "." + newSection)) {
            newSection = currentSection + "." + newSection;
        } else {
            String parentSection = currentSection;

            while (!parentSection.isEmpty()
                    && !commentedConfig.contains((parentSection = getParentPath(parentSection)) + "." + newSection)) {
                continue;
            }

            newSection = parentSection.trim().isEmpty() ? newSection : parentSection + "." + newSection;
        }

        return newSection;
    }

    private static boolean isComment(String line) {
        String trimLine = line.trim();
        return trimLine.startsWith("#") || trimLine.isEmpty();
    }

    private static String getParentPath(String path) {
        return path.contains(".") ? path.substring(0, path.lastIndexOf('.')) : "";
    }

    private static void correctIndexes(ConfigurationSection section, ConfigurationSection target) {
        if (target == null) {
            return;
        }

        List<Pair<String, Object>> sectionMap = getSectionMap(section);
        List<Pair<String, Object>> correctOrder = new ArrayList<>(sectionMap.size());

        for (Pair<String, Object> entry : sectionMap) {
            correctOrder.add(new Pair<>(entry.key, target.get(entry.key)));
        }

        clearConfiguration(target);

        for (Pair<String, Object> entry : correctOrder) {
            target.set(entry.key, entry.value);
        }
    }

    private static List<Pair<String, Object>> getSectionMap(ConfigurationSection section) {
        List<Pair<String, Object>> list = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            list.add(new Pair<>(key, section.get(key)));
        }

        return list;
    }

    private static void clearConfiguration(ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            section.set(key, null);
        }
    }

    private record Pair<K, V>(K key, V value) {

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Pair<?, ?> other && Objects.equals(this.key, other.key)
                    && Objects.equals(this.value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.key);
        }
    }
}
