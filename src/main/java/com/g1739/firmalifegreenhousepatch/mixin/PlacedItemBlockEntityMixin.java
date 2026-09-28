package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import net.dries007.tfc.common.blockentities.PlacedItemBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 地板上放置的食物被玩家右键捡起时，也要清掉地窖专用保鲜 trait，
 * 否则会出现"丢在地上再捡起来仍带着保鲜"的泄漏路径。
 */
@Mixin(value = PlacedItemBlockEntity.class, remap = false)
public abstract class PlacedItemBlockEntityMixin
{
    @ModifyArg(
        method = "onRightClick(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;ZZ)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/items/ItemHandlerHelper;giveItemToPlayer(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            remap = false
        ),
        index = 1,
        require = 0
    )
    private ItemStack flgp$sanitizePlacedItemPickup(ItemStack stack)
    {
        return CellarPreservationHelper.sanitizeTakenStack(stack);
    }
}
