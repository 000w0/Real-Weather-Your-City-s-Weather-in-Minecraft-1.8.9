package ru.example.realweather.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import ru.example.realweather.RealWeatherMod;

public final class OpenMeteoClient {
    private static final int TIMEOUT_MS = 5000;
    private static final int MAX_RESPONSE_CHARS = 16384;

    private OpenMeteoClient() {
    }

    public static WeatherSnapshot fetch(double latitude, double longitude) throws IOException {
        String url = String.format(Locale.ROOT,
                "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f"
                        + "&current=temperature_2m,weather_code&timezone=auto&forecast_days=1",
                latitude, longitude);
        JsonObject root = getJson(new URL(url));
        try {
            JsonObject current = requiredObject(root, "current");
            int code = required(current, "weather_code").getAsInt();
            WeatherCondition condition = WeatherCondition.fromOpenMeteoCode(code);
            if (condition == null) {
                throw new IOException("Open-Meteo returned unknown weather code " + code);
            }
            double temperature = required(current, "temperature_2m").getAsDouble();
            if (Double.isNaN(temperature) || Double.isInfinite(temperature)) {
                throw new IOException("Open-Meteo returned invalid temperature");
            }
            ZoneId zone = cityZone(root);
            LocalDateTime observationTime = LocalDateTime.parse(required(current, "time").getAsString());
            return new WeatherSnapshot(condition, temperature, zone, observationTime, Instant.now());
        } catch (RuntimeException e) {
            throw new IOException("Invalid Open-Meteo weather response: " + e.getMessage(), e);
        }
    }

    private static ZoneId cityZone(JsonObject root) throws IOException {
        String timeZoneName = required(root, "timezone").getAsString();
        try {
            return ZoneId.of(timeZoneName);
        } catch (DateTimeException unsupportedByRuntime) {
            // Older Java 8 time-zone databases know the former Kyiv identifier.
            if ("Europe/Kyiv".equals(timeZoneName)) {
                try {
                    return ZoneId.of("Europe/Kiev");
                } catch (DateTimeException ignored) {
                    // Fall through to the offset supplied with this API response.
                }
            }

            JsonElement offsetElement = root.get("utc_offset_seconds");
            if (offsetElement != null && !offsetElement.isJsonNull()) {
                int offsetSeconds = offsetElement.getAsInt();
                if (offsetSeconds >= -64800 && offsetSeconds <= 64800) {
                    RealWeatherMod.LOGGER.warn("Java does not recognize time zone '{}'; using the "
                            + "Open-Meteo UTC offset ({} seconds) until the next update.",
                            timeZoneName, offsetSeconds);
                    return ZoneOffset.ofTotalSeconds(offsetSeconds);
                }
            }

            RealWeatherMod.LOGGER.warn("Java does not recognize time zone '{}' and Open-Meteo "
                    + "did not provide a valid UTC offset; using UTC for the in-game clock.", timeZoneName);
            return ZoneOffset.UTC;
        }
    }

    public static CityLocation searchCity(String query, String language) throws IOException {
        String trimmed = query.trim();
        if (trimmed.length() < 2) {
            throw new IOException("Enter at least two characters in the city name");
        }
        String url = "https://geocoding-api.open-meteo.com/v1/search?name="
                + URLEncoder.encode(trimmed, "UTF-8") + "&count=1&language="
                + URLEncoder.encode(language, "UTF-8") + "&format=json";
        JsonObject root = getJson(new URL(url));
        try {
            JsonElement results = root.get("results");
            if (results == null || !results.isJsonArray()) {
                return null;
            }
            JsonArray array = results.getAsJsonArray();
            if (array.size() == 0) {
                return null;
            }
            JsonObject city = array.get(0).getAsJsonObject();
            String name = required(city, "name").getAsString();
            if (city.has("country") && !city.get("country").isJsonNull()) {
                name += ", " + city.get("country").getAsString();
            }
            double latitude = required(city, "latitude").getAsDouble();
            double longitude = required(city, "longitude").getAsDouble();
            if (Double.isNaN(latitude) || Double.isInfinite(latitude) || latitude < -90 || latitude > 90
                    || Double.isNaN(longitude) || Double.isInfinite(longitude)
                    || longitude < -180 || longitude > 180) {
                throw new IOException("Geocoding returned invalid coordinates");
            }
            return new CityLocation(name, latitude, longitude);
        } catch (RuntimeException e) {
            throw new IOException("Invalid Open-Meteo city response: " + e.getMessage(), e);
        }
    }

    private static JsonObject getJson(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                String reason = readApiReason(connection.getErrorStream());
                throw new IOException("Open-Meteo returned HTTP " + status + reason);
            }
            JsonObject root = parseObject(readLimited(connection.getInputStream()));
            if (root.has("error") && root.get("error").getAsBoolean()) {
                throw new IOException("Open-Meteo API error: " + getReason(root));
            }
            return root;
        } catch (RuntimeException e) {
            throw new IOException("Invalid Open-Meteo JSON response: " + e.getMessage(), e);
        } finally {
            connection.disconnect();
        }
    }

    private static JsonElement required(JsonObject parent, String key) throws IOException {
        JsonElement value = parent.get(key);
        if (value == null || value.isJsonNull()) {
            throw new IOException("Open-Meteo response has no " + key);
        }
        return value;
    }

    private static JsonObject requiredObject(JsonObject parent, String key) throws IOException {
        JsonElement value = required(parent, key);
        if (!value.isJsonObject()) {
            throw new IOException("Open-Meteo field " + key + " is not an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonObject parseObject(String json) throws IOException {
        try {
            JsonElement element = new JsonParser().parse(json);
            if (!element.isJsonObject()) {
                throw new IOException("Open-Meteo response is not a JSON object");
            }
            return element.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Cannot parse Open-Meteo JSON: " + e.getMessage(), e);
        }
    }

    private static String readApiReason(InputStream stream) {
        if (stream == null) {
            return "";
        }
        try {
            return ": " + getReason(parseObject(readLimited(stream)));
        } catch (IOException e) {
            return "";
        }
    }

    private static String getReason(JsonObject object) {
        JsonElement reason = object.get("reason");
        return reason != null && !reason.isJsonNull() ? reason.getAsString() : "unknown reason";
    }

    private static String readLimited(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            char[] buffer = new char[2048];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                if (body.length() + length > MAX_RESPONSE_CHARS) {
                    throw new IOException("Open-Meteo response is too large");
                }
                body.append(buffer, 0, length);
            }
            return body.toString();
        }
    }
}
