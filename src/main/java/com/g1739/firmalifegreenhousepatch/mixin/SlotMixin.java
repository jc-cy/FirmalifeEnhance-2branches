package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.dries007.tfc.common.blockentities.TFCChestBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class SlotMixin
{
    @Shadow @Final public Container container;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void flgp$removeCellarTraitsOnTake(Player player, ItemStack stack, CallbackInfo ci)
    {
        CellarPreservationHelper.sanitizeTakenStack(stack);
    }

    @Inject(method = "setChanged", at = @At("TAIL"), require = 0)
    private void flgp$syncChestCellarTraits(CallbackInfo ci)
    {
        if (container instanceof TFCChestBlockEntity chest)
        {
            CellarPreservationHelper.syncTFCChestBlockEntity(chest);
        }
        else
        {
            CellarPreservationHelper.syncExternalContainer(container);
        }
    }
}
