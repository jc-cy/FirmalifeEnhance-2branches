package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版箱子（{@link ChestBlockEntity}）没有 TFC 箱子那套回调，且 NeoForge 的物品能力表已被原版容器占据、
 * 后注册的 provider 不会被查询，拿不到插入 / 取出事件，所以直接钩 {@link BaseContainerBlockEntity} 的
 * {@code setItem} / {@code removeItem}：漏斗、管道与原版箱子手动操作都会经过这两个方法。
 */
@Mixin(BaseContainerBlockEntity.class)
public abstract class CellarChestBlockEntityMixin
{
    @ModifyVariable(method = "setItem", at = @At("HEAD"), argsOnly = true, require = 0)
    private ItemStack flgp$tagInsertedCellarStack(ItemStack stack)
    {
        final BlockEntity self = (BlockEntity) (Object) this;
        return self instanceof ChestBlockEntity ? CellarPreservationHelper.tagInsertedStack(self, stack) : stack;
    }

    @Inject(method = "removeItem", at = @At("RETURN"), cancellable = true, require = 0)
    private void flgp$sanitizeRemovedCellarStack(int index, int count, CallbackInfoReturnable<ItemStack> cir)
    {
        final BlockEntity self = (BlockEntity) (Object) this;
        final ItemStack removed = cir.getReturnValue();
        if (self instanceof ChestBlockEntity && removed != null && !removed.isEmpty())
        {
            cir.setReturnValue(CellarPreservationHelper.sanitizeExtractedStack(self, removed));
        }
    }
}
