package com.g1739.firmalifegreenhousepatch.mixin;

import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.climate.ClimateRange;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Supplier;

@Mixin(value = net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock.class, remap = false)
public interface SeasonalPlantBlockAccessor
{
    @Accessor(value = "climateRange", remap = false)
    Supplier<ClimateRange> flgp$getClimateRange();

    @Invoker(value = "getLifecycleForCurrentMonth", remap = false)
    Lifecycle flgp$invokeGetLifecycleForCurrentMonth();

    @Invoker(value = "getLifecycleForMonth", remap = false)
    Lifecycle flgp$invokeGetLifecycleForMonth(Month month);
}
