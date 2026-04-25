package com.g1739.firmalifegreenhousepatch.common.recipe;

import com.google.gson.JsonObject;
import com.g1739.firmalifegreenhousepatch.common.ModRecipeSerializers;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.recipes.RecipeSerializerImpl;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.Nullable;

public final class GreenhouseSalvageRecipe extends CustomRecipe
{
    private final Ingredient ingredient;
    private final int inputCount;
    private final ItemStack result;
    private final ItemStack extraResult;

    public GreenhouseSalvageRecipe(ResourceLocation id, Ingredient ingredient, int inputCount, ItemStack result, ItemStack extraResult)
    {
        super(id, CraftingBookCategory.MISC);
        this.ingredient = ingredient;
        this.inputCount = validatePositiveCount(inputCount, "input_count");
        this.result = result.copy();
        this.extraResult = extraResult.copy();
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
        return result.copy();
    }

    public ItemStack extraResult()
    {
        return extraResult.copy();
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
    public boolean matches(CraftingContainer input, Level level)
    {
        return findMatch(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries)
    {
        return findMatch(input) != null ? result.copy() : ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input)
    {
        final NonNullList<ItemStack> remaining = NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);
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
    public ItemStack getResultItem(RegistryAccess registries)
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

    private @Nullable Match findMatch(CraftingContainer input)
    {
        int sawSlot = -1;
        int ingredientSlot = -1;

        for (int slot = 0; slot < input.getContainerSize(); slot++)
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
            Helpers.damageItem(copy, 1);
        }
        return copy.isEmpty() ? ItemStack.EMPTY : copy;
    }

    private record Match(int sawSlot, int ingredientSlot) {}

    public static final class Serializer extends RecipeSerializerImpl<GreenhouseSalvageRecipe>
    {
        @Override
        public GreenhouseSalvageRecipe fromJson(ResourceLocation recipeId, JsonObject json)
        {
            final Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            final int inputCount = validatePositiveCount(GsonHelper.getAsInt(json, "input_count"), "input_count");
            final ItemStack result = readStack(GsonHelper.getAsJsonObject(json, "result"));
            final ItemStack extraResult = json.has("extra_result") ? readStack(GsonHelper.getAsJsonObject(json, "extra_result")) : ItemStack.EMPTY;
            return new GreenhouseSalvageRecipe(recipeId, ingredient, inputCount, result, extraResult);
        }

        @Nullable
        @Override
        public GreenhouseSalvageRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer)
        {
            final Ingredient ingredient = Ingredient.fromNetwork(buffer);
            final int inputCount = validatePositiveCount(buffer.readVarInt(), "input_count");
            final ItemStack result = buffer.readItem();
            final ItemStack extraResult = buffer.readItem();
            return new GreenhouseSalvageRecipe(recipeId, ingredient, inputCount, result, extraResult);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, GreenhouseSalvageRecipe recipe)
        {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeVarInt(recipe.inputCount);
            buffer.writeItem(recipe.result);
            buffer.writeItem(recipe.extraResult);
        }

        private static ItemStack readStack(JsonObject json)
        {
            final String itemName = json.has("id") ? GsonHelper.getAsString(json, "id") : GsonHelper.getAsString(json, "item");
            final Item item = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(itemName))
                .orElseThrow(() -> new IllegalArgumentException("Unknown item: " + itemName));
            final int count = validatePositiveCount(GsonHelper.getAsInt(json, "count", 1), itemName + ".count");
            return new ItemStack(item, count);
        }
    }

    private static int validatePositiveCount(int count, String fieldName)
    {
        if (count <= 0)
        {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return count;
    }
}
