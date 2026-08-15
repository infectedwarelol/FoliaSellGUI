package net.mackenziemolloy.shopguiplus.sellgui.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.NotNull;

public final class Hastebin {

    private static final String BASE_URL = "https://paste.helpch.at/";
    private static final String API_URL = "https://paste.helpch.at/documents";
    private static final int TIMEOUT_MILLIS = 15_000;

    public @NotNull String post(@NotNull String text) throws IOException {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = openConnection(payload.length);

        try {
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload);
            }

            String response;
            try (InputStream inputStream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,
                         StandardCharsets.UTF_8))) {
                response = reader.readLine();
            }

            if (response == null || response.isBlank()) {
                throw new IOException("The paste service returned an empty response.");
            }

            int keyIndex = response.indexOf("\"key\"");
            if (keyIndex < 0) {
                return response;
            }

            int start = response.indexOf('"', response.indexOf(':', keyIndex) + 1);
            int end = (start < 0 ? -1 : response.indexOf('"', start + 1));
            if (start < 0 || end < 0) {
                return response;
            }

            return BASE_URL + response.substring(start + 1, end);
        } finally {
            connection.disconnect();
        }
    }

    private static @NotNull HttpURLConnection openConnection(int contentLength) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();

        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", "ShopGUIPlus-SellGUI");
        connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
        connection.setRequestProperty("Content-Length", Integer.toString(contentLength));
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setUseCaches(false);

        return connection;
    }
}
