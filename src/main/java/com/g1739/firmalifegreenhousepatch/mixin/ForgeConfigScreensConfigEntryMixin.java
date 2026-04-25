package com.g1739.firmalifegreenhousepatch.mixin;

import fuzs.forgeconfigscreens.client.gui.data.IEntryData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "fuzs.forgeconfigscreens.client.gui.screens.ConfigScreen$ConfigEntry", remap = false)
public abstract class ForgeConfigScreensConfigEntryMixin
{
    @Redirect(
        method = "addLines",
        at = @At(
            value = "INVOKE",
            target = "Lfuzs/forgeconfigscreens/client/gui/data/IEntryData;getPath()Ljava/lang/String;"
        )
    )
    private String flgp$useTranslatedTitleInTooltip(IEntryData entryData)
    {
        final String translated = entryData.getTitle().getString();
        return translated.isBlank() ? entryData.getPath() : translated;
    }
}
