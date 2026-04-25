package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.common.recipe.GreenhouseSalvageRecipe;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin
{
    @Unique private int flgp$salvageIngredientSlot = -1;
    @Unique private int flgp$salvageInputCount = 1;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void flgp$captureGreenhouseSalvageState(Player player, ItemStack stack, CallbackInfo ci)
    {
        final ResultSlot self = (ResultSlot) (Object) this;
        if (!(self.container instanceof CraftingContainer craftSlots))
        {
            flgp$clearGreenhouseSalvageState();
            return;
        }

        final var matchedRecipe = player.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftSlots, player.level()).orElse(null);
        if (!(matchedRecipe instanceof GreenhouseSalvageRecipe recipe))
        {
            flgp$clearGreenhouseSalvageState();
            return;
        }

        flgp$salvageIngredientSlot = flgp$findIngredientSlot(craftSlots, recipe);
        flgp$salvageInputCount = recipe.inputCount();
    }

    @Redirect(
        method = "onTake",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack flgp$removeConfiguredGreenhouseInput(CraftingContainer craftSlots, int slot, int count)
    {
        final int adjustedCount = slot == flgp$salvageIngredientSlot ? Math.max(count, flgp$salvageInputCount) : count;
        return craftSlots.removeItem(slot, adjustedCount);
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void flgp$clearGreenhouseSalvageState(Player player, ItemStack stack, CallbackInfo ci)
    {
        flgp$clearGreenhouseSalvageState();
    }

    @Unique
    private int flgp$findIngredientSlot(CraftingContainer craftSlots, GreenhouseSalvageRecipe recipe)
    {
        for (int slot = 0; slot < craftSlots.getContainerSize(); slot++)
        {
            final ItemStack candidate = craftSlots.getItem(slot);
            if (!candidate.isEmpty() && !candidate.is(TFCItems.GEM_SAW.get()) && recipe.ingredient().test(candidate))
            {
                return slot;
            }
        }
        return -1;
    }

    @Unique
    private void flgp$clearGreenhouseSalvageState()
    {
        flgp$salvageIngredientSlot = -1;
        flgp$salvageInputCount = 1;
    }
}
