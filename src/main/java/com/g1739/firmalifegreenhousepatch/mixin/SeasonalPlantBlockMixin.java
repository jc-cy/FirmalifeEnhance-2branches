package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock.class)
public abstract class SeasonalPlantBlockMixin
{
    @Inject(method = "getLifecycleForCurrentMonth", at = @At("HEAD"), cancellable = true, require = 0)
    private void flgp$ignoreSeasonInsideGreenhouse(Level level, BlockPos pos, CallbackInfoReturnable<Lifecycle> cir)
    {
        if (GreenhouseTemperatureHelper.isControlledGreenhouse(level, pos))
        {
            cir.setReturnValue(Lifecycle.FRUITING);
        }
    }

    @Redirect(
        method = "onUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getAverageTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F"
        ),
        require = 0
    )
    private float flgp$useGreenhouseAverageTemperature(Level level, BlockPos pos)
    {
        return GreenhouseTemperatureHelper.getControlledTemperature(level, pos, Climate.getAverageTemperature(level, pos));
    }
}
