package com.g1739.firmalifegreenhousepatch.common;

import com.g1739.firmalifegreenhousepatch.FirmalifeGreenhousePatch;
import com.g1739.firmalifegreenhousepatch.common.config.PatchConfig;
import java.util.ArrayList;
import java.util.List;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.minecraft.resources.ResourceLocation;

public final class ModFoodTraits
{
    private static final List<String> TRAIT_NAMES = List.of(
        "cellar_8x",
        "cellar_7x",
        "cellar_6x",
        "cellar_5x",
        "cellar_4x",
        "cellar_3x",
        "cellar_2_5x"
    );
    private static final List<String> TOOLTIP_KEYS = List.of(
        "firmalife_greenhouse_patch.tooltip.food_trait.cellar_level_1",
        "firmalife_greenhouse_patch.tooltip.food_trait.cellar_level_2",
        "firmalife_greenhouse_patch.tooltip.food_trait.cellar_level_3",
        "firmalife_greenhouse_patch.tooltip.food_trait.cellar_level_4",
        "firmalife_greenhouse_patch.tooltip.food_trait.cellar_level_5",
        "firmalife_greenhouse_patch.tooltip.food_trait.cellar_level_6",
        "firmalife_greenhouse_patch.tooltip.food_trait.cellar_level_7"
    );

    private static List<FoodTrait> cellarTraits = List.of();
    private static boolean initialized = false;

    private ModFoodTraits() {}

    public static synchronized void init()
    {
        if (initialized)
        {
            return;
        }

        PatchConfig.refreshCaches();
        final List<PatchConfig.CellarLevel> levels = PatchConfig.getCellarPreservationLevels();
        final List<FoodTrait> registeredTraits = new ArrayList<>(TRAIT_NAMES.size());
        for (int index = 0; index < TRAIT_NAMES.size(); index++)
        {
            final float multiplier = levels.get(Math.min(index, levels.size() - 1)).preservationMultiplier();
            final float decayModifier = multiplier <= 0f ? 1f : 1f / multiplier;
            registeredTraits.add(register(TRAIT_NAMES.get(index), TOOLTIP_KEYS.get(index), decayModifier));
        }

        cellarTraits = List.copyOf(registeredTraits);
        initialized = true;
    }

    public static List<FoodTrait> getCellarTraits()
    {
        ensureInitialized();
        return cellarTraits;
    }

    public static FoodTrait getCellarTrait(int index)
    {
        ensureInitialized();
        if (cellarTraits.isEmpty())
        {
            throw new IllegalStateException("Cellar food traits have not been registered");
        }
        final int clampedIndex = Math.max(0, Math.min(index, cellarTraits.size() - 1));
        return cellarTraits.get(clampedIndex);
    }

    public static FoodTrait getDefaultCellarTrait()
    {
        ensureInitialized();
        return getCellarTrait(cellarTraits.size() - 1);
    }

    private static void ensureInitialized()
    {
        if (!initialized)
        {
            init();
        }
    }

    private static FoodTrait register(String name, String tooltipKey, float decayModifier)
    {
        return FoodTrait.register(
            new ResourceLocation(FirmalifeGreenhousePatch.MOD_ID, name),
            new FoodTrait(decayModifier, tooltipKey)
        );
    }
}
