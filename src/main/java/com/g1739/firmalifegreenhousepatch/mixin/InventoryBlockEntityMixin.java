package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;

@Mixin(value = InventoryBlockEntity.class, remap = false)
public abstract class InventoryBlockEntityMixin
{
    @Inject(method = "setAndUpdateSlots", at = @At("TAIL"))
    private void flgp$syncCellarTraitsOnSlotChange(int slot, CallbackInfo ci)
    {
        CellarPreservationHelper.syncInventoryBlockEntity((InventoryBlockEntity<?>) (Object) this);
    }

    @Inject(method = "ejectInventory", at = @At("HEAD"))
    private void flgp$sanitizeDroppedCellarItems(CallbackInfo ci)
    {
        CellarPreservationHelper.sanitizeInventoryForDrop((InventoryBlockEntity<?>) (Object) this);
    }

    @Inject(method = "getCapability", at = @At("RETURN"), cancellable = true)
    private <T> void flgp$wrapCellarSidedInventory(Capability<T> cap, @Nullable Direction context, CallbackInfoReturnable<LazyOptional<T>> cir)
    {
        if (cap == Capabilities.ITEM)
        {
            final LazyOptional<IItemHandler> handlers = cir.getReturnValue().cast();
            cir.setReturnValue(handlers
                .lazyMap(handler -> CellarPreservationHelper.wrapSidedInventory((InventoryBlockEntity<?>) (Object) this, handler))
                .cast());
        }
    }
}
