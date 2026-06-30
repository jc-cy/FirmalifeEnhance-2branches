package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin
{
    @Shadow @Final public NonNullList<Slot> slots;

    @Inject(method = "setCarried", at = @At("HEAD"))
    private void flgp$sanitizeCarriedStack(ItemStack stack, CallbackInfo ci)
    {
        CellarPreservationHelper.sanitizeTakenStack(stack);
    }

    @Inject(method = "moveItemStackTo", at = @At("RETURN"), require = 0)
    private void flgp$sanitizeMovingStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection, CallbackInfoReturnable<Boolean> cir)
    {
        if (cir.getReturnValueZ())
        {
            flgp$sanitizePlayerInventorySlots(startIndex, endIndex);
        }
    }

    private void flgp$sanitizePlayerInventorySlots(int startIndex, int endIndex)
    {
        final int start = Math.max(0, Math.min(startIndex, slots.size()));
        final int end = Math.max(start, Math.min(endIndex, slots.size()));
        for (int index = start; index < end; index++)
        {
            final Slot slot = slots.get(index);
            if (slot.container instanceof Inventory)
            {
                CellarPreservationHelper.sanitizeTakenStack(slot.getItem());
                slot.setChanged();
            }
        }
    }
}
