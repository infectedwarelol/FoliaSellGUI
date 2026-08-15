package net.mackenziemolloy.shopguiplus.sellgui.utility;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.mackenziemolloy.shopguiplus.sellgui.compat.Schedulers;
import org.jetbrains.annotations.NotNull;

public final class UpdateChecker {

    private static final String UPDATE_URL = "https://api.spigotmc.org/legacy/update.php?resource=%s";
    private static final int TIMEOUT_MILLIS = 10_000;

    private final Logger logger;
    private final Schedulers schedulers;
    private final int resourceId;

    public UpdateChecker(@NotNull Logger logger, @NotNull Schedulers schedulers, int resourceId) {
        this.logger = logger;
        this.schedulers = schedulers;
        this.resourceId = resourceId;
    }

    public void check(@NotNull Consumer<String> consumer) {
        this.schedulers.async(() -> {
            String version = fetchVersion();
            if (version != null) {
                consumer.accept(version);
            }
        });
    }

    private String fetchVersion() {
        String url = String.format(Locale.US, UPDATE_URL, this.resourceId);

        try {
            URLConnection connection = URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);

            try (InputStream inputStream = connection.getInputStream();
                 Scanner scanner = new Scanner(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                if (scanner.hasNext()) {
                    return scanner.next();
                }
            } finally {
                if (connection instanceof HttpURLConnection httpConnection) {
                    httpConnection.disconnect();
                }
            }
        } catch (Exception exception) {
            this.logger.log(Level.INFO, "Failed to check for updates because an error occurred: "
                    + exception.getMessage());
        }

        return null;
    }
}
