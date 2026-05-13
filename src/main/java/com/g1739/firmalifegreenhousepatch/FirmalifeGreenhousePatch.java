package com.g1739.firmalifegreenhousepatch;

import com.g1739.firmalifegreenhousepatch.client.PatchClientEventHandler;
import com.g1739.firmalifegreenhousepatch.common.ModFoodTraits;
import com.g1739.firmalifegreenhousepatch.common.ModMenuTypes;
import com.g1739.firmalifegreenhousepatch.common.ModRecipeSerializers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(FirmalifeGreenhousePatch.MOD_ID)
public final class FirmalifeGreenhousePatch
{
    public static final String MOD_ID = "firmalife_greenhouse_patch";

    public FirmalifeGreenhousePatch(ModContainer mod, IEventBus bus)
    {
        ModFoodTraits.TRAITS.register(bus);
        ModMenuTypes.MENUS.register(bus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(bus);

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            PatchClientEventHandler.init(bus);
        }
    }
}
