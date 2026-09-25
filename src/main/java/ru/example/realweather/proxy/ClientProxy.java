package ru.example.realweather.proxy;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import ru.example.realweather.client.ClientWeatherController;
import ru.example.realweather.client.WeatherConfig;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        WeatherConfig config = WeatherConfig.load(event.getSuggestedConfigurationFile());
        KeyBinding settingsKey = new KeyBinding("key.realweather.settings", Keyboard.KEY_O,
                "key.categories.misc");
        ClientRegistry.registerKeyBinding(settingsKey);
        ClientWeatherController controller = new ClientWeatherController(config, settingsKey);
        FMLCommonHandler.instance().bus().register(controller);
        MinecraftForge.EVENT_BUS.register(controller);
    }
}
