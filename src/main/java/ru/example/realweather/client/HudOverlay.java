package ru.example.realweather.client;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

public final class HudOverlay {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final int HEIGHT = 48;

    private HudOverlay() {
    }

    public static int getWidth(Minecraft minecraft, WeatherConfig config,
                               WeatherSnapshot snapshot, String error) {
        String[] lines = lines(config, snapshot, error);
        FontRenderer font = minecraft.fontRendererObj;
        int width = 150;
        for (String line : lines) {
            width = Math.max(width, font.getStringWidth(line) + 12);
        }
        return width;
    }

    public static int getHeight() {
        return HEIGHT;
    }

    public static void draw(Minecraft minecraft, WeatherConfig config, WeatherSnapshot snapshot,
                            String error, int screenWidth, int screenHeight) {
        int panelWidth = Math.min(getWidth(minecraft, config, snapshot, error), screenWidth);
        int x = Math.max(0, Math.min(config.hudX, screenWidth - panelWidth));
        int y = Math.max(0, Math.min(config.hudY, screenHeight - HEIGHT));
        drawAt(minecraft, config, snapshot, error, x, y, panelWidth);
    }

    public static void drawAt(Minecraft minecraft, WeatherConfig config, WeatherSnapshot snapshot,
                              String error, int x, int y, int panelWidth) {
        String[] lines = lines(config, snapshot, error);
        FontRenderer font = minecraft.fontRendererObj;
        Gui.drawRect(x, y, x + panelWidth, y + HEIGHT, 0xAA101820);
        Gui.drawRect(x, y, x + 2, y + HEIGHT, 0xFF55C8FF);
        font.drawStringWithShadow(font.trimStringToWidth(lines[0], panelWidth - 12),
                x + 6, y + 5, 0x70D8FF);
        font.drawStringWithShadow(font.trimStringToWidth(lines[1], panelWidth - 12),
                x + 6, y + 19, 0xFFFFFF);
        font.drawStringWithShadow(font.trimStringToWidth(lines[2], panelWidth - 12),
                x + 6, y + 33, error == null ? 0xBBBBBB : 0xFFAA88);
    }

    private static String[] lines(WeatherConfig config, WeatherSnapshot snapshot, String error) {
        String title = config.cityName == null || config.cityName.isEmpty()
                ? I18n.format("realweather.hud.weather") : config.cityName;
        if (snapshot == null) {
            return new String[] {title,
                    I18n.format(error == null ? "realweather.hud.loading" : "realweather.hud.noData"),
                    I18n.format(error == null ? "realweather.hud.openSettings" : error)};
        }
        String temperature = String.format(Locale.ROOT, "%+.1f°C", snapshot.temperatureCelsius);
        String weather = weatherName(snapshot.condition);
        ZonedDateTime cityNow = ZonedDateTime.now(snapshot.cityZone);
        String observed = CLOCK.format(snapshot.observationTime);
        String status = error == null
                ? I18n.format("realweather.hud.observed", CLOCK.format(cityNow), observed)
                : I18n.format("realweather.hud.disconnected", observed);
        return new String[] {title, temperature + "  ·  " + weather, status};
    }

    private static String weatherName(WeatherCondition condition) {
        switch (condition) {
            case CLEAR: return I18n.format("realweather.weather.clear");
            case RAIN: return I18n.format("realweather.weather.rain");
            case SNOW: return I18n.format("realweather.weather.snow");
            case THUNDER: return I18n.format("realweather.weather.thunder");
            default: return I18n.format("realweather.hud.weather");
        }
    }
}
