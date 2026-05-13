package com.g1739.firmalifegreenhousepatch.mixin;

import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.blockentities.ClimateStationBlockEntity;
import com.g1739.firmalifegreenhousepatch.common.menu.ClimateStationTemperatureMenu;
import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import com.g1739.firmalifegreenhousepatch.common.temperature.ClimateStationAccess;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import java.util.Locale;
import java.util.function.Consumer;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(com.eerussianguy.firmalife.common.blocks.greenhouse.ClimateStationBlock.class)
public abstract class ClimateStationBlockMixin
{
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void flgp$openTemperatureMenu(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir)
    {
        if (!player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty())
        {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof ClimateStationBlockEntity))
        {
            return;
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer)
        {
            ClimateStationTemperatureMenu.open(serverPlayer, pos);
        }
        cir.setReturnValue(ItemInteractionResult.sidedSuccess(level.isClientSide));
    }

    @Inject(method = "addHoeOverlayInfo", at = @At("TAIL"))
    private void flgp$addTemperatureTooltip(Level level, BlockPos pos, BlockState state, Consumer<Component> tooltip, boolean debug, CallbackInfo ci)
    {
        if (level.getBlockEntity(pos) instanceof ClimateStationAccess station)
        {
            tooltip.accept(Component.translatable("firmalife_greenhouse_patch.tooltip.current_effective_temperature", station.flgp$getTargetTemperature()));
            if (station.flgp$getClimateType() == ClimateType.CELLAR)
            {
                tooltip.accept(Component.translatable(
                    "firmalife_greenhouse_patch.tooltip.current_preservation",
                    flgp$formatFactor(CellarPreservationHelper.getCellarPreservationMultiplier(level, pos)),
                    CellarPreservationHelper.getCellarDecayPercent(level, pos)
                ));
            }
            tooltip.accept(Component.translatable(
                "firmalife_greenhouse_patch.tooltip.temperature_range",
                GreenhouseTemperatureHelper.getAmbientTemperature(level, pos),
                GreenhouseTemperatureHelper.getMinAllowedTemperature(level, pos, station),
                GreenhouseTemperatureHelper.getMaxAllowedTemperature(level, pos, station)
            ));
            tooltip.accept(Component.translatable("firmalife_greenhouse_patch.tooltip.heating_items"));
            tooltip.accept(Component.translatable("firmalife_greenhouse_patch.tooltip.cooling_items"));
            tooltip.accept(Component.translatable("firmalife_greenhouse_patch.tooltip.open_menu"));
        }
    }

    @Inject(method = "onRemove", at = @At("HEAD"))
    private void flgp$dropThermalInventory(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving, CallbackInfo ci)
    {
        if (state.is(newState.getBlock()) || level.isClientSide())
        {
            return;
        }
        if (level.getBlockEntity(pos) instanceof ClimateStationAccess station)
        {
            for (int slot = 0; slot < station.flgp$getThermalInventory().getSlots(); slot++)
            {
                final ItemStack stack = station.flgp$getThermalInventory().getStackInSlot(slot);
                if (!stack.isEmpty())
                {
                    Helpers.spawnItem(level, pos, stack.copy());
                    station.flgp$getThermalInventory().setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    private static String flgp$formatFactor(float factor)
    {
        if (Float.isInfinite(factor))
        {
            return "\u221e";
        }
        if (Math.abs(factor - Math.round(factor)) < 0.0001f)
        {
            return Integer.toString(Math.round(factor));
        }
        if (Math.abs(factor * 10f - Math.round(factor * 10f)) < 0.0001f)
        {
            return String.format(Locale.ROOT, "%.1f", factor);
        }
        return String.format(Locale.ROOT, "%.2f", factor);
    }
}
