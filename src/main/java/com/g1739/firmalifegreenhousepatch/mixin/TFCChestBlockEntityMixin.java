package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.dries007.tfc.common.blockentities.TFCChestBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TFCChestBlockEntity.class, remap = false)
public abstract class TFCChestBlockEntityMixin
{
    @Inject(method = {"createMenu", "m_6555_"}, at = @At("HEAD"), require = 0)
    private void flgp$syncCellarTraitsOnOpen(int id, Inventory inventory, CallbackInfoReturnable<AbstractContainerMenu> cir)
    {
        CellarPreservationHelper.syncChestBlockEntity((TFCChestBlockEntity) (Object) this);
    }

    public void setAndUpdateSlots(int slot)
    {
        CellarPreservationHelper.syncChestBlockEntity((TFCChestBlockEntity) (Object) this);
    }
}
