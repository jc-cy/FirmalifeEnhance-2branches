package com.g1739.firmalifegreenhousepatch.mixin;

import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.calendar.Month;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.dries007.tfc.common.blocks.plant.fruit.StationaryBerryBushBlock.class)
public abstract class StationaryBerryBushBlockMixin
{
    @Redirect(
        method = "randomTick",
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

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/plant/fruit/StationaryBerryBushBlock;getLifecycleForMonth(Lnet/dries007/tfc/util/calendar/Month;)Lnet/dries007/tfc/common/blocks/plant/fruit/Lifecycle;"
        ),
        require = 0
    )
    private Lifecycle flgp$ignoreDormantMonthsInsideGreenhouse(net.dries007.tfc.common.blocks.plant.fruit.StationaryBerryBushBlock bush, Month month, net.minecraft.world.level.block.state.BlockState state, ServerLevel level, BlockPos pos, RandomSource rand)
    {
        return GreenhouseTemperatureHelper.isControlledGreenhouse(level, pos)
            ? Lifecycle.HEALTHY
            : ((SeasonalPlantBlockAccessor) bush).flgp$invokeGetLifecycleForMonth(month);
    }

    @Redirect(
        method = "addHoeOverlayInfo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/soil/FarmlandBlock;getAverageTemperatureTooltip(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/util/climate/ClimateRange;Z)Lnet/minecraft/network/chat/Component;"
        ),
        require = 0
    )
    private net.minecraft.network.chat.Component flgp$useStemTemperatureTooltip(Level level, BlockPos sourcePos, net.dries007.tfc.util.climate.ClimateRange validRange, boolean allowWiggle)
    {
        return net.dries007.tfc.common.blocks.soil.FarmlandBlock.getAverageTemperatureTooltip(level, sourcePos.above(), validRange, allowWiggle);
    }
}
