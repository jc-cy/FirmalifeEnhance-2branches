package com.g1739.firmalifegreenhousepatch.common;

import com.g1739.firmalifegreenhousepatch.FirmalifeGreenhousePatch;
import com.g1739.firmalifegreenhousepatch.common.menu.ClimateStationTemperatureMenu;
import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes
{
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, FirmalifeGreenhousePatch.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ClimateStationTemperatureMenu>> CLIMATE_STATION_TEMPERATURE = RegistrationHelpers.registerContainer(
        MENUS,
        "climate_station_temperature",
        (windowId, playerInventory, buffer) -> ClimateStationTemperatureMenu.create(windowId, playerInventory, buffer.readBlockPos())
    );

    private ModMenuTypes() {}
}
