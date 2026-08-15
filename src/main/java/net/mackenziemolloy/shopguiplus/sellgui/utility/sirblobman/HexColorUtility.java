package net.mackenziemolloy.shopguiplus.sellgui.utility.sirblobman;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

public final class HexColorUtility {

    private static final Map<Character, Pattern> KNOWN_PATTERN_MAP = new ConcurrentHashMap<>();
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("[&§][0-9a-fA-FkKlLmMnNoOrRxX]");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("[&§]#([0-9a-fA-F]){6}");

    private HexColorUtility() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull String replaceHexColors(char colorChar, @NotNull String string) {
        Pattern pattern = getReplaceAllRgbPattern(colorChar);
        Matcher matcher = pattern.matcher(string);

        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                matcher.appendReplacement(buffer, colorChar + "#$2");
                continue;
            }

            try {
                String hexCode = parseHexColor(matcher.group(2));
                matcher.appendReplacement(buffer, hexCode);
            } catch (NumberFormatException ignored) {
            }
        }

        matcher.appendTail(buffer);

        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(buffer.toString());
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static @NotNull String purgeAllColor(@NotNull String input) {
        String withoutHex = HEX_COLOR_PATTERN.matcher(input).replaceAll("");
        return LEGACY_COLOR_PATTERN.matcher(withoutHex).replaceAll("");
    }

    private static @NotNull Pattern getReplaceAllRgbPattern(char colorChar) {
        return KNOWN_PATTERN_MAP.computeIfAbsent(colorChar, character -> {
            String colorCharPattern = Pattern.quote(Character.toString(character));
            return Pattern.compile("(" + colorCharPattern + ")?" + colorCharPattern + "#([0-9a-fA-F]{6})");
        });
    }

    private static @NotNull String parseHexColor(@NotNull String string) throws NumberFormatException {
        String value = (string.startsWith("#") ? string.substring(1) : string);
        if (value.length() != 6) {
            throw new NumberFormatException("Invalid hex length!");
        }

        int colorInt = Integer.decode("#" + value);
        if (colorInt < 0x000000 || colorInt > 0xFFFFFF) {
            throw new NumberFormatException("Invalid hex color value!");
        }

        StringBuilder assembled = new StringBuilder();
        assembled.append(MessageUtility.COLOR_CHAR).append('x');

        for (char character : value.toCharArray()) {
            assembled.append(MessageUtility.COLOR_CHAR).append(character);
        }

        return assembled.toString();
    }
}
