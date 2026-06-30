package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    private void flgp$syncCrate()
    {
        CellarPreservationHelper.syncContainerBlockEntity((BlockEntity) (Object) this, (Container) (Object) this);
    }
}
