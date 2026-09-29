package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "blusunrize.immersiveengineering.common.blocks.wooden.WoodenCrateBlockEntity", remap = false)
public abstract class ImmersiveEngineeringCrateBlockEntityMixin
{
    @Inject(method = "loadAdditional", at = @At("TAIL"), require = 0)
    private void flgp$syncCellarTraitsOnLoad(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci)
    {
        flgp$syncCrate();
    }

    @Inject(method = "onBEPlaced(Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"), require = 0)
    private void flgp$syncCellarTraitsOnPlaced(ItemStack stack, CallbackInfo ci)
    {
        flgp$syncCrate();
    }

    @Inject(method = "getBlockEntityDrop", at = @At("HEAD"), require = 0)
    private void flgp$sanitizeCellarTraitsBeforeDrop(LootContext context, Consumer<ItemStack> drop, CallbackInfo ci)
    {
        CellarPreservationHelper.sanitizeContainerBlockEntityForDrop((BlockEntity) (Object) this, (Container) (Object) this);
    }

    @Inject(method = "getInventoryCap", at = @At("RETURN"), cancellable = true, require = 0)
    private void flgp$wrapCellarCrateHandler(CallbackInfoReturnable<IItemHandler> cir)
    {
        cir.setReturnValue(CellarPreservationHelper.wrapBlockEntityItemHandler((BlockEntity) (Object) this, cir.getReturnValue()));
    }

    private void flgp$syncCrate()
    {
        CellarPreservationHelper.syncContainerBlockEntity((BlockEntity) (Object) this, (Container) (Object) this);
    }
}
