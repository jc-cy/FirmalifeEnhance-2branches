package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.dries007.tfc.common.blockentities.TFCChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(net.minecraft.world.level.block.ChestBlock.class)
public abstract class TFCChestBlockMixin
{
    @Inject(method = "onRemove", at = @At("HEAD"), require = 0)
    private void flgp$sanitizeDroppedCellarItems(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving, CallbackInfo ci)
    {
        if (state.is(newState.getBlock()) || level.isClientSide())
        {
            return;
        }
        if (level.getBlockEntity(pos) instanceof TFCChestBlockEntity chest)
        {
            CellarPreservationHelper.sanitizeChestForDrop(chest);
        }
    }
}
