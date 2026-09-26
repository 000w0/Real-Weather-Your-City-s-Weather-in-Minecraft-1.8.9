package ru.example.realweather.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Looks up a nearby settlement for coordinates entered in the settings screen. */
public final class ReverseGeocodingClient {
    private static final int TIMEOUT_MS = 5000;
    private static final int MAX_RESPONSE_CHARS = 16384;

    private ReverseGeocodingClient() {
    }

    public static String findCity(double latitude, double longitude, String language) throws IOException {
        if (!WeatherConfig.validCoordinates(latitude, longitude)) {
            throw new IOException("Coordinates are outside the valid range");
        }
        String address = String.format(Locale.ROOT,
                "https://photon.komoot.io/reverse?lat=%.6f&lon=%.6f&limit=1"
                        + "&layer=city&layer=locality&radius=50&lang=%s",
                latitude, longitude, language);
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "RealWeatherMinecraftForge/1.3.0");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Photon returned HTTP " + status);
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buffer = new char[2048];
                int length;
                while ((length = reader.read(buffer)) != -1) {
                    if (body.length() + length > MAX_RESPONSE_CHARS) {
                        throw new IOException("Photon response is too large");
                    }
                    body.append(buffer, 0, length);
                }
            }
            try {
                JsonObject root = new JsonParser().parse(body.toString()).getAsJsonObject();
                JsonArray features = root.getAsJsonArray("features");
                if (features == null || features.size() == 0) {
                    return null;
                }
                JsonObject properties = features.get(0).getAsJsonObject().getAsJsonObject("properties");
                String name = value(properties, "name");
                if (name == null) {
                    return null;
                }
                String country = value(properties, "country");
                return country == null || name.equalsIgnoreCase(country) ? name : name + ", " + country;
            } catch (RuntimeException e) {
                throw new IOException("Invalid Photon reverse geocoding response: " + e.getMessage(), e);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String value(JsonObject object, String key) {
        if (object == null) {
            return null;
        }
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        String text = value.getAsString().trim();
        return text.isEmpty() ? null : text;
    }
}
