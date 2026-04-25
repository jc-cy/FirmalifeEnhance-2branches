package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.item.ItemStack;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.container.Container;
import net.dries007.tfc.common.container.ISlotCallback;

@Mixin(value = Container.class, remap = false)
public abstract class ContainerMixin
{
    @Shadow protected ISlotCallback callback;

    @Inject(method = "setCarried", at = @At("HEAD"))
    private void flgp$removeCellarTraitsFromCarried(ItemStack stack, CallbackInfo ci)
    {
        if (callback instanceof InventoryBlockEntity<?> inventory)
        {
            CellarPreservationHelper.sanitizeTakenStack(inventory, stack);
        }
    }
}
