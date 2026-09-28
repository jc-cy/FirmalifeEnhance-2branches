package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 双箱（大箱子）在打开时暴露给菜单的是原版 {@link CompoundContainer}，
 * 而不是任何一个方块实体，所以"按方块实体同步"的那条链看不到它。
 * 这里在合并容器变化时分别同步两半，保证大箱子里的食物也能拿到地窖保鲜。
 */
@Mixin(CompoundContainer.class)
public abstract class CompoundContainerMixin
{
    @Shadow @Final private Container container1;
    @Shadow @Final private Container container2;

    @Inject(method = "setChanged", at = @At("TAIL"), require = 0)
    private void flgp$syncCompoundChestCellarTraits(CallbackInfo ci)
    {
        flgp$syncContainer(container1);
        flgp$syncContainer(container2);
    }

    private void flgp$syncContainer(Container container)
    {
        if (container instanceof BlockEntity blockEntity)
        {
            CellarPreservationHelper.syncContainerBlockEntity(blockEntity, container);
        }
    }
}
