package com.g1739.firmalifegreenhousepatch.mixin;

import com.eerussianguy.firmalife.common.blockentities.HangerBlockEntity;
import com.g1739.firmalifegreenhousepatch.common.ModFoodTraits;
import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HangerBlockEntity.class, remap = false)
public abstract class HangerBlockEntityMixin
{
    @Inject(method = "getFoodTrait", at = @At("HEAD"), cancellable = true)
    private void flgp$usePatchCellarTrait(CallbackInfoReturnable<FoodTrait> cir)
    {
        final HangerBlockEntity hanger = (HangerBlockEntity) (Object) this;
        final var level = hanger.getLevel();
        cir.setReturnValue(level != null ? CellarPreservationHelper.getCellarTrait(level, hanger.getBlockPos()) : ModFoodTraits.getDefaultCellarTrait());
    }

    @Inject(method = "getPossibleTraits", at = @At("HEAD"), cancellable = true)
    private void flgp$useAllCellarTraits(CallbackInfoReturnable<FoodTrait[]> cir)
    {
        cir.setReturnValue(CellarPreservationHelper.getPossibleTraits());
    }
}
