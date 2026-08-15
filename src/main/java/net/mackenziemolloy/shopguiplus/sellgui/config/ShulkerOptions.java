package net.mackenziemolloy.shopguiplus.sellgui.config;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;

public record ShulkerOptions(boolean enabled, boolean includeContainerPrice, int maxDepth, int maxScannedItems,
                             @NotNull UnsellablePolicy unsellablePolicy, @NotNull ReceiptMode receiptMode,
                             boolean showEstimate) {

    public enum UnsellablePolicy {
        KEEP_ITEMS,
        RETURN_BOX,
        DISCARD;

        public static @NotNull UnsellablePolicy parse(String value, @NotNull UnsellablePolicy fallback) {
            if (value == null) {
                return fallback;
            }

            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }

    public enum ReceiptMode {
        SUMMARY,
        DETAILED;

        public static @NotNull ReceiptMode parse(String value, @NotNull ReceiptMode fallback) {
            if (value == null) {
                return fallback;
            }

            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
    }
}
