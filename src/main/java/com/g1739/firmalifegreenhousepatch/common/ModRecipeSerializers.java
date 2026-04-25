package com.g1739.firmalifegreenhousepatch.common;

import com.g1739.firmalifegreenhousepatch.FirmalifeGreenhousePatch;
import com.g1739.firmalifegreenhousepatch.common.recipe.GreenhouseSalvageRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers
{
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, FirmalifeGreenhousePatch.MOD_ID);

    public static final RegistryObject<RecipeSerializer<GreenhouseSalvageRecipe>> GREENHOUSE_SALVAGE =
        RECIPE_SERIALIZERS.register("greenhouse_salvage", GreenhouseSalvageRecipe.Serializer::new);

    private ModRecipeSerializers() {}
}
