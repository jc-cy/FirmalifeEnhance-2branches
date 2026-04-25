package com.g1739.firmalifegreenhousepatch.common;

import com.g1739.firmalifegreenhousepatch.FirmalifeGreenhousePatch;
import com.g1739.firmalifegreenhousepatch.common.menu.ClimateStationTemperatureMenu;
import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes
{
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, FirmalifeGreenhousePatch.MOD_ID);

    public static final RegistryObject<MenuType<ClimateStationTemperatureMenu>> CLIMATE_STATION_TEMPERATURE = RegistrationHelpers.registerContainer(
        MENUS,
        "climate_station_temperature",
        (windowId, playerInventory, buffer) -> ClimateStationTemperatureMenu.create(windowId, playerInventory, buffer.readBlockPos())
    );

    private ModMenuTypes() {}
}
