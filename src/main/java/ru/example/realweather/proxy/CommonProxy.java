package ru.example.realweather.proxy;

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        // The dedicated server never changes its weather for this client-only mod.
    }
}
