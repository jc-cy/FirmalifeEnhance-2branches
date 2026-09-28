package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.dries007.tfc.common.blocks.soil.FarmlandBlock.class)
public abstract class FarmlandBlockMixin
{
    @Redirect(
        method = "getInstantTemperatureTooltip(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/util/climate/ClimateRange;Z)Lnet/minecraft/network/chat/Component;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getInstantTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F"
        ),
        require = 0
    )
    private static float flgp$useGreenhouseTooltipTemperature(Level level, BlockPos pos)
    {
        return flgp$controlledTooltipTemperature(level, pos, Climate.getInstantTemperature(level, pos));
    }

    @Redirect(
        method = "getAverageTemperatureTooltip(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/util/climate/ClimateRange;Z)Lnet/minecraft/network/chat/Component;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getAverageTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F"
        ),
        require = 0
    )
    private static float flgp$useGreenhouseAverageTooltipTemperature(Level level, BlockPos pos)
    {
        return flgp$controlledTooltipTemperature(level, pos, Climate.getAverageTemperature(level, pos));
    }

    /**
     * 温度 tooltip 的位置回退：浆果丛 / 蔓延丛（例如黑莓）自己覆写了 {@code addHoeOverlayInfo}，
     * 传给 tooltip 的是"下方一格"（土壤），而土壤在 floodfill 里算温室地面墙、不在温室内部坐标集合里，
     * 于是温度会退回环境值。这里先按原位置判定，判不到时再用上方一格（植物自身）判一次。
     */
    private static float flgp$controlledTooltipTemperature(Level level, BlockPos pos, float fallbackTemperature)
    {
        if (GreenhouseTemperatureHelper.isControlledGreenhouse(level, pos))
        {
            return GreenhouseTemperatureHelper.getControlledTemperature(level, pos, fallbackTemperature);
        }
        final BlockPos above = pos.above();
        if (GreenhouseTemperatureHelper.isControlledGreenhouse(level, above))
        {
            return GreenhouseTemperatureHelper.getControlledTemperature(level, above, fallbackTemperature);
        }
        return fallbackTemperature;
    }
}
