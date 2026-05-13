package com.g1739.firmalifegreenhousepatch.compat.jei;

import java.util.Optional;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import com.g1739.firmalifegreenhousepatch.common.recipe.GreenhouseSalvageRecipe;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public final class GreenhouseSalvageCraftingExtension implements ICraftingCategoryExtension<GreenhouseSalvageRecipe>
{
    public static final GreenhouseSalvageCraftingExtension INSTANCE = new GreenhouseSalvageCraftingExtension();

    private static final int OUTPUT_X = 94;
    private static final int MAIN_OUTPUT_Y = 18;
    private static final int EXTRA_OUTPUT_Y = 0;
    private static final int SAW_X = 1;
    private static final int INPUT_X = 19;
    private static final int INPUT_Y = 1;

    private GreenhouseSalvageCraftingExtension() {}

    @Override
    public boolean isHandled(RecipeHolder<GreenhouseSalvageRecipe> holder)
    {
        return true;
    }

    @Override
    public Optional<ResourceLocation> getRegistryName(RecipeHolder<GreenhouseSalvageRecipe> holder)
    {
        return Optional.of(holder.id());
    }

    @Override
    public int getWidth(RecipeHolder<GreenhouseSalvageRecipe> holder)
    {
        return 2;
    }

    @Override
    public int getHeight(RecipeHolder<GreenhouseSalvageRecipe> holder)
    {
        return 1;
    }

    @Override
    public void setRecipe(RecipeHolder<GreenhouseSalvageRecipe> holder, IRecipeLayoutBuilder builder, mezz.jei.api.gui.ingredient.ICraftingGridHelper craftingGridHelper, IFocusGroup focuses)
    {
        builder.setShapeless();
        builder.addInputSlot(SAW_X, INPUT_Y).addItemStack(new ItemStack(TFCItems.GEM_SAW.get()));
        builder.addInputSlot(INPUT_X, INPUT_Y).addItemStack(holder.value().displayInputStack());
        builder.addOutputSlot(OUTPUT_X, MAIN_OUTPUT_Y).addItemStack(holder.value().result());

        final ItemStack extra = holder.value().extraResult();
        if (!extra.isEmpty())
        {
            builder.addOutputSlot(OUTPUT_X, EXTRA_OUTPUT_Y).addItemStack(extra);
        }
    }
}
