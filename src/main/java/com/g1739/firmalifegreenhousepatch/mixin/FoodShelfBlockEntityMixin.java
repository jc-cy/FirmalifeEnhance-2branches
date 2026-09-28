package com.g1739.firmalifegreenhousepatch.mixin;

import com.eerussianguy.firmalife.common.blockentities.FoodShelfBlockEntity;
import com.g1739.firmalifegreenhousepatch.common.ModFoodTraits;
import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.dries007.tfc.common.component.food.FoodTrait;
import java.util.Set;

@Mixin(FoodShelfBlockEntity.class)
public abstract class FoodShelfBlockEntityMixin
{
    @Inject(method = "getFoodTrait", at = @At("HEAD"), cancellable = true)
    private void flgp$usePatchCellarTrait(CallbackInfoReturnable<Holder<FoodTrait>> cir)
    {
        final FoodShelfBlockEntity shelf = (FoodShelfBlockEntity) (Object) this;
        final Level level = shelf.getLevel();
        cir.setReturnValue(level != null ? CellarPreservationHelper.getCellarTrait(level, shelf.getBlockPos()) : ModFoodTraits.CELLAR_2_5X);
    }

    @Inject(method = "getPossibleTraits", at = @At("HEAD"), cancellable = true)
    private void flgp$useAllCellarTraits(CallbackInfoReturnable<Set<DeferredHolder<FoodTrait, FoodTrait>>> cir)
    {
        cir.setReturnValue(CellarPreservationHelper.getPossibleTraits());
    }

    // 整体接管 Firmalife 的 updatePreservation：上游在 preserved=true 时只添加新档位、不清理旧档位，
    // 温度变化后会出现档位叠加（保鲜时间被成倍拉长）。这里改为先归一化再应用。
    @Inject(method = "updatePreservation", at = @At("HEAD"), cancellable = true, require = 0)
    private void flgp$normalizeShelfPreservation(boolean preserved, CallbackInfo ci)
    {
        CellarPreservationHelper.syncFoodShelfBlockEntity((FoodShelfBlockEntity) (Object) this, preserved);
        ci.cancel();
    }

    @Redirect(
        method = {"isItemValid", "use"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/dries007/tfc/common/component/food/FoodCapability;removeTrait(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/item/ItemStack;"
        ),
        require = 0
    )
    private ItemStack flgp$sanitizeAllCellarTraits(ItemStack stack, Holder<FoodTrait> ignored)
    {
        return CellarPreservationHelper.sanitizeTakenStack(stack);
    }
}
