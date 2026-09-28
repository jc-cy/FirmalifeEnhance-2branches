package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 蔓延形态的浆果丛（{@link net.dries007.tfc.common.blocks.plant.fruit.SpreadingBushBlock}）自己实现了
 * {@code randomTick}，所以打在其父类上的温度重定向覆盖不到它。这里单独补一条，
 * 让温室 / 冷库范围内的蔓延丛按受控温度判定，不会因为室外温度不合适而直接枯死。
 */
@Mixin(net.dries007.tfc.common.blocks.plant.fruit.SpreadingBushBlock.class)
public abstract class SpreadingBushBlockMixin
{
    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getAverageTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F"
        ),
        require = 0
    )
    private float flgp$useGreenhouseSpreadingBushTemperature(Level level, BlockPos pos)
    {
        return GreenhouseTemperatureHelper.getControlledTemperature(level, pos, Climate.getAverageTemperature(level, pos));
    }
}
