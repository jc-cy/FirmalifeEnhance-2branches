package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(com.eerussianguy.firmalife.common.blockentities.FLBeehiveBlockEntity.class)
public abstract class FLBeehiveBlockEntityMixin
{
    @Redirect(
        method = "tryPeriodicUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getInstantTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/util/calendar/ICalendar;J)F"
        ),
        require = 0
    )
    private float flgp$useGreenhousePeriodicTemperature(Level level, BlockPos pos, ICalendar calendar, long tick)
    {
        return GreenhouseTemperatureHelper.getControlledTemperature(level, pos, Climate.getInstantTemperature(level, pos, calendar, tick));
    }

    @Redirect(
        method = "isWarmEnough",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getInstantTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F"
        ),
        require = 0
    )
    private float flgp$useGreenhouseWarmEnoughTemperature(Level level, BlockPos pos)
    {
        return GreenhouseTemperatureHelper.getControlledTemperature(level, pos, Climate.getInstantTemperature(level, pos));
    }
}
