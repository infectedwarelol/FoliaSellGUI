package net.mackenziemolloy.shopguiplus.sellgui.utility.sirblobman;

import org.jetbrains.annotations.NotNull;

public final class MessageUtility {

    public static final char COLOR_CHAR = '§';

    private static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    private MessageUtility() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull String color(@NotNull String message) {
        char[] characters = message.toCharArray();

        for (int index = 0; index < (characters.length - 1); index++) {
            if (characters[index] != '&' || ALL_CODES.indexOf(characters[index + 1]) < 0) {
                continue;
            }

            characters[index] = COLOR_CHAR;
            characters[index + 1] = Character.toLowerCase(characters[index + 1]);
        }

        return new String(characters);
    }
}
