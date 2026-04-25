package com.g1739.firmalifegreenhousepatch.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class FirmalifeGreenhousePatchMixinPlugin implements IMixinConfigPlugin
{
    private static final String TFE_MOD_ID = "tfe";
    private static final String CONFIGURED_MOD_ID = "configured";
    private static final String FORGE_CONFIG_SCREENS_MOD_ID = "forgeconfigscreens";
    private static final Set<String> ORIGINAL_ONLY_MIXINS = Set.of(
        "com.g1739.firmalifegreenhousepatch.mixin.BananaPlantBlockMixin",
        "com.g1739.firmalifegreenhousepatch.mixin.FruitTreeBranchBlockMixin",
        "com.g1739.firmalifegreenhousepatch.mixin.StationaryBerryBushBlockMixin",
        "com.g1739.firmalifegreenhousepatch.mixin.GrowingFruitTreeBranchBlockMixin",
        "com.g1739.firmalifegreenhousepatch.mixin.FruitTreeSaplingBlockMixin"
    );
    private static final Set<String> TFE_ONLY_MIXINS = Set.of(
        "com.g1739.firmalifegreenhousepatch.mixin.TfeBananaPlantBlockMixin",
        "com.g1739.firmalifegreenhousepatch.mixin.TfeStationaryBerryBushBlockMixin",
        "com.g1739.firmalifegreenhousepatch.mixin.TfeFruitTreeSaplingBlockMixin"
    );
    private static final Set<String> CONFIGURED_ONLY_MIXINS = Set.of(
        "com.g1739.firmalifegreenhousepatch.mixin.ConfiguredForgeFolderEntryMixin",
        "com.g1739.firmalifegreenhousepatch.mixin.ConfiguredForgeValueMixin"
    );
    private static final Set<String> FORGE_CONFIG_SCREENS_ONLY_MIXINS = Set.of(
        "com.g1739.firmalifegreenhousepatch.mixin.ForgeConfigScreensConfigEntryMixin",
        "com.g1739.firmalifegreenhousepatch.mixin.ForgeConfigScreensEntryDataMixin"
    );

    private static Boolean tfeLoaded;
    private static Boolean configuredLoaded;
    private static Boolean forgeConfigScreensLoaded;

    @Override
    public void onLoad(String mixinPackage)
    {
    }

    @Override
    public String getRefMapperConfig()
    {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        if (ORIGINAL_ONLY_MIXINS.contains(mixinClassName))
        {
            return !isTfeLoaded();
        }
        if (TFE_ONLY_MIXINS.contains(mixinClassName))
        {
            return isTfeLoaded();
        }
        if (CONFIGURED_ONLY_MIXINS.contains(mixinClassName))
        {
            return isConfiguredLoaded();
        }
        if (FORGE_CONFIG_SCREENS_ONLY_MIXINS.contains(mixinClassName))
        {
            return isForgeConfigScreensLoaded();
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
    {
    }

    @Override
    public List<String> getMixins()
    {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {
    }

    private static boolean isTfeLoaded()
    {
        if (tfeLoaded == null)
        {
            tfeLoaded = isModLoaded(TFE_MOD_ID);
        }
        return tfeLoaded;
    }

    private static boolean isConfiguredLoaded()
    {
        if (configuredLoaded == null)
        {
            configuredLoaded = isModLoaded(CONFIGURED_MOD_ID);
        }
        return configuredLoaded;
    }

    private static boolean isForgeConfigScreensLoaded()
    {
        if (forgeConfigScreensLoaded == null)
        {
            forgeConfigScreensLoaded = isModLoaded(FORGE_CONFIG_SCREENS_MOD_ID);
        }
        return forgeConfigScreensLoaded;
    }

    private static boolean isModLoaded(String modId)
    {
        final LoadingModList loadingModList = FMLLoader.getLoadingModList();
        return loadingModList != null && loadingModList.getModFileById(modId) != null;
    }
}
