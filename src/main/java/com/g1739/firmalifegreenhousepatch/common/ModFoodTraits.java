package com.g1739.firmalifegreenhousepatch.common;

import com.g1739.firmalifegreenhousepatch.FirmalifeGreenhousePatch;
import com.g1739.firmalifegreenhousepatch.common.config.PatchConfig;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import java.util.List;
import java.util.Set;
import net.dries007.tfc.common.component.food.FoodTrait;
import net.dries007.tfc.common.component.food.FoodTraits;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFoodTraits
{
    public static final DeferredRegister<FoodTrait> TRAITS = DeferredRegister.create(FoodTraits.KEY, FirmalifeGreenhousePatch.MOD_ID);

    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_8X = register("cellar_8x", 0);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_7X = register("cellar_7x", 1);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_6X = register("cellar_6x", 2);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_5X = register("cellar_5x", 3);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_4X = register("cellar_4x", 4);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_3X = register("cellar_3x", 5);
    public static final DeferredHolder<FoodTrait, FoodTrait> CELLAR_2_5X = register("cellar_2_5x", 6);

    public static final Set<DeferredHolder<FoodTrait, FoodTrait>> CELLAR_TRAITS = Set.of(
        CELLAR_8X,
        CELLAR_7X,
        CELLAR_6X,
        CELLAR_5X,
        CELLAR_4X,
        CELLAR_3X,
        CELLAR_2_5X
    );

    private static final List<DeferredHolder<FoodTrait, FoodTrait>> ORDERED_CELLAR_TRAITS = List.of(
        CELLAR_8X,
        CELLAR_7X,
        CELLAR_6X,
        CELLAR_5X,
        CELLAR_4X,
        CELLAR_3X,
        CELLAR_2_5X
    );

    private ModFoodTraits() {}

    public static List<DeferredHolder<FoodTrait, FoodTrait>> getCellarTraits()
    {
        return ORDERED_CELLAR_TRAITS;
    }

    public static DeferredHolder<FoodTrait, FoodTrait> getCellarTrait(int index)
    {
        final int clampedIndex = Math.max(0, Math.min(index, ORDERED_CELLAR_TRAITS.size() - 1));
        return ORDERED_CELLAR_TRAITS.get(clampedIndex);
    }

    public static DeferredHolder<FoodTrait, FoodTrait> getDefaultCellarTrait()
    {
        return getCellarTrait(ORDERED_CELLAR_TRAITS.size() - 1);
    }

    public static boolean isPatchCellarTrait(FoodTrait trait)
    {
        for (DeferredHolder<FoodTrait, FoodTrait> holder : ORDERED_CELLAR_TRAITS)
        {
            if (holder.isBound() && holder.value() == trait)
            {
                return true;
            }
        }
        return false;
    }

    public static String getCellarTraitMultiplierText(FoodTrait trait)
    {
        final float decayModifier = trait.getDecayModifier();
        final float multiplier = decayModifier <= 0f ? Float.POSITIVE_INFINITY : 1f / decayModifier;
        return GreenhouseTemperatureHelper.formatFactor(multiplier);
    }

    private static DeferredHolder<FoodTrait, FoodTrait> register(String name, int levelIndex)
    {
        return TRAITS.register(name, () -> new FoodTrait(
            () -> {
                final List<PatchConfig.CellarLevel> levels = PatchConfig.getCellarPreservationLevels();
                final float multiplier = levels.get(Math.min(levelIndex, levels.size() - 1)).preservationMultiplier();
                return multiplier <= 0f ? 1d : 1d / multiplier;
            },
            "firmalife_greenhouse_patch.tooltip.food_trait.cellar_level_" + (levelIndex + 1)
        ));
    }
}
