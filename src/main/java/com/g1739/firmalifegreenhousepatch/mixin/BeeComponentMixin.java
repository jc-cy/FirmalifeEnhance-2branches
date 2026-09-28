package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 生成野生蜜蜂（{@link com.eerussianguy.firmalife.common.capabilities.bee.BeeComponent#getWildBee}）时，
 * Firmalife 3.0.14 用环境平均温度挑选蜂种。这里把它接到补丁的受控温度，
 * 使温室 / 冷库范围内生成的野蜂按受控温度选种。与二代 `群峦现代化生活` 的同名 mixin 逐字对应。
 */
@Mixin(com.eerussianguy.firmalife.common.capabilities.bee.BeeComponent.class)
public abstract class BeeComponentMixin
{
    @Redirect(
        method = "getWildBee",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getAverageTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F"
        ),
        require = 0
    )
    private static float flgp$useControlledAverageTemperature(Level level, BlockPos pos)
    {
        return GreenhouseTemperatureHelper.getControlledTemperature(level, pos, Climate.getAverageTemperature(level, pos));
    }
}
