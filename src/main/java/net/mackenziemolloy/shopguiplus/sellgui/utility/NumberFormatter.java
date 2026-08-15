package net.mackenziemolloy.shopguiplus.sellgui.utility;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NumberFormatter {

    private static final List<String> QUANTITY_CODES = List.of("K", "M", "B", "T", "Q", "Qa", "Sx", "Sp", "Oc", "No",
            "De", "Un", "Du", "Tr", "Qu", "Qi", "Se", "Sev", "Oc", "Nov", "Vg", "C");

    private final boolean rounded;
    private final boolean removeTrailingZeros;
    private final boolean abbreviate;

    public NumberFormatter(boolean rounded, boolean removeTrailingZeros, boolean abbreviate) {
        this.rounded = rounded;
        this.removeTrailingZeros = removeTrailingZeros;
        this.abbreviate = abbreviate;
    }

    public @NotNull String money(double value) {
        String result = this.rounded ? String.format(Locale.US, "%,.2f", value) : Double.toString(value);

        if (this.removeTrailingZeros) {
            result = stripZeroFraction(result);
        }

        if (this.abbreviate && !this.rounded) {
            Double parsed = parseOrNull(result);
            if (parsed != null) {
                return abbreviate(parsed);
            }
        }

        return result;
    }

    public @NotNull String count(long value) {
        if (this.abbreviate && !this.rounded) {
            return abbreviate((double) value);
        }

        if (this.rounded) {
            return String.format(Locale.US, "%,d", value);
        }

        return Long.toString(value);
    }

    private @NotNull String abbreviate(double count) {
        double absolute = Math.abs(count);
        if (absolute < 1000.0D) {
            return formatWhole(count);
        }

        int exponent = (int) (Math.log(absolute) / Math.log(1000.0D));
        if (exponent < 1 || exponent > QUANTITY_CODES.size()) {
            return formatWhole(count);
        }

        String value = String.format(Locale.US, "%.2f", count / Math.pow(1000.0D, exponent));
        value = stripZeroFraction(value);
        if (value.indexOf('.') >= 0) {
            while (value.endsWith("0")) {
                value = value.substring(0, value.length() - 1);
            }

            if (value.endsWith(".")) {
                value = value.substring(0, value.length() - 1);
            }
        }

        return value + QUANTITY_CODES.get(exponent - 1);
    }

    private static @NotNull String formatWhole(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }

        return Double.toString(value);
    }

    private static @NotNull String stripZeroFraction(@NotNull String value) {
        int dotIndex = value.indexOf('.');
        if (dotIndex < 0) {
            return value;
        }

        for (int index = (dotIndex + 1); index < value.length(); index++) {
            if (value.charAt(index) != '0') {
                return value;
            }
        }

        return value.substring(0, dotIndex);
    }

    private static @Nullable Double parseOrNull(@NotNull String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
