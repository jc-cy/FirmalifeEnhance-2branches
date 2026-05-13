package com.g1739.firmalifegreenhousepatch.compat.jei;

import com.g1739.firmalifegreenhousepatch.FirmalifeGreenhousePatch;
import com.g1739.firmalifegreenhousepatch.common.recipe.GreenhouseSalvageRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class PatchJeiPlugin implements IModPlugin
{
    @Override
    public ResourceLocation getPluginUid()
    {
        return ResourceLocation.fromNamespaceAndPath(FirmalifeGreenhousePatch.MOD_ID, "jei");
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registry)
    {
        registry.getCraftingCategory().addExtension(GreenhouseSalvageRecipe.class, GreenhouseSalvageCraftingExtension.INSTANCE);
    }
}
