package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.dries007.tfc.common.blockentities.TFCChestBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class SlotMixin
{
    @Inject(method = "onTake", at = @At("HEAD"))
    private void flgp$removeCellarTraitsOnTake(Player player, ItemStack stack, CallbackInfo ci)
    {
        CellarPreservationHelper.sanitizeTakenStack(stack);
    }

    @Redirect(
        method = "setChanged",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setChanged()V"),
        require = 0
    )
    private void flgp$syncChestCellarTraits(Container container)
    {
        container.setChanged();
        if (container instanceof TFCChestBlockEntity chest)
        {
            CellarPreservationHelper.syncTFCChestBlockEntity(chest);
        }
    }
}
