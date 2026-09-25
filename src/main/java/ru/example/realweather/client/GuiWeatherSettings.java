package ru.example.realweather.client;

import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public final class GuiWeatherSettings extends GuiScreen {
    private final ClientWeatherController controller;
    private GuiTextField cityField;
    private GuiTextField latitudeField;
    private GuiTextField longitudeField;
    private GuiTextField intervalField;
    private GuiButton searchButton;
    private GuiButton enabledButton;
    private GuiButton timeButton;
    private boolean enabled;
    private boolean syncDayNight;
    private String message = "";

    public GuiWeatherSettings(ClientWeatherController controller) {
        this.controller = controller;
    }

    @Override
    public void initGui() {
        WeatherConfig config = controller.getConfig();
        int left = width / 2 - 120;
        String draftCity = cityField == null ? config.cityName : cityField.getText();
        String draftLatitude = latitudeField == null ? Double.toString(config.latitude) : latitudeField.getText();
        String draftLongitude = longitudeField == null ? Double.toString(config.longitude) : longitudeField.getText();
        String draftInterval = intervalField == null
                ? Integer.toString(config.updateIntervalMinutes) : intervalField.getText();
        if (cityField == null) {
            enabled = config.enabled;
            syncDayNight = config.syncDayNight;
        }

        cityField = new GuiTextField(1, fontRendererObj, left, 45, 160, 18);
        cityField.setMaxStringLength(100);
        cityField.setText(draftCity);
        latitudeField = new GuiTextField(2, fontRendererObj, left, 81, 115, 18);
        latitudeField.setMaxStringLength(24);
        latitudeField.setText(draftLatitude);
        longitudeField = new GuiTextField(3, fontRendererObj, left + 125, 81, 115, 18);
        longitudeField.setMaxStringLength(24);
        longitudeField.setText(draftLongitude);
        intervalField = new GuiTextField(4, fontRendererObj, left, 117, 115, 18);
        intervalField.setMaxStringLength(6);
        intervalField.setText(draftInterval);

        searchButton = new GuiButton(1, left + 165, 44, 75, 20, "Найти");
        enabledButton = new GuiButton(2, left + 125, 116, 115, 20, enabledLabel());
        timeButton = new GuiButton(3, left, 143, 240, 20, timeLabel());
        buttonList.add(searchButton);
        buttonList.add(enabledButton);
        buttonList.add(timeButton);
        buttonList.add(new GuiButton(4, left, 169, 240, 20, "Переместить панель"));
        buttonList.add(new GuiButton(5, left, 195, 115, 20, "Сохранить"));
        buttonList.add(new GuiButton(6, left + 125, 195, 115, 20, "Закрыть"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = width / 2 - 120;
        drawCenteredString(fontRendererObj, "Real Weather — настройки", width / 2, 15, 0xFFFFFF);
        fontRendererObj.drawStringWithShadow("Город", left, 33, 0xCCCCCC);
        fontRendererObj.drawStringWithShadow("Широта", left, 69, 0xCCCCCC);
        fontRendererObj.drawStringWithShadow("Долгота", left + 125, 69, 0xCCCCCC);
        fontRendererObj.drawStringWithShadow("Интервал (мин)", left, 105, 0xCCCCCC);
        cityField.drawTextBox();
        latitudeField.drawTextBox();
        longitudeField.drawTextBox();
        intervalField.drawTextBox();
        if (!message.isEmpty()) {
            drawCenteredString(fontRendererObj,
                    fontRendererObj.trimStringToWidth(message, width - 12),
                    width / 2, 221, 0xFFCC88);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void updateScreen() {
        cityField.updateCursorCounter();
        latitudeField.updateCursorCounter();
        longitudeField.updateCursorCounter();
        intervalField.updateCursorCounter();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        cityField.mouseClicked(mouseX, mouseY, mouseButton);
        latitudeField.mouseClicked(mouseX, mouseY, mouseButton);
        longitudeField.mouseClicked(mouseX, mouseY, mouseButton);
        intervalField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (cityField.textboxKeyTyped(typedChar, keyCode)
                || latitudeField.textboxKeyTyped(typedChar, keyCode)
                || longitudeField.textboxKeyTyped(typedChar, keyCode)
                || intervalField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 1:
                searchCity();
                break;
            case 2:
                enabled = !enabled;
                enabledButton.displayString = enabledLabel();
                break;
            case 3:
                syncDayNight = !syncDayNight;
                timeButton.displayString = timeLabel();
                break;
            case 4:
                if (saveSettings()) {
                    mc.displayGuiScreen(new GuiHudPosition(controller));
                }
                break;
            case 5:
                if (saveSettings()) {
                    mc.displayGuiScreen(null);
                }
                break;
            case 6:
                mc.displayGuiScreen(null);
                break;
            default:
                break;
        }
    }

    private void searchCity() {
        final String query = cityField.getText().trim();
        if (query.length() < 2) {
            message = "Введите минимум 2 символа для поиска города";
            return;
        }
        searchButton.enabled = false;
        message = "Поиск города...";
        controller.searchCity(query, new ClientWeatherController.CitySearchCallback() {
            @Override
            public void complete(CityLocation city, String error) {
                if (mc.currentScreen != GuiWeatherSettings.this) {
                    return;
                }
                searchButton.enabled = true;
                if (city == null) {
                    message = "Поиск не удался: " + error;
                    return;
                }
                cityField.setText(city.displayName);
                latitudeField.setText(String.format(Locale.ROOT, "%.5f", city.latitude));
                longitudeField.setText(String.format(Locale.ROOT, "%.5f", city.longitude));
                message = "Найдено: " + city.displayName + ". Нажмите «Сохранить».";
            }
        });
    }

    private boolean saveSettings() {
        try {
            double latitude = Double.parseDouble(latitudeField.getText().trim().replace(',', '.'));
            double longitude = Double.parseDouble(longitudeField.getText().trim().replace(',', '.'));
            int interval = Integer.parseInt(intervalField.getText().trim());
            String error = controller.getConfig().update(enabled, syncDayNight,
                    cityField.getText(), latitude, longitude, interval);
            if (error != null) {
                message = error;
                return false;
            }
            controller.settingsChanged();
            return true;
        } catch (NumberFormatException e) {
            message = "Проверьте числа: широту, долготу и интервал";
            return false;
        }
    }

    private String enabledLabel() {
        return "Мод: " + (enabled ? "вкл" : "выкл");
    }

    private String timeLabel() {
        return "Время города: " + (syncDayNight ? "вкл" : "выкл");
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
