package ru.example.realweather.client;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public final class GuiHudPosition extends GuiScreen {
    private final ClientWeatherController controller;
    private int panelX;
    private int panelY;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean dragging;

    public GuiHudPosition(ClientWeatherController controller) {
        this.controller = controller;
    }

    @Override
    public void initGui() {
        panelX = controller.getConfig().hudX;
        panelY = controller.getConfig().hudY;
        buttonList.add(new GuiButton(1, width / 2 - 50, height - 28, 100, 20,
                I18n.format("realweather.hudPosition.done")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x55000000);
        int panelWidth = Math.min(HudOverlay.getWidth(mc, controller.getConfig(),
                controller.getSnapshot(), controller.getLastError()), width);
        panelX = Math.max(0, Math.min(panelX, width - panelWidth));
        panelY = Math.max(0, Math.min(panelY, height - HudOverlay.getHeight()));
        HudOverlay.drawAt(mc, controller.getConfig(), controller.getSnapshot(),
                controller.getLastError(), panelX, panelY, panelWidth);
        drawCenteredString(fontRendererObj, I18n.format("realweather.hudPosition.drag"),
                width / 2, height - 43, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mc.currentScreen != this) {
            return;
        }
        int panelWidth = Math.min(HudOverlay.getWidth(mc, controller.getConfig(),
                controller.getSnapshot(), controller.getLastError()), width);
        if (mouseButton == 0 && mouseX >= panelX && mouseX < panelX + panelWidth
                && mouseY >= panelY && mouseY < panelY + HudOverlay.getHeight()) {
            dragging = true;
            dragOffsetX = mouseX - panelX;
            dragOffsetY = mouseY - panelY;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
                                  long timeSinceLastClick) {
        if (dragging && clickedMouseButton == 0) {
            int panelWidth = Math.min(HudOverlay.getWidth(mc, controller.getConfig(),
                    controller.getSnapshot(), controller.getLastError()), width);
            panelX = Math.max(0, Math.min(mouseX - dragOffsetX, width - panelWidth));
            panelY = Math.max(0, Math.min(mouseY - dragOffsetY, height - HudOverlay.getHeight()));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (dragging) {
            dragging = false;
            controller.getConfig().setHudPosition(panelX, panelY);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) {
            controller.getConfig().setHudPosition(panelX, panelY);
            mc.displayGuiScreen(new GuiWeatherSettings(controller));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            controller.getConfig().setHudPosition(panelX, panelY);
            mc.displayGuiScreen(new GuiWeatherSettings(controller));
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
