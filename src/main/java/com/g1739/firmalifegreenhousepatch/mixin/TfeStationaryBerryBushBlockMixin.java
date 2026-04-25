package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.compat.TfeClimateCompat;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock;
import net.dries007.tfc.common.blocks.plant.fruit.StationaryBerryBushBlock;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StationaryBerryBushBlock.class, remap = false)
public abstract class TfeStationaryBerryBushBlockMixin
{
    @Shadow protected abstract boolean mayDie(Level level, BlockPos pos, BlockState state, int monthsSpentDying);

    @Shadow protected abstract BlockState getDeadState(BlockState state);

    @Shadow protected abstract BlockState growAndPropagate(Level level, BlockPos pos, net.minecraft.util.RandomSource random, BlockState state);

    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true, require = 0)
    private void flgp$runTfeGreenhouseUpdate(Level level, BlockPos pos, BlockState state, CallbackInfo ci)
    {
        if (!TfeClimateCompat.isEnabled() || !GreenhouseTemperatureHelper.isControlledGreenhouse(level, pos))
        {
            return;
        }

        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity bush)
        {
            Lifecycle currentLifecycle = state.getValue(StationaryBerryBushBlock.LIFECYCLE);
            if (!SeasonalPlantBlock.checkAndSetDormant(level, pos, state, currentLifecycle, Lifecycle.FRUITING))
            {
                long deltaTicks = Math.min(bush.getTicksSinceBushUpdate(), Calendars.SERVER.getCalendarTicksInYear());
                long currentCalendarTick = Calendars.SERVER.getCalendarTicks();
                long nextCalendarTick = currentCalendarTick - deltaTicks;

                final ClimateRange range = ((SeasonalPlantBlockAccessor) this).flgp$getClimateRange().get();
                final int hydration = TfeClimateCompat.getFruitBushHydrationFromRootPos(level, pos.below());

                int monthsSpentDying = 0;
                do
                {
                    nextCalendarTick = Math.min(nextCalendarTick + Calendars.SERVER.getCalendarTicksInMonth(), currentCalendarTick);

                    final float temperatureAtNextTick = GreenhouseTemperatureHelper.getControlledTemperature(
                        level,
                        pos,
                        Climate.getTemperature(level, pos, nextCalendarTick, Calendars.SERVER.getCalendarDaysInMonth())
                    );
                    if (range.checkBoth(hydration, temperatureAtNextTick, false))
                    {
                        currentLifecycle = currentLifecycle.advanceTowards(Lifecycle.FRUITING);
                    }
                    else
                    {
                        currentLifecycle = Lifecycle.DORMANT;
                    }

                    if (currentLifecycle == Lifecycle.DORMANT)
                    {
                        monthsSpentDying++;
                    }
                    else
                    {
                        monthsSpentDying = 0;
                    }
                }
                while (nextCalendarTick < currentCalendarTick);

                final BlockState newState = mayDie(level, pos, state, monthsSpentDying)
                    ? getDeadState(state)
                    : growAndPropagate(level, pos, level.getRandom(), state.setValue(StationaryBerryBushBlock.LIFECYCLE, currentLifecycle));

                if (state != newState)
                {
                    level.setBlock(pos, newState, 3);
                }
            }
        }

        ci.cancel();
    }
}
