package com.g1739.firmalifegreenhousepatch.mixin;

import com.eerussianguy.firmalife.common.blockentities.HangerBlockEntity;
import com.g1739.firmalifegreenhousepatch.common.ModFoodTraits;
import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import java.util.Set;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.dries007.tfc.common.component.food.FoodTrait;

@Mixin(HangerBlockEntity.class)
public abstract class HangerBlockEntityMixin
{
    @Inject(method = "getFoodTrait", at = @At("HEAD"), cancellable = true)
    private void flgp$usePatchCellarTrait(CallbackInfoReturnable<Holder<FoodTrait>> cir)
    {
        final HangerBlockEntity hanger = (HangerBlockEntity) (Object) this;
        final var level = hanger.getLevel();
        cir.setReturnValue(level != null ? CellarPreservationHelper.getCellarTrait(level, hanger.getBlockPos()) : ModFoodTraits.CELLAR_2_5X);
    }

    @Inject(method = "getPossibleTraits", at = @At("HEAD"), cancellable = true)
    private void flgp$useAllCellarTraits(CallbackInfoReturnable<Set<DeferredHolder<FoodTrait, FoodTrait>>> cir)
    {
        cir.setReturnValue(CellarPreservationHelper.getPossibleTraits());
    }
}
