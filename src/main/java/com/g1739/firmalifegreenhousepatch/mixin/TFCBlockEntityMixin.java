package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import com.g1739.firmalifegreenhousepatch.common.temperature.ClimateStationAccess;
import com.g1739.firmalifegreenhousepatch.common.temperature.ClimateStationRegistry;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = net.dries007.tfc.common.blockentities.TFCBlockEntity.class, remap = false)
public abstract class TFCBlockEntityMixin
{
    @Inject(method = "onLoadAdditional", at = @At("TAIL"))
    private void flgp$registerClimateStation(CallbackInfo ci)
    {
        if ((Object) this instanceof InventoryBlockEntity<?> inventory)
        {
            CellarPreservationHelper.syncInventoryBlockEntity(inventory);
        }
        if ((Object) this instanceof ClimateStationAccess station && (Object) this instanceof BlockEntity blockEntity)
        {
            ClimateStationRegistry.register(blockEntity, station);
            CellarPreservationHelper.syncTrackedInventories(station);
        }
    }

    @Inject(method = "onUnloadAdditional", at = @At("TAIL"))
    private void flgp$unregisterClimateStation(CallbackInfo ci)
    {
        if ((Object) this instanceof ClimateStationAccess station && (Object) this instanceof BlockEntity blockEntity)
        {
            ClimateStationRegistry.unregister(blockEntity, station);
        }
    }
}
