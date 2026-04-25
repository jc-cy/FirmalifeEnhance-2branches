package com.g1739.firmalifegreenhousepatch.mixin;

import java.util.List;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.mrcrayfish.configured.impl.forge.ForgeFolderEntry", remap = false)
public abstract class ConfiguredForgeFolderEntryMixin
{
    @Shadow @Final protected List<String> path;
    @Shadow @Final protected ForgeConfigSpec spec;

    @Inject(method = "getEntryName", at = @At("HEAD"), cancellable = true)
    private void flgp$useTranslatedFolderName(CallbackInfoReturnable<String> cir)
    {
        final String translationKey = spec.getLevelTranslationKey(path);
        if (translationKey != null && I18n.exists(translationKey))
        {
            cir.setReturnValue(Component.translatable(translationKey).getString());
        }
    }
}
