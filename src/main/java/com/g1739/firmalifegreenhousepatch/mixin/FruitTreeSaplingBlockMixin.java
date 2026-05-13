package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

@Mixin(net.dries007.tfc.common.blocks.plant.fruit.FruitTreeSaplingBlock.class)
public abstract class FruitTreeSaplingBlockMixin
{
    @Redirect(
        method = "randomTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/plant/fruit/Lifecycle;active()Z"
        ),
        require = 0
    )
    private boolean flgp$allowSaplingGrowthOutsideSeason(Lifecycle lifecycle, BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        return GreenhouseTemperatureHelper.isControlledGreenhouse(level, pos) || lifecycle.active();
    }

    @Redirect(
        method = "addHoeOverlayInfo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/plant/fruit/Lifecycle;active()Z"
        ),
        require = 0
    )
    private boolean flgp$hideWrongMonthTooltipInsideGreenhouse(Lifecycle lifecycle, Level level, BlockPos pos, BlockState state, Consumer<Component> text, boolean isDebug)
    {
        return GreenhouseTemperatureHelper.isControlledGreenhouse(level, pos) || lifecycle.active();
    }

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
}
