package com.g1739.firmalifegreenhousepatch.client;

import com.g1739.firmalifegreenhousepatch.common.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class PatchClientEventHandler
{
    private PatchClientEventHandler() {}

    public static void init(IEventBus bus)
    {
        bus.addListener(PatchClientEventHandler::registerMenuScreens);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event)
    {
        event.register(ModMenuTypes.CLIMATE_STATION_TEMPERATURE.get(), ClimateStationTemperatureScreen::new);
    }
}
