package com.g1739.firmalifegreenhousepatch.common.temperature;

import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public interface ClimateStationAccess
{
    int flgp$getTargetTemperature();

    int flgp$getRequestedTemperature();

    void flgp$setTargetTemperature(int temperature);

    Set<BlockPos> flgp$getGreenhousePositions();

    ClimateType flgp$getClimateType();

    IItemHandlerModifiable flgp$getThermalInventory();

    int flgp$getGreenhouseTier();

    boolean flgp$isStainlessGreenhouse();
}
