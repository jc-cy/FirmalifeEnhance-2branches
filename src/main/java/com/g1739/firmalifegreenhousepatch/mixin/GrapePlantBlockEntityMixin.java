package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(com.eerussianguy.firmalife.common.blockentities.GrapePlantBlockEntity.class)
public abstract class GrapePlantBlockEntityMixin
{
    @Redirect(
        method = "updateTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/climate/Climate;getInstantTemperature(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)F"
        ),
        require = 0
    )
    private float flgp$useGreenhouseInstantTemperature(Level level, BlockPos pos)
    {
        return GreenhouseTemperatureHelper.getControlledTemperature(level, pos, Climate.getInstantTemperature(level, pos));
    }

    @Redirect(
        method = "updateTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/util/calendar/ICalendar;getHemispheralCalendarMonthOfYear(Z)Lnet/dries007/tfc/util/calendar/Month;"
        ),
        require = 0
    )
    private Month flgp$ignoreGrapeSeasonInsideGreenhouse(ICalendar calendar, boolean northernHemisphere)
    {
        final BlockEntity self = (BlockEntity) (Object) this;
        final Level level = self.getLevel();
        if (level != null && GreenhouseTemperatureHelper.isControlledGreenhouse(level, self.getBlockPos()))
        {
            return Month.JULY;
        }
        return calendar.getHemispheralCalendarMonthOfYear(northernHemisphere);
    }
}
