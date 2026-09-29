package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版箱子（{@link net.minecraft.world.level.block.entity.ChestBlockEntity}）的物品能力由原版容器自己提供，
 * 后注册的 provider 不会被查询，所以在这里给它的 {@code Capability} 套上地窖包装：
 * 漏斗 / 管道插入带档位、取出被剥掉。与二代 1.20 的路线一致。
 */
@Mixin(BaseContainerBlockEntity.class)
public abstract class CellarChestBlockEntityMixin
{
    @Inject(
        method = "getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private <T> void flgp$wrapCellarItemHandler(Capability<T> cap, @Nullable Direction side, CallbackInfoReturnable<LazyOptional<T>> cir)
    {
        final BlockEntity owner = (BlockEntity) (Object) this;
        if (cap == Capabilities.ITEM && CellarPreservationHelper.canWrapBlockEntityItemHandler(owner))
        {
            final LazyOptional<IItemHandler> handlers = cir.getReturnValue().cast();
            cir.setReturnValue(handlers
                .lazyMap(handler -> CellarPreservationHelper.wrapBlockEntityItemHandler(owner, handler))
                .cast());
        }
    }
}
