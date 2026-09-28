package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.compat.TfeClimateCompat;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import java.util.List;
import java.util.function.Supplier;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.plant.Plant;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeSaplingBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// priority = 1100：TFE 用 @Overwrite 重写了 randomTick 与 addHoeOverlayInfo（默认 priority = 1000），
// 必须让本 mixin 在它之后应用，`@Inject(HEAD)` 才能挂在 TFE 的方法体前面；
// 否则整个方法体会被 TFE 替换、温室内果树苗的全年生长与温度提示失效。
@Mixin(value = FruitTreeSaplingBlock.class, remap = false, priority = 1100)
public abstract class TfeFruitTreeSaplingBlockMixin
{
    @Shadow @Final private Supplier<ClimateRange> climateRange;
    @Shadow @Final private Lifecycle[] stages;

    @Shadow public abstract void createTree(Level level, BlockPos pos, BlockState state, RandomSource random);

    @Shadow public abstract int getTreeGrowthDays();

    @Inject(method = "addHoeOverlayInfo", at = @At("HEAD"), cancellable = true, require = 0)
    private void flgp$showTfeGreenhouseTooltip(Level level, BlockPos pos, BlockState state, List<Component> text, boolean isDebug, CallbackInfo ci)
    {
        if (!TfeClimateCompat.isEnabled())
        {
            return;
        }

        final BlockPos stemPos = TfeClimateCompat.getFruitTreeSaplingStemPos(level, pos);
        if (!GreenhouseTemperatureHelper.isControlledGreenhouse(level, stemPos))
        {
            return;
        }

        final ClimateRange range = climateRange.get();
        final int hydration = TfeClimateCompat.getFruitBushHydrationFromRootPos(level, stemPos.below());
        final float temperature = GreenhouseTemperatureHelper.getControlledTemperature(level, stemPos, Climate.getAverageTemperature(level, stemPos));

        text.add(FarmlandBlock.getHydrationTooltip(level, stemPos, range, false, hydration));
        text.add(FarmlandBlock.getTemperatureTooltip(level, stemPos, range, temperature, false, "tfc.tooltip.climate_average_temperature"));
        text.add(Component.translatable("tfc.tooltip.fruit_tree.growing"));
        if (FruitTreeSaplingBlock.maySplice(level, pos, state))
        {
            text.add(Component.translatable("tfc.tooltip.fruit_tree.sapling_splice"));
        }

        ci.cancel();
    }

    @Inject(method = {"randomTick", "m_213898_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void flgp$runTfeGreenhouseGrowth(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci)
    {
        if (!TfeClimateCompat.isEnabled())
        {
            return;
        }

        final BlockPos stemPos = TfeClimateCompat.getFruitTreeSaplingStemPos(level, pos);
        if (!GreenhouseTemperatureHelper.isControlledGreenhouse(level, stemPos))
        {
            return;
        }

        if (level.getBlockEntity(pos) instanceof TickCounterBlockEntity counter)
        {
            final long ticksToGrow = (long) (ICalendar.TICKS_IN_DAY * getTreeGrowthDays() * TFCConfig.SERVER.globalFruitSaplingGrowthModifier.get());
            final long elapsedTicks = counter.getTicksSinceUpdate();
            if (elapsedTicks > ticksToGrow)
            {
                final int hydration = TfeClimateCompat.getFruitBushHydrationFromRootPos(level, stemPos.below());
                final float temperature = GreenhouseTemperatureHelper.getControlledTemperature(level, stemPos, Climate.getAverageTemperature(level, stemPos));
                if (!climateRange.get().checkBoth(hydration, temperature, false))
                {
                    level.setBlockAndUpdate(pos, TFCBlocks.PLANTS.get(Plant.DEAD_BUSH).get().defaultBlockState());
                }
                else
                {
                    createTree(level, pos, state, random);
                    final long carriedTicks = elapsedTicks - ticksToGrow;
                    if (carriedTicks > 0L && level.getBlockEntity(pos) instanceof TickCounterBlockEntity grownCounter)
                    {
                        grownCounter.reduceCounter(carriedTicks);
                    }
                }
            }
            ci.cancel();
        }
    }
}
