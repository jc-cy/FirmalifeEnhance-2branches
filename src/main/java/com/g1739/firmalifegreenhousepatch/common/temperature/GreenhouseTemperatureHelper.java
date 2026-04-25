package com.g1739.firmalifegreenhousepatch.common.temperature;

import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.g1739.firmalifegreenhousepatch.common.config.PatchConfig;
import java.util.Locale;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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

    public static int getDefaultTemperature()
    {
        return PatchConfig.getDefaultTemperature();
    }

    public static int getMinimumCellarTemperature()
    {
        return PatchConfig.getMinimumCellarTemperature();
    }

    public static int clampTemperature(int temperature)
    {
        return Mth.clamp(temperature, PatchConfig.getMinimumTemperature(), PatchConfig.getMaximumTemperature());
    }

    public static boolean isHeatingItem(ItemStack stack)
    {
        return !stack.isEmpty() && getHeatingUnitFactor(stack) > 0f;
    }

    public static boolean isCoolingItem(ItemStack stack)
    {
        return !stack.isEmpty() && getCoolingUnitFactor(stack) > 0f;
    }

    public static float getHeatingMultiplier(ItemStack stack)
    {
        return stack.isEmpty() ? 0f : stack.getCount() * getHeatingUnitFactor(stack);
    }

    public static float getCoolingMultiplier(ItemStack stack)
    {
        return stack.isEmpty() ? 0f : stack.getCount() * getCoolingUnitFactor(stack);
    }

    public static float getHeatingUnitFactor(ItemStack stack)
    {
        return PatchConfig.getHeatingItemFactor(stack);
    }

    public static float getCoolingUnitFactor(ItemStack stack)
    {
        return PatchConfig.getCoolingItemFactor(stack);
    }

    public static int getAmbientTemperature(Level level, BlockPos stationPos)
    {
        return Math.round(Climate.getAverageTemperature(level, stationPos));
    }

    public static int getBaseControlRange(ClimateStationAccess station)
    {
        if (station.flgp$getClimateType() == ClimateType.CELLAR)
        {
            return PatchConfig.getCellarBaseControlRange();
        }

        final int tier = Math.max(0, station.flgp$getGreenhouseTier());
        if (tier <= 0)
        {
            return 0;
        }

        final GreenhouseStructureData structureData = station.flgp$getGreenhouseStructureData();
        if (structureData != null)
        {
            return tier + Math.max(0, structureData.controlBonus());
        }

        return tier;
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
        final int clamped = Mth.clamp(minTemperature, PatchConfig.getMinimumTemperature(), PatchConfig.getMaximumTemperature());
        return station.flgp$getClimateType() == ClimateType.CELLAR
            ? Math.max(getMinimumCellarTemperature(), clamped)
            : clamped;
    }

    public static int getMaxAllowedTemperature(Level level, BlockPos stationPos, ClimateStationAccess station)
    {
        final int maxTemperature = getAmbientTemperature(level, stationPos) + getHeatingControlRange(station);
        return Mth.clamp(maxTemperature, PatchConfig.getMinimumTemperature(), PatchConfig.getMaximumTemperature());
    }

    public static int getDisplayedMinTemperatureLimit(ClimateStationAccess station)
    {
        return station.flgp$getClimateType() == ClimateType.CELLAR
            ? Math.max(PatchConfig.getMinimumCellarTemperature(), PatchConfig.getMinimumTemperature())
            : PatchConfig.getMinimumTemperature();
    }

    public static int getDisplayedMaxTemperatureLimit(ClimateStationAccess station)
    {
        return PatchConfig.getMaximumTemperature();
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

    public static String formatFactor(float factor)
    {
        if (Float.isInfinite(factor))
        {
            return "\u221e";
        }
        if (Math.abs(factor - Math.round(factor)) < 0.0001f)
        {
            return Integer.toString(Math.round(factor));
        }
        if (Math.abs(factor * 10f - Math.round(factor * 10f)) < 0.0001f)
        {
            return String.format(Locale.ROOT, "%.1f", factor);
        }
        return String.format(Locale.ROOT, "%.2f", factor);
    }
}
