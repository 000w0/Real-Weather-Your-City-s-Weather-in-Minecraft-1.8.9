package ru.example.realweather;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.example.realweather.proxy.CommonProxy;

@Mod(modid = RealWeatherMod.MOD_ID, name = "Real Weather", version = "1.1.1",
        clientSideOnly = true, acceptableRemoteVersions = "*")
public class RealWeatherMod {
    public static final String MOD_ID = "realweather";
    public static final Logger LOGGER = LogManager.getLogger("RealWeather");

    @SidedProxy(
            clientSide = "ru.example.realweather.proxy.ClientProxy",
            serverSide = "ru.example.realweather.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }
}
