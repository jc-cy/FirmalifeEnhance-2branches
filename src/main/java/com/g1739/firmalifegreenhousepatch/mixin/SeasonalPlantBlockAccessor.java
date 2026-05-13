package com.g1739.firmalifegreenhousepatch.mixin;

import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.calendar.Month;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock.class)
public interface SeasonalPlantBlockAccessor
{
    @Invoker("getLifecycleForMonth")
    Lifecycle flgp$invokeGetLifecycleForMonth(Month month);
}
