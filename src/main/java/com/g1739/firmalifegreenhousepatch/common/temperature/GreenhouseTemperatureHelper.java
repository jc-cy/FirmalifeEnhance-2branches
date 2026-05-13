package com.g1739.firmalifegreenhousepatch.common.temperature;

import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class GreenhouseTemperatureHelper
{
    public static final int DEFAULT_TEMPERATURE = 20;
    public static final int MIN_TEMPERATURE = -2048;
    public static final int MAX_TEMPERATURE = 2048;
    public static final int CELLAR_MIN_TEMPERATURE = -24;

    public static final int HEATING_SLOT = 0;
    public static final int COOLING_SLOT = 1;

    public static final float MAGMA_BLOCK_FACTOR = 0.1f;
    public static final float ICE_FACTOR = 0.01f;
    public static final float PACKED_ICE_FACTOR = 0.1f;
    public static final float BLUE_ICE_FACTOR = 1.0f;
    public static final int CELLAR_CONTROL_RANGE = 10;

    private GreenhouseTemperatureHelper() {}

    public static int clampTemperature(int temperature)
    {
        return Mth.clamp(temperature, MIN_TEMPERATURE, MAX_TEMPERATURE);
    }

    public static boolean isHeatingItem(ItemStack stack)
    {
        return !stack.isEmpty() && stack.is(Blocks.MAGMA_BLOCK.asItem());
    }

    public static boolean isCoolingItem(ItemStack stack)
    {
        return !stack.isEmpty() && (
            stack.is(Blocks.ICE.asItem()) ||
                stack.is(Blocks.PACKED_ICE.asItem()) ||
                stack.is(Blocks.BLUE_ICE.asItem())
        );
    }

    public static float getHeatingMultiplier(ItemStack stack)
    {
        return isHeatingItem(stack) ? stack.getCount() * MAGMA_BLOCK_FACTOR : 0f;
    }

    public static float getCoolingMultiplier(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return 0f;
        }
        if (stack.is(Blocks.BLUE_ICE.asItem()))
        {
            return stack.getCount() * BLUE_ICE_FACTOR;
        }
        if (stack.is(Blocks.PACKED_ICE.asItem()))
        {
            return stack.getCount() * PACKED_ICE_FACTOR;
        }
        if (stack.is(Blocks.ICE.asItem()))
        {
            return stack.getCount() * ICE_FACTOR;
        }
        return 0f;
    }

    public static float getCoolingUnitFactor(ItemStack stack)
    {
        if (stack.is(Blocks.BLUE_ICE.asItem()))
        {
            return BLUE_ICE_FACTOR;
        }
        if (stack.is(Blocks.PACKED_ICE.asItem()))
        {
            return PACKED_ICE_FACTOR;
        }
        if (stack.is(Blocks.ICE.asItem()))
        {
            return ICE_FACTOR;
        }
        return 0f;
    }

    public static int getAmbientTemperature(Level level, BlockPos stationPos)
    {
        return Math.round(Climate.getAverageTemperature(level, stationPos));
    }

    public static int getBaseControlRange(ClimateStationAccess station)
    {
        if (station.flgp$getClimateType() == ClimateType.CELLAR)
        {
            return CELLAR_CONTROL_RANGE;
        }

        final int tier = Math.max(0, station.flgp$getGreenhouseTier());
        if (tier <= 0)
        {
            return 0;
        }
        return station.flgp$isStainlessGreenhouse() ? tier + 5 : tier;
    }

    public static int getHeatingControlRange(ClimateStationAccess station)
    {
        final int baseRange = getBaseControlRange(station);
        if (baseRange <= 0)
        {
            return 0;
        }
        return Mth.ceil(baseRange * getHeatingMultiplier(station.flgp$getThermalInventory().getStackInSlot(HEATING_SLOT)));
    }

    public static int getCoolingControlRange(ClimateStationAccess station)
    {
        final int baseRange = getBaseControlRange(station);
        if (baseRange <= 0)
        {
            return 0;
        }
        return Mth.ceil(baseRange * getCoolingMultiplier(station.flgp$getThermalInventory().getStackInSlot(COOLING_SLOT)));
    }

    public static int getMinAllowedTemperature(Level level, BlockPos stationPos, ClimateStationAccess station)
    {
        final int minTemperature = getAmbientTemperature(level, stationPos) - getCoolingControlRange(station);
        return station.flgp$getClimateType() == ClimateType.CELLAR
            ? Math.max(CELLAR_MIN_TEMPERATURE, minTemperature)
            : minTemperature;
    }

    public static int getMaxAllowedTemperature(Level level, BlockPos stationPos, ClimateStationAccess station)
    {
        return getAmbientTemperature(level, stationPos) + getHeatingControlRange(station);
    }

    public static int clampRequestedTemperature(Level level, BlockPos stationPos, ClimateStationAccess station, int requestedTemperature)
    {
        return Mth.clamp(
            clampTemperature(requestedTemperature),
            getMinAllowedTemperature(level, stationPos, station),
            getMaxAllowedTemperature(level, stationPos, station)
        );
    }

    public static float getControlledTemperature(Level level, BlockPos pos, float fallbackTemperature)
    {
        return getControlledTemperature(level, pos, ClimateType.GREENHOUSE, fallbackTemperature);
    }

    public static float getControlledTemperature(Level level, BlockPos pos, ClimateType climateType, float fallbackTemperature)
    {
        final ClimateStationAccess station = climateType == ClimateType.CELLAR
            ? ClimateStationRegistry.findControllingCellarStation(level, pos)
            : ClimateStationRegistry.findControllingStation(level, pos);
        return station != null ? station.flgp$getTargetTemperature() : fallbackTemperature;
    }

    public static boolean isControlledGreenhouse(Level level, BlockPos pos)
    {
        return ClimateStationRegistry.findControllingStation(level, pos) != null;
    }
}
