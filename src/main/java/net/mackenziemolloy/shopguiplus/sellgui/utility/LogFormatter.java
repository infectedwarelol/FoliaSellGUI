package net.mackenziemolloy.shopguiplus.sellgui.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LogFormatter extends Formatter {

    private static final String DEFAULT_PATTERN = "yyyy/MM/dd HH:mm:ss";

    private final DateTimeFormatter dateFormatter;

    public LogFormatter(@Nullable String pattern) {
        this.dateFormatter = createFormatter(pattern);
    }

    @Override
    public @NotNull String format(@NotNull LogRecord logRecord) {
        return this.dateFormatter.format(LocalDateTime.now()) + " " + logRecord.getMessage() + System.lineSeparator();
    }

    private static @NotNull DateTimeFormatter createFormatter(@Nullable String pattern) {
        if (pattern != null && !pattern.isBlank()) {
            try {
                return DateTimeFormatter.ofPattern(pattern, Locale.US);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return DateTimeFormatter.ofPattern(DEFAULT_PATTERN, Locale.US);
    }
}
