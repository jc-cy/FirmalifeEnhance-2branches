package com.g1739.firmalifegreenhousepatch.mixin;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateStationBlockEntity;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import com.g1739.firmalifegreenhousepatch.common.temperature.ClimateStationAccess;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseStructureData;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClimateStationBlockEntity.class, remap = false)
public abstract class ClimateStationBlockEntityMixin implements ClimateStationAccess
{
    @Shadow private Set<BlockPos> positions;
    @Shadow private ClimateType type;
    @Shadow @Nullable private ResourceLocation favoriteGreenhouseType;
    @Shadow private boolean favoriteIsCellar;

    @Unique private static final String FLGP_TARGET_TEMPERATURE_KEY = "GreenhouseTargetTemperature";
    @Unique private static final String FLGP_THERMAL_INVENTORY_KEY = "GreenhouseThermalInventory";
    @Unique private static final String FLGP_GREENHOUSE_TIER_KEY = "GreenhouseControlTier";
    @Unique private static final String FLGP_GREENHOUSE_STRUCTURE_KEY = "GreenhouseStructureData";

    @Unique private int flgp$targetTemperature = GreenhouseTemperatureHelper.getDefaultTemperature();
    @Unique private int flgp$greenhouseTier = 0;
    @Unique private boolean flgp$lastRequestedValidity = false;
    @Unique private boolean flgp$loadingInventory = false;
    @Unique private Set<BlockPos> flgp$previousPositions = Set.of();
    @Unique private ClimateType flgp$previousType = ClimateType.GREENHOUSE;
    @Unique @Nullable private GreenhouseStructureData flgp$greenhouseStructureData = null;
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
            final ClimateStationBlockEntity self = (ClimateStationBlockEntity) (Object) ClimateStationBlockEntityMixin.this;
            final Level level = self.getLevel();
            if (level != null && !level.isClientSide())
            {
                CellarPreservationHelper.syncTrackedInventories(ClimateStationBlockEntityMixin.this);
            }
            self.markForSync();
        }
    };

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void flgp$loadTargetTemperature(CompoundTag nbt, CallbackInfo ci)
    {
        flgp$targetTemperature = nbt.contains(FLGP_TARGET_TEMPERATURE_KEY)
            ? GreenhouseTemperatureHelper.clampTemperature(nbt.getInt(FLGP_TARGET_TEMPERATURE_KEY))
            : GreenhouseTemperatureHelper.getDefaultTemperature();
        flgp$greenhouseTier = Math.max(0, nbt.getInt(FLGP_GREENHOUSE_TIER_KEY));
        flgp$loadingInventory = true;
        if (nbt.contains(FLGP_THERMAL_INVENTORY_KEY))
        {
            flgp$thermalInventory.deserializeNBT(nbt.getCompound(FLGP_THERMAL_INVENTORY_KEY));
        }
        flgp$loadingInventory = false;
        flgp$greenhouseStructureData = nbt.contains(FLGP_GREENHOUSE_STRUCTURE_KEY)
            ? GreenhouseStructureData.fromTag(nbt.getCompound(FLGP_GREENHOUSE_STRUCTURE_KEY))
            : null;
        flgp$normalizeTargetTemperature();
        flgp$previousPositions = positions;
        flgp$previousType = type;
    }

    @Inject(method = "setFavorite", at = @At("HEAD"), cancellable = true)
    private void flgp$acceptConfiguredCellarWall(ItemStack held, CallbackInfoReturnable<Boolean> cir)
    {
        if (!(held.getItem() instanceof BlockItem blockItem))
        {
            return;
        }
        if (!com.g1739.firmalifegreenhousepatch.common.config.PatchConfig.isConfiguredCellarWall(blockItem.getBlock().defaultBlockState()))
        {
            return;
        }

        favoriteGreenhouseType = null;
        favoriteIsCellar = true;
        flgp$markForSyncIfServer();
        cir.setReturnValue(true);
    }

    @Inject(method = "setPositions", at = @At("HEAD"))
    private void flgp$capturePreviousPositions(Set<BlockPos> newPositions, CallbackInfo ci)
    {
        flgp$previousPositions = positions;
        flgp$previousType = type;
    }

    @Inject(method = {"saveAdditional", "m_183515_"}, at = @At("TAIL"), require = 0)
    private void flgp$saveTargetTemperature(CompoundTag nbt, CallbackInfo ci)
    {
        nbt.putInt(FLGP_TARGET_TEMPERATURE_KEY, flgp$targetTemperature);
        nbt.putInt(FLGP_GREENHOUSE_TIER_KEY, flgp$greenhouseTier);
        nbt.put(FLGP_THERMAL_INVENTORY_KEY, flgp$thermalInventory.serializeNBT());
        if (flgp$greenhouseStructureData != null)
        {
            nbt.put(FLGP_GREENHOUSE_STRUCTURE_KEY, flgp$greenhouseStructureData.toTag());
        }
    }

    @Inject(method = "updateValidity", at = @At("HEAD"))
    private void flgp$cacheGreenhouseTier(boolean valid, int tier, CallbackInfo ci)
    {
        flgp$lastRequestedValidity = valid;
        flgp$greenhouseTier = valid ? Math.max(0, tier) : 0;
        if (!valid)
        {
            flgp$greenhouseStructureData = null;
        }
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
        if (climateType != ClimateType.GREENHOUSE)
        {
            flgp$greenhouseStructureData = null;
        }
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
            if (level != null && !level.isClientSide())
            {
                CellarPreservationHelper.syncTrackedInventories(this);
            }
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
        return favoriteGreenhouseType != null && favoriteGreenhouseType.getPath().contains("stainless_steel");
    }

    @Override
    @Nullable
    public GreenhouseStructureData flgp$getGreenhouseStructureData()
    {
        return flgp$greenhouseStructureData;
    }

    @Override
    public void flgp$setGreenhouseStructureData(@Nullable GreenhouseStructureData data)
    {
        if ((flgp$greenhouseStructureData == null && data == null) || (flgp$greenhouseStructureData != null && flgp$greenhouseStructureData.equals(data)))
        {
            return;
        }
        flgp$greenhouseStructureData = data;
        flgp$markForSyncIfServer();
    }

    @Override
    public boolean flgp$hasFavoriteGreenhouseType()
    {
        return favoriteGreenhouseType != null;
    }

    @Override
    public void flgp$clearFavoriteClimateHints()
    {
        if (favoriteGreenhouseType == null && !favoriteIsCellar)
        {
            return;
        }
        favoriteGreenhouseType = null;
        favoriteIsCellar = false;
        flgp$markForSyncIfServer();
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

    @Unique
    private void flgp$markForSyncIfServer()
    {
        final ClimateStationBlockEntity self = (ClimateStationBlockEntity) (Object) this;
        final Level level = self.getLevel();
        if (level != null && !level.isClientSide())
        {
            self.markForSync();
        }
    }
}
