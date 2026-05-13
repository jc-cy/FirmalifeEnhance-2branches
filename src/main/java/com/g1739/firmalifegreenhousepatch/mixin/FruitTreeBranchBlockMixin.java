package com.g1739.firmalifegreenhousepatch.mixin;

import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.util.climate.ClimateRange;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.dries007.tfc.common.blocks.plant.fruit.FruitTreeBranchBlock.class)
public abstract class FruitTreeBranchBlockMixin
{
    @Redirect(
        method = "addHoeOverlayInfo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/blocks/soil/FarmlandBlock;getAverageTemperatureTooltip(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/dries007/tfc/util/climate/ClimateRange;Z)Lnet/minecraft/network/chat/Component;"
        ),
        require = 0
    )
    private Component flgp$useStemTemperatureTooltip(Level level, BlockPos rootPos, ClimateRange validRange, boolean allowWiggle)
    {
        return FarmlandBlock.getAverageTemperatureTooltip(level, rootPos.above(), validRange, allowWiggle);
    }
}
