package com.g1739.firmalifegreenhousepatch.common;

import com.g1739.firmalifegreenhousepatch.FirmalifeGreenhousePatch;
import java.util.Set;
import net.dries007.tfc.common.component.food.FoodTrait;
import net.dries007.tfc.common.component.food.FoodTraits;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFoodTraits
{
    public static final DeferredRegister<FoodTrait> TRAITS = DeferredRegister.create(FoodTraits.KEY, FirmalifeGreenhousePatch.MOD_ID);

    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_2_5X = register("cellar_2_5x", 0.4d);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_3X = register("cellar_3x", 1d / 3d);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_4X = register("cellar_4x", 0.25d);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_5X = register("cellar_5x", 0.2d);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_6X = register("cellar_6x", 1d / 6d);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_7X = register("cellar_7x", 1d / 7d);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_8X = register("cellar_8x", 0.125d);

    public static final Set<DeferredHolder<FoodTrait, FoodTrait>> CELLAR_TRAITS = Set.of(
        CELLAR_2_5X,
        CELLAR_3X,
        CELLAR_4X,
        CELLAR_5X,
        CELLAR_6X,
        CELLAR_7X,
        CELLAR_8X
    );

    private ModFoodTraits() {}

    private static DeferredHolder<FoodTrait, FoodTrait> register(String name, double decayModifier)
    {
        return TRAITS.register(name, () -> new FoodTrait(() -> decayModifier, null));
    }
}
