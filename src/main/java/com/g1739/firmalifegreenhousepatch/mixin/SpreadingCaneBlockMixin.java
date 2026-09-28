package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 藤条（{@link net.dries007.tfc.common.blocks.plant.fruit.SpreadingCaneBlock}）继承自蔓延浆果丛，
 * 但自己又覆写了一份 {@code randomTick}（温度 + 水分不满足就变成"枯萎的藤条"），
 * 因此父类的重定向同样覆盖不到，需要单独补一条。
 */
@Mixin(net.dries007.tfc.common.blocks.plant.fruit.SpreadingCaneBlock.class)
public abstract class SpreadingCaneBlockMixin
{
    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getAverageTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F"
        ),
        require = 0
    )
    private float flgp$useGreenhouseSpreadingCaneTemperature(Level level, BlockPos pos)
    {
        return GreenhouseTemperatureHelper.getControlledTemperature(level, pos, Climate.getAverageTemperature(level, pos));
    }
}
