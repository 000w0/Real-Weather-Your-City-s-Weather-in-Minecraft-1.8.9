package ru.example.realweather.client;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

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
        String title = config.cityName == null || config.cityName.isEmpty() ? "Погода" : config.cityName;
        if (snapshot == null) {
            return new String[] {title, error == null ? "Загрузка данных..." : "Нет данных",
                    error == null ? "O — настройки" : error};
        }
        String temperature = String.format(Locale.ROOT, "%+.1f°C", snapshot.temperatureCelsius);
        String weather = weatherName(snapshot.condition);
        ZonedDateTime cityNow = ZonedDateTime.now(snapshot.cityZone);
        String observed = CLOCK.format(snapshot.observationTime);
        String status = error == null
                ? CLOCK.format(cityNow) + "  ·  данные " + observed
                : "Нет связи · данные от " + observed;
        return new String[] {title, temperature + "  ·  " + weather, status};
    }

    private static String weatherName(WeatherCondition condition) {
        switch (condition) {
            case CLEAR: return "Ясно";
            case RAIN: return "Дождь";
            case SNOW: return "Снег";
            case THUNDER: return "Гроза";
            default: return "Погода";
        }
    }
}
