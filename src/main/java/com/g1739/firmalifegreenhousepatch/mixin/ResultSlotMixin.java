package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.recipe.GreenhouseSalvageRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin
{
    @Shadow @Final private Player player;
    @Shadow @Final private CraftingContainer craftSlots;

    @Shadow protected abstract void checkTakeAchievements(ItemStack stack);

    @Inject(method = "onTake", at = @At("HEAD"), cancellable = true)
    private void flgp$handleGreenhouseSalvage(Player player, ItemStack stack, CallbackInfo ci)
    {
        final CraftingInput.Positioned positioned = craftSlots.asPositionedCraftInput();
        final CraftingInput input = positioned.input();
        final RecipeHolder<?> holder = player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, player.level()).orElse(null);
        if (holder == null || !(holder.value() instanceof GreenhouseSalvageRecipe recipe))
        {
            return;
        }

        checkTakeAchievements(stack);

        CommonHooks.setCraftingPlayer(player);
        final NonNullList<ItemStack> remaining = recipe.getRemainingItems(input);
        CommonHooks.setCraftingPlayer(null);

        for (int row = 0; row < input.height(); row++)
        {
            for (int column = 0; column < input.width(); column++)
            {
                final int slot = column + positioned.left() + (row + positioned.top()) * craftSlots.getWidth();
                ItemStack current = craftSlots.getItem(slot);
                final ItemStack remainder = remaining.get(column + row * input.width());

                if (!current.isEmpty())
                {
                    final int removeCount = recipe.ingredient().test(current) ? recipe.inputCount() : 1;
                    craftSlots.removeItem(slot, removeCount);
                    current = craftSlots.getItem(slot);
                }

                if (!remainder.isEmpty())
                {
                    if (current.isEmpty())
                    {
                        craftSlots.setItem(slot, remainder);
                    }
                    else if (ItemStack.isSameItemSameComponents(current, remainder))
                    {
                        remainder.grow(current.getCount());
                        craftSlots.setItem(slot, remainder);
                    }
                    else if (!this.player.getInventory().add(remainder))
                    {
                        this.player.drop(remainder, false);
                    }
                }
            }
        }

        ci.cancel();
    }
}
