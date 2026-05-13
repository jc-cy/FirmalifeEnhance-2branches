package com.g1739.firmalifegreenhousepatch.mixin;

import com.eerussianguy.firmalife.config.FLConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(com.eerussianguy.firmalife.common.util.Mechanics.class)
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
        return Math.min(128, FLConfig.SERVER.cellarRadius.get() * 2);
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
        return Math.min(128, FLConfig.SERVER.greenhouseRadius.get() * 2);
    }
}
