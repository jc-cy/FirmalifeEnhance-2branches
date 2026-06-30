package com.g1739.firmalifegreenhousepatch.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class FirmalifeGreenhousePatchMixinPlugin implements IMixinConfigPlugin
{
    private static final String IE_MOD_ID = "immersiveengineering";
    private static final Set<String> IE_ONLY_MIXINS = Set.of(
        "com.g1739.firmalifegreenhousepatch.mixin.ImmersiveEngineeringCrateBlockEntityMixin"
    );

    private static Boolean immersiveEngineeringLoaded;

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
        if (IE_ONLY_MIXINS.contains(mixinClassName))
        {
            return isImmersiveEngineeringLoaded();
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

    private static boolean isImmersiveEngineeringLoaded()
    {
        if (immersiveEngineeringLoaded == null)
        {
            immersiveEngineeringLoaded = isModLoaded(IE_MOD_ID);
        }
        return immersiveEngineeringLoaded;
    }

    private static boolean isModLoaded(String modId)
    {
        final LoadingModList loadingModList = FMLLoader.getLoadingModList();
        return loadingModList != null && loadingModList.getModFileById(modId) != null;
    }
}
