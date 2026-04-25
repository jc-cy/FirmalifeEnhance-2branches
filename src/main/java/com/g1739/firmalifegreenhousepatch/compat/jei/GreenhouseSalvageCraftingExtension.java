package com.g1739.firmalifegreenhousepatch.compat.jei;

import com.g1739.firmalifegreenhousepatch.common.recipe.GreenhouseSalvageRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record GreenhouseSalvageCraftingExtension(GreenhouseSalvageRecipe recipe) implements ICraftingCategoryExtension
{
    private static final int OUTPUT_X = 94;
    private static final int MAIN_OUTPUT_Y = 18;
    private static final int EXTRA_OUTPUT_Y = 0;
    private static final int SAW_X = 1;
    private static final int INPUT_X = 19;
    private static final int INPUT_Y = 1;

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses)
    {
        builder.setShapeless();
        builder.addSlot(RecipeIngredientRole.INPUT, SAW_X, INPUT_Y).addItemStack(new ItemStack(TFCItems.GEM_SAW.get()));
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y).addItemStack(recipe.displayInputStack());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, MAIN_OUTPUT_Y).addItemStack(recipe.result());

        final ItemStack extra = recipe.extraResult();
        if (!extra.isEmpty())
        {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, EXTRA_OUTPUT_Y).addItemStack(extra);
        }
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return recipe.getId();
    }

    @Override
    public int getWidth()
    {
        return 2;
    }

    @Override
    public int getHeight()
    {
        return 1;
    }
}
