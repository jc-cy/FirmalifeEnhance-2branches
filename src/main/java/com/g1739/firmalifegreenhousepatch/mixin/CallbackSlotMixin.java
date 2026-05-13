package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.container.ISlotCallback;
import net.dries007.tfc.common.container.slot.CallbackSlot;

@Mixin(CallbackSlot.class)
public abstract class CallbackSlotMixin
{
    @Shadow @Final private ISlotCallback callback;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void flgp$removeCellarTraitsOnTake(Player player, ItemStack stack, CallbackInfo ci)
    {
        if (callback instanceof InventoryBlockEntity<?> inventory)
        {
            CellarPreservationHelper.sanitizeTakenStack(inventory, stack);
        }
    }
}
