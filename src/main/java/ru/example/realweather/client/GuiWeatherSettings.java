package ru.example.realweather.client;

import java.io.IOException;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

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

        searchButton = new GuiButton(1, left + 165, 44, 75, 20, I18n.format("realweather.settings.search"));
        enabledButton = new GuiButton(2, left + 125, 116, 115, 20, enabledLabel());
        timeButton = new GuiButton(3, left, 143, 240, 20, timeLabel());
        buttonList.add(searchButton);
        buttonList.add(enabledButton);
        buttonList.add(timeButton);
        buttonList.add(new GuiButton(4, left, 169, 240, 20, I18n.format("realweather.settings.movePanel")));
        buttonList.add(new GuiButton(5, left, 195, 115, 20, I18n.format("realweather.settings.save")));
        buttonList.add(new GuiButton(6, left + 125, 195, 115, 20, I18n.format("realweather.settings.close")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = width / 2 - 120;
        drawCenteredString(fontRendererObj, I18n.format("realweather.settings.title"), width / 2, 15, 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(I18n.format("realweather.settings.city"), left, 33, 0xCCCCCC);
        fontRendererObj.drawStringWithShadow(I18n.format("realweather.settings.latitude"), left, 69, 0xCCCCCC);
        fontRendererObj.drawStringWithShadow(I18n.format("realweather.settings.longitude"), left + 125, 69, 0xCCCCCC);
        fontRendererObj.drawStringWithShadow(I18n.format("realweather.settings.interval"), left, 105, 0xCCCCCC);
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
            message = I18n.format("realweather.settings.searchTooShort");
            return;
        }
        searchButton.enabled = false;
        message = I18n.format("realweather.settings.searching");
        controller.searchCity(query, new ClientWeatherController.CitySearchCallback() {
            @Override
            public void complete(CityLocation city, String error) {
                if (mc.currentScreen != GuiWeatherSettings.this) {
                    return;
                }
                searchButton.enabled = true;
                if (city == null) {
                    message = I18n.format(error == null
                            ? "realweather.settings.cityNotFound" : "realweather.settings.searchFailed");
                    return;
                }
                cityField.setText(city.displayName);
                latitudeField.setText(String.format(Locale.ROOT, "%.5f", city.latitude));
                longitudeField.setText(String.format(Locale.ROOT, "%.5f", city.longitude));
                message = I18n.format("realweather.settings.found", city.displayName);
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
                message = I18n.format(error);
                return false;
            }
            controller.settingsChanged();
            return true;
        } catch (NumberFormatException e) {
            message = I18n.format("realweather.settings.invalidNumbers");
            return false;
        }
    }

    private String enabledLabel() {
        return I18n.format("realweather.settings.mod", I18n.format(enabled
                ? "realweather.common.on" : "realweather.common.off"));
    }

    private String timeLabel() {
        return I18n.format("realweather.settings.cityTime", I18n.format(syncDayNight
                ? "realweather.common.on" : "realweather.common.off"));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
