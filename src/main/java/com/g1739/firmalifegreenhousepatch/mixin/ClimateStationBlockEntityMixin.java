package com.g1739.firmalifegreenhousepatch.mixin;

import com.eerussianguy.firmalife.common.blockentities.ClimateStationBlockEntity;
import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.util.GreenhouseType;
import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import com.g1739.firmalifegreenhousepatch.common.temperature.ClimateStationAccess;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClimateStationBlockEntity.class)
public abstract class ClimateStationBlockEntityMixin implements ClimateStationAccess
{
    @Shadow
    private Set<BlockPos> positions;

    @Shadow
    private ClimateType type;

    @Shadow
    @Nullable
    private GreenhouseType favoriteGreenhouseType;

    @Unique private static final String FLGP_TARGET_TEMPERATURE_KEY = "GreenhouseTargetTemperature";
    @Unique private static final String FLGP_THERMAL_INVENTORY_KEY = "GreenhouseThermalInventory";
    @Unique private static final String FLGP_GREENHOUSE_TIER_KEY = "GreenhouseControlTier";

    @Unique private int flgp$targetTemperature = GreenhouseTemperatureHelper.DEFAULT_TEMPERATURE;
    @Unique private int flgp$greenhouseTier = 0;
    @Unique private boolean flgp$lastRequestedValidity = false;
    @Unique private boolean flgp$loadingInventory = false;
    @Unique private Set<BlockPos> flgp$previousPositions = Set.of();
    @Unique private ClimateType flgp$previousType = ClimateType.GREENHOUSE;
    @Unique private final ItemStackHandler flgp$thermalInventory = new ItemStackHandler(2)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            if (flgp$loadingInventory)
            {
                return;
            }
            flgp$normalizeTargetTemperature();
            ((ClimateStationBlockEntity) (Object) ClimateStationBlockEntityMixin.this).markForSync();
        }
    };

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void flgp$loadTargetTemperature(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci)
    {
        flgp$targetTemperature = nbt.contains(FLGP_TARGET_TEMPERATURE_KEY)
            ? GreenhouseTemperatureHelper.clampTemperature(nbt.getInt(FLGP_TARGET_TEMPERATURE_KEY))
            : GreenhouseTemperatureHelper.DEFAULT_TEMPERATURE;
        flgp$greenhouseTier = Math.max(0, nbt.getInt(FLGP_GREENHOUSE_TIER_KEY));
        flgp$loadingInventory = true;
        if (nbt.contains(FLGP_THERMAL_INVENTORY_KEY))
        {
            flgp$thermalInventory.deserializeNBT(provider, nbt.getCompound(FLGP_THERMAL_INVENTORY_KEY));
        }
        flgp$loadingInventory = false;
        flgp$normalizeTargetTemperature();
        flgp$previousPositions = positions;
        flgp$previousType = type;
    }

    @Inject(method = "setPositions", at = @At("HEAD"))
    private void flgp$capturePreviousPositions(Set<BlockPos> newPositions, CallbackInfo ci)
    {
        flgp$previousPositions = positions;
        flgp$previousType = type;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void flgp$saveTargetTemperature(CompoundTag nbt, HolderLookup.Provider provider, CallbackInfo ci)
    {
        nbt.putInt(FLGP_TARGET_TEMPERATURE_KEY, flgp$targetTemperature);
        nbt.putInt(FLGP_GREENHOUSE_TIER_KEY, flgp$greenhouseTier);
        nbt.put(FLGP_THERMAL_INVENTORY_KEY, flgp$thermalInventory.serializeNBT(provider));
    }

    @Inject(method = "updateValidity", at = @At("HEAD"))
    private void flgp$cacheGreenhouseTier(boolean valid, int tier, CallbackInfo ci)
    {
        flgp$lastRequestedValidity = valid;
        flgp$greenhouseTier = valid ? Math.max(0, tier) : 0;
        flgp$normalizeTargetTemperature();
    }

    @Inject(method = "updateValidity", at = @At("TAIL"))
    private void flgp$syncCellarInventories(boolean valid, int tier, CallbackInfo ci)
    {
        final ClimateStationBlockEntity self = (ClimateStationBlockEntity) (Object) this;
        final Level level = self.getLevel();
        if (level != null && !level.isClientSide())
        {
            final Set<BlockPos> removedPositions = new HashSet<>(flgp$previousPositions);
            removedPositions.removeAll(positions);

            for (BlockPos removedPos : removedPositions)
            {
                final ClimateReceiver receiver = ClimateReceiver.get(level, removedPos);
                if (receiver != null)
                {
                    receiver.setValid(level, removedPos, false, 0, flgp$previousType);
                }
            }

            if (flgp$previousType == ClimateType.CELLAR && !removedPositions.isEmpty())
            {
                CellarPreservationHelper.syncBlockEntities(level, removedPositions);
            }
        }

        CellarPreservationHelper.syncTrackedInventories(this, valid);
        flgp$previousPositions = positions;
        flgp$previousType = type;
    }

    @Inject(method = "setType", at = @At("TAIL"))
    private void flgp$syncCellarInventoriesOnTypeChange(ClimateType climateType, CallbackInfo ci)
    {
        final ClimateStationBlockEntity self = (ClimateStationBlockEntity) (Object) this;
        final Level level = self.getLevel();
        if (level == null || level.isClientSide())
        {
            return;
        }

        if (climateType != flgp$previousType)
        {
            final int tier = climateType == ClimateType.CELLAR ? 0 : flgp$greenhouseTier;
            for (BlockPos pos : positions)
            {
                final ClimateReceiver receiver = ClimateReceiver.get(level, pos);
                if (receiver != null)
                {
                    receiver.setValid(level, pos, false, 0, flgp$previousType);
                    receiver.setValid(level, pos, flgp$lastRequestedValidity, tier, climateType);
                }
            }

            if (flgp$previousType == ClimateType.CELLAR)
            {
                CellarPreservationHelper.syncBlockEntities(level, positions);
            }
        }

        if (climateType == ClimateType.CELLAR)
        {
            CellarPreservationHelper.syncTrackedInventories(this, flgp$lastRequestedValidity);
        }

        flgp$previousPositions = positions;
        flgp$previousType = climateType;
    }

    @Override
    public int flgp$getTargetTemperature()
    {
        final ClimateStationBlockEntity self = (ClimateStationBlockEntity) (Object) this;
        final Level level = self.getLevel();
        return level != null
            ? GreenhouseTemperatureHelper.clampRequestedTemperature(level, self.getBlockPos(), this, flgp$targetTemperature)
            : GreenhouseTemperatureHelper.clampTemperature(flgp$targetTemperature);
    }

    @Override
    public int flgp$getRequestedTemperature()
    {
        return flgp$targetTemperature;
    }

    @Override
    public void flgp$setTargetTemperature(int temperature)
    {
        final ClimateStationBlockEntity self = (ClimateStationBlockEntity) (Object) this;
        final Level level = self.getLevel();
        final int clampedTemperature = level != null
            ? GreenhouseTemperatureHelper.clampRequestedTemperature(level, self.getBlockPos(), this, temperature)
            : GreenhouseTemperatureHelper.clampTemperature(temperature);
        if (clampedTemperature != flgp$targetTemperature)
        {
            flgp$targetTemperature = clampedTemperature;
            self.markForSync();
        }
    }

    @Override
    public Set<BlockPos> flgp$getGreenhousePositions()
    {
        return positions;
    }

    @Override
    public ClimateType flgp$getClimateType()
    {
        return type;
    }

    @Override
    public IItemHandlerModifiable flgp$getThermalInventory()
    {
        return flgp$thermalInventory;
    }

    @Override
    public int flgp$getGreenhouseTier()
    {
        return flgp$greenhouseTier;
    }

    @Override
    public boolean flgp$isStainlessGreenhouse()
    {
        final ResourceLocation id = favoriteGreenhouseType == null ? null : GreenhouseType.MANAGER.getId(favoriteGreenhouseType);
        return id != null && id.getPath().contains("stainless_steel");
    }

    @Unique
    private void flgp$normalizeTargetTemperature()
    {
        final ClimateStationBlockEntity self = (ClimateStationBlockEntity) (Object) this;
        final Level level = self.getLevel();
        flgp$targetTemperature = level != null
            ? GreenhouseTemperatureHelper.clampRequestedTemperature(level, self.getBlockPos(), this, flgp$targetTemperature)
            : GreenhouseTemperatureHelper.clampTemperature(flgp$targetTemperature);
    }
}
