package com.g1739.firmalifegreenhousepatch.common.recipe;

import com.g1739.firmalifegreenhousepatch.common.ModRecipeSerializers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.recipes.RecipeHelpers;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class GreenhouseSalvageRecipe extends CustomRecipe
{
    public static final MapCodec<GreenhouseSalvageRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(GreenhouseSalvageRecipe::ingredient),
        Codec.INT.fieldOf("input_count").forGetter(GreenhouseSalvageRecipe::inputCount),
        ItemStack.CODEC.fieldOf("result").forGetter(GreenhouseSalvageRecipe::result),
        ItemStack.CODEC.optionalFieldOf("extra_result", ItemStack.EMPTY).forGetter(GreenhouseSalvageRecipe::extraResult)
    ).apply(instance, GreenhouseSalvageRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GreenhouseSalvageRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, GreenhouseSalvageRecipe::ingredient,
        ByteBufCodecs.INT, GreenhouseSalvageRecipe::inputCount,
        ItemStack.STREAM_CODEC, GreenhouseSalvageRecipe::result,
        ItemStack.OPTIONAL_STREAM_CODEC, GreenhouseSalvageRecipe::extraResult,
        GreenhouseSalvageRecipe::new
    );

    private final Ingredient ingredient;
    private final int inputCount;
    private final ItemStack result;
    private final ItemStack extraResult;

    public GreenhouseSalvageRecipe(Ingredient ingredient, int inputCount, ItemStack result, ItemStack extraResult)
    {
        super(CraftingBookCategory.MISC);
        this.ingredient = ingredient;
        this.inputCount = inputCount;
        this.result = result;
        this.extraResult = extraResult;
    }

    public Ingredient ingredient()
    {
        return ingredient;
    }

    public int inputCount()
    {
        return inputCount;
    }

    public ItemStack result()
    {
        return result;
    }

    public ItemStack extraResult()
    {
        return extraResult;
    }

    public ItemStack displayInputStack()
    {
        final ItemStack[] items = ingredient.getItems();
        if (items.length == 0)
        {
            return ItemStack.EMPTY;
        }

        final ItemStack stack = items[0].copy();
        stack.setCount(inputCount);
        return stack;
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return findMatch(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries)
    {
        return findMatch(input) != null ? result.copy() : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input)
    {
        final NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        final @Nullable Match match = findMatch(input);
        if (match == null)
        {
            return remaining;
        }

        remaining.set(match.sawSlot(), damageGemSaw(input.getItem(match.sawSlot())));
        if (!extraResult.isEmpty())
        {
            remaining.set(match.ingredientSlot(), extraResult.copy());
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return width * height >= 2;
    }

    @Override
    public boolean isSpecial()
    {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries)
    {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients()
    {
        return NonNullList.of(Ingredient.EMPTY, Ingredient.of(TFCItems.GEM_SAW.get()), ingredient);
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return ModRecipeSerializers.GREENHOUSE_SALVAGE.get();
    }

    private @Nullable Match findMatch(CraftingInput input)
    {
        int sawSlot = -1;
        int ingredientSlot = -1;

        for (int slot = 0; slot < input.size(); slot++)
        {
            final ItemStack stack = input.getItem(slot);
            if (stack.isEmpty())
            {
                continue;
            }

            if (stack.is(TFCItems.GEM_SAW.get()))
            {
                if (sawSlot != -1)
                {
                    return null;
                }
                sawSlot = slot;
                continue;
            }

            if (ingredientSlot != -1 || !ingredient.test(stack) || stack.getCount() < inputCount)
            {
                return null;
            }
            ingredientSlot = slot;
        }

        return sawSlot != -1 && ingredientSlot != -1 ? new Match(sawSlot, ingredientSlot) : null;
    }

    private static ItemStack damageGemSaw(ItemStack stack)
    {
        final ItemStack copy = stack.copyWithCount(1);
        if (copy.isDamageableItem())
        {
            final @Nullable Player player = RecipeHelpers.getCraftingPlayer();
            if (player != null)
            {
                Helpers.damageItem(copy, player.level());
            }
            else
            {
                Helpers.damageItem(copy);
            }
        }
        return copy.isEmpty() ? ItemStack.EMPTY : copy;
    }

    private record Match(int sawSlot, int ingredientSlot) {}
}
