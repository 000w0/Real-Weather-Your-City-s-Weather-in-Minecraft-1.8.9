package ru.example.realweather.client;

import java.io.File;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import ru.example.realweather.RealWeatherMod;

public final class WeatherConfig {
    private static final String CATEGORY = Configuration.CATEGORY_GENERAL;

    private final Configuration configuration;
    public boolean enabled;
    public boolean syncDayNight;
    public String cityName;
    public double latitude;
    public double longitude;
    public int updateIntervalMinutes;
    public int hudX;
    public int hudY;
    public boolean valid;

    private WeatherConfig(Configuration configuration) {
        this.configuration = configuration;
    }

    public static WeatherConfig load(File file) {
        Configuration configuration = new Configuration(file);
        configuration.load();
        WeatherConfig config = new WeatherConfig(configuration);
        config.enabled = configuration.getBoolean("enabled", CATEGORY, true,
                "Enable client-side weather synchronization.");
        config.syncDayNight = configuration.getBoolean("sync_day_night", CATEGORY, true,
                "Match the client sky to the city's local time (06:00 sunrise, 12:00 noon).");
        config.cityName = configuration.get(CATEGORY, "city_name", "Kyiv",
                "Name shown on the HUD. Use the in-game search to find coordinates.").getString();
        Property latitude = configuration.get(CATEGORY, "latitude", 50.4501D,
                "Example: Kyiv, Ukraine. Valid range: -90 to 90.");
        Property longitude = configuration.get(CATEGORY, "longitude", 30.5234D,
                "Example: Kyiv, Ukraine. Valid range: -180 to 180.");
        Property interval = configuration.get(CATEGORY, "update_interval_minutes", 15,
                "Open-Meteo polling interval in minutes; valid range: 1 to 1440.");
        config.hudX = Math.max(0, parseInt(configuration.get(CATEGORY, "hud_x", 6).getString(), "hud_x"));
        config.hudY = Math.max(0, parseInt(configuration.get(CATEGORY, "hud_y", 6).getString(), "hud_y"));
        if (configuration.hasChanged()) {
            configuration.save();
        }

        config.latitude = parseDouble(latitude.getString(), "latitude");
        config.longitude = parseDouble(longitude.getString(), "longitude");
        config.updateIntervalMinutes = parseInt(interval.getString(), "update_interval_minutes");
        config.valid = validCoordinates(config.latitude, config.longitude)
                && validInterval(config.updateIntervalMinutes);
        if (!config.valid) {
            RealWeatherMod.LOGGER.warn("Invalid weather coordinates or interval in {}. "
                    + "Open-Meteo requests are disabled until corrected in the in-game settings.",
                    file.getAbsolutePath());
        }
        return config;
    }

    public String update(boolean enabled, boolean syncDayNight, String cityName,
                         double latitude, double longitude, int interval) {
        if (!validCoordinates(latitude, longitude)) {
            return "realweather.settings.invalidCoordinates";
        }
        if (!validInterval(interval)) {
            return "realweather.settings.invalidInterval";
        }
        this.enabled = enabled;
        this.syncDayNight = syncDayNight;
        this.cityName = cityName.trim();
        this.latitude = latitude;
        this.longitude = longitude;
        this.updateIntervalMinutes = interval;
        this.valid = true;
        save();
        return null;
    }

    public void setHudPosition(int x, int y) {
        hudX = Math.max(0, x);
        hudY = Math.max(0, y);
        save();
    }

    private void save() {
        configuration.get(CATEGORY, "enabled", true).set(enabled);
        configuration.get(CATEGORY, "sync_day_night", true).set(syncDayNight);
        configuration.get(CATEGORY, "city_name", "Kyiv").set(cityName);
        configuration.get(CATEGORY, "latitude", 50.4501D).set(latitude);
        configuration.get(CATEGORY, "longitude", 30.5234D).set(longitude);
        configuration.get(CATEGORY, "update_interval_minutes", 15).set(updateIntervalMinutes);
        configuration.get(CATEGORY, "hud_x", 6).set(hudX);
        configuration.get(CATEGORY, "hud_y", 6).set(hudY);
        configuration.save();
    }

    private static boolean validCoordinates(double latitude, double longitude) {
        return !Double.isNaN(latitude) && !Double.isInfinite(latitude)
                && latitude >= -90.0D && latitude <= 90.0D
                && !Double.isNaN(longitude) && !Double.isInfinite(longitude)
                && longitude >= -180.0D && longitude <= 180.0D;
    }

    private static boolean validInterval(int interval) {
        return interval >= 1 && interval <= 1440;
    }

    private static double parseDouble(String value, String name) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            RealWeatherMod.LOGGER.warn("Invalid number for {}: '{}'", name, value);
            return Double.NaN;
        }
    }

    private static int parseInt(String value, String name) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            RealWeatherMod.LOGGER.warn("Invalid integer for {}: '{}'", name, value);
            return -1;
        }
    }
}
