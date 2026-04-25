package com.g1739.firmalifegreenhousepatch.mixin;

import com.eerussianguy.firmalife.config.FLConfig;
import com.g1739.firmalifegreenhousepatch.common.config.PatchConfig;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = com.eerussianguy.firmalife.common.util.Mechanics.class, remap = false)
public abstract class MechanicsMixin
{
    @ModifyArg(
        method = "tryFindCellarInfo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;inflatedBy(I)Lnet/minecraft/world/level/levelgen/structure/BoundingBox;"
        ),
        index = 0,
        require = 0
    )
    private static int flgp$increaseCellarRadius(int original)
    {
        return Math.min(128, Mth.ceil(FLConfig.SERVER.cellarRadius.get() * PatchConfig.getCellarRadiusMultiplier()));
    }

    @ModifyArg(
        method = "tryFindGreenhouseInfo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/BoundingBox;inflatedBy(I)Lnet/minecraft/world/level/levelgen/structure/BoundingBox;"
        ),
        index = 0,
        require = 0
    )
    private static int flgp$increaseGreenhouseRadius(int original)
    {
        return Math.min(128, Mth.ceil(FLConfig.SERVER.greenhouseRadius.get() * PatchConfig.getGreenhouseRadiusMultiplier()));
    }
}
