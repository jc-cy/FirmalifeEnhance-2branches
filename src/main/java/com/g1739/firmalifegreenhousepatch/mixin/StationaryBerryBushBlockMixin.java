package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.plant.fruit.StationaryBerryBushBlock;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.calendar.Month;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = net.dries007.tfc.common.blocks.plant.fruit.StationaryBerryBushBlock.class, remap = false)
public abstract class StationaryBerryBushBlockMixin
{
    @Shadow protected abstract boolean mayDie(Level level, BlockPos pos, BlockState state, int monthsSpentDying);

    @Shadow protected abstract BlockState getDeadState(BlockState state);

    @Shadow protected abstract BlockState growAndPropagate(Level level, BlockPos pos, net.minecraft.util.RandomSource random, BlockState state);

    @Redirect(
        method = "onUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;JI)F"
        ),
        require = 0
    )
    private float flgp$useGreenhouseTrackedTemperature(Level level, BlockPos pos, long tick, int daysInMonth)
    {
        return GreenhouseTemperatureHelper.getControlledTemperature(level, pos, Climate.getTemperature(level, pos, tick, daysInMonth));
    }

    @Redirect(
        method = "onUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/plant/fruit/StationaryBerryBushBlock;getLifecycleForCurrentMonth()Lnet/dries007/tfc/common/blocks/plant/fruit/Lifecycle;"
        ),
        require = 0
    )
    private Lifecycle flgp$useGreenhouseCurrentLifecycle(net.dries007.tfc.common.blocks.plant.fruit.StationaryBerryBushBlock bush, Level level, BlockPos pos, BlockState state)
    {
        return GreenhouseTemperatureHelper.isControlledGreenhouse(level, pos)
            ? Lifecycle.FRUITING
            : ((SeasonalPlantBlockAccessor) bush).flgp$invokeGetLifecycleForCurrentMonth();
    }

    @Redirect(
        method = "onUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/plant/fruit/StationaryBerryBushBlock;getLifecycleForMonth(Lnet/dries007/tfc/util/calendar/Month;)Lnet/dries007/tfc/common/blocks/plant/fruit/Lifecycle;"
        ),
        require = 0
    )
    private Lifecycle flgp$ignoreDormantMonthsInsideGreenhouse(net.dries007.tfc.common.blocks.plant.fruit.StationaryBerryBushBlock bush, Month month, Level level, BlockPos pos, BlockState state)
    {
        return GreenhouseTemperatureHelper.isControlledGreenhouse(level, pos)
            ? Lifecycle.FRUITING
            : ((SeasonalPlantBlockAccessor) bush).flgp$invokeGetLifecycleForMonth(month);
    }

    @Redirect(
        method = "addHoeOverlayInfo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/soil/FarmlandBlock;getTemperatureTooltip(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/util/climate/ClimateRange;Z)Lnet/minecraft/network/chat/Component;"
        ),
        require = 0
    )
    private net.minecraft.network.chat.Component flgp$useStemTemperatureTooltip(Level level, BlockPos sourcePos, net.dries007.tfc.util.climate.ClimateRange validRange, boolean allowWiggle)
    {
        return net.dries007.tfc.common.blocks.soil.FarmlandBlock.getTemperatureTooltip(level, sourcePos.above(), validRange, allowWiggle);
    }
}
