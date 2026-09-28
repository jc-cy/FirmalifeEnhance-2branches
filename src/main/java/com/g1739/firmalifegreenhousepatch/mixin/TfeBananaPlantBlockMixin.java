package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.compat.TfeClimateCompat;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.plant.fruit.BananaPlantBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// priority = 1100：TFE 用 @Overwrite 重写了 onUpdate（默认 priority = 1000），必须让本 mixin
// 在它之后应用，`@Inject(HEAD)` 才能挂在 TFE 的方法体前面；否则整个方法体会被 TFE 替换、反季节失效。
@Mixin(value = BananaPlantBlock.class, remap = false, priority = 1100)
public abstract class TfeBananaPlantBlockMixin
{
    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true, require = 0)
    private void flgp$runTfeGreenhouseUpdate(Level level, BlockPos pos, BlockState state, CallbackInfo ci)
    {
        if (!TfeClimateCompat.isEnabled())
        {
            return;
        }

        final BlockPos rootPos = TfeClimateCompat.getBananaRootPos(level, pos);
        if (!GreenhouseTemperatureHelper.isControlledGreenhouse(level, rootPos))
        {
            return;
        }

        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity bush)
        {
            Lifecycle currentLifecycle = state.getValue(BananaPlantBlock.LIFECYCLE);
            if (!SeasonalPlantBlock.checkAndSetDormant(level, pos, state, currentLifecycle, Lifecycle.FRUITING))
            {
                long deltaTicks = Math.min(bush.getTicksSinceBushUpdate(), Calendars.SERVER.getCalendarTicksInYear());
                long currentCalendarTick = Calendars.SERVER.getCalendarTicks();
                long nextCalendarTick = currentCalendarTick - deltaTicks;

                final ClimateRange range = ((SeasonalPlantBlockAccessor) this).flgp$getClimateRange().get();
                final int hydration = TfeClimateCompat.getFruitBushHydrationFromRootPos(level, rootPos);

                int stage = state.getValue(BananaPlantBlock.STAGE);
                final BlockPos abovePos = pos.above();
                BlockState newState = state;
                do
                {
                    nextCalendarTick = Math.min(nextCalendarTick + Calendars.SERVER.getCalendarTicksInMonth(), currentCalendarTick);
                    if (currentLifecycle.active() && stage < 2)
                    {
                        final BlockPos downPos = pos.below(3);
                        if (!Helpers.isBlock(level.getBlockState(abovePos), (BananaPlantBlock) (Object) this) && (level.random.nextInt(4) == 0 || Helpers.isBlock(level.getBlockState(downPos), (BananaPlantBlock) (Object) this)))
                        {
                            stage++;
                        }
                    }

                    final float temperatureAtNextTick = GreenhouseTemperatureHelper.getControlledTemperature(
                        level,
                        rootPos,
                        Climate.getTemperature(level, rootPos, nextCalendarTick, Calendars.SERVER.getCalendarDaysInMonth())
                    );
                    if (range.checkBoth(hydration, temperatureAtNextTick, false))
                    {
                        currentLifecycle = currentLifecycle.advanceTowards(Lifecycle.FRUITING);
                    }
                    else
                    {
                        currentLifecycle = Lifecycle.DORMANT;
                    }
                    if (stage < 2 && currentLifecycle.active())
                    {
                        currentLifecycle = Lifecycle.HEALTHY;
                    }

                    newState = state.setValue(BananaPlantBlock.STAGE, stage).setValue(BananaPlantBlock.LIFECYCLE, currentLifecycle);
                    if (stage < 2 && currentLifecycle.active() && level.isEmptyBlock(abovePos) && level.canSeeSky(abovePos))
                    {
                        final long propagatedTick = nextCalendarTick;
                        level.setBlockAndUpdate(abovePos, newState);
                        level.getBlockEntity(abovePos, TFCBlockEntities.BERRY_BUSH.get()).ifPresent(newBush -> newBush.setLastBushTick(propagatedTick));
                    }
                }
                while (nextCalendarTick < currentCalendarTick);

                if (state != newState)
                {
                    level.setBlockAndUpdate(pos, newState);
                }
            }
        }

        ci.cancel();
    }
}
