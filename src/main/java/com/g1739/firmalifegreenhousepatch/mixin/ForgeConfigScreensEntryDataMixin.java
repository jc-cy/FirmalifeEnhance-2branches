package com.g1739.firmalifegreenhousepatch.mixin;

import com.g1739.firmalifegreenhousepatch.client.ConfigScreenTranslationHelper;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "fuzs.forgeconfigscreens.client.gui.data.EntryData", remap = false)
public abstract class ForgeConfigScreensEntryDataMixin
{
    private static final String CATEGORY_ENTRY_CLASS = "fuzs.forgeconfigscreens.client.gui.data.EntryData$CategoryEntryData";

    @Shadow @Final private String path;

    @Inject(method = "getTitle", at = @At("HEAD"), cancellable = true)
    private void flgp$useTranslatedCategoryTitle(CallbackInfoReturnable<Component> cir)
    {
        if (!CATEGORY_ENTRY_CLASS.equals(getClass().getName()))
        {
            return;
        }

        final Component translated = ConfigScreenTranslationHelper.getCategoryTitle(path);
        if (translated != null)
        {
            cir.setReturnValue(translated);
        }
    }
}
