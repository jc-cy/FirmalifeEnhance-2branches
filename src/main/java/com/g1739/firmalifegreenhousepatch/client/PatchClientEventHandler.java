package com.g1739.firmalifegreenhousepatch.client;

import com.g1739.firmalifegreenhousepatch.common.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class PatchClientEventHandler
{
    private PatchClientEventHandler() {}

    public static void init(IEventBus bus)
    {
        bus.addListener(PatchClientEventHandler::registerMenuScreens);
    }

    private static void registerMenuScreens(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> MenuScreens.register(ModMenuTypes.CLIMATE_STATION_TEMPERATURE.get(), ClimateStationTemperatureScreen::new));
    }
}
