package com.g1739.firmalifegreenhousepatch.mixin;

import com.eerussianguy.firmalife.common.FLHelpers;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.blockentities.ClimateStationBlockEntity;
import com.eerussianguy.firmalife.common.util.FLAdvancements;
import com.eerussianguy.firmalife.common.util.GreenhouseType;
import com.eerussianguy.firmalife.common.util.Mechanics;
import com.g1739.firmalifegreenhousepatch.common.menu.ClimateStationTemperatureMenu;
import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import com.g1739.firmalifegreenhousepatch.common.temperature.ClimateStationAccess;
import com.g1739.firmalifegreenhousepatch.common.temperature.ClimateStationRegistry;
import com.g1739.firmalifegreenhousepatch.common.temperature.ConfiguredCellarDetector;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import com.g1739.firmalifegreenhousepatch.common.temperature.MixedGreenhouseDetector;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.Set;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = com.eerussianguy.firmalife.common.blocks.greenhouse.ClimateStationBlock.class, remap = false)
public abstract class ClimateStationBlockMixin
{
    @Inject(method = "check", at = @At("HEAD"), cancellable = true, require = 0)
    private static void flgp$replaceClimateCheck(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Either<Mechanics.GreenhouseInfo, Set<BlockPos>>> cir)
    {
        final StructureCheckResult result = flgp$checkStructure(level, pos, state);
        if (result == null)
        {
            cir.setReturnValue(null);
            return;
        }

        if (result.greenhouseResult() != null)
        {
            final GreenhouseType representativeType = result.greenhouseResult().representativeType();
            final GreenhouseType displayType = representativeType != null ? representativeType : flgp$getFallbackGreenhouseType();
            cir.setReturnValue(displayType != null
                ? Either.left(new Mechanics.GreenhouseInfo(displayType, result.greenhouseResult().positions()))
                : null);
            return;
        }

        cir.setReturnValue(Either.right(result.cellarPositions()));
    }

    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void flgp$openTemperatureMenu(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir)
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
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void flgp$useMixedClimateCheck(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir)
    {
        if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty())
        {
            return;
        }

        final boolean willConsumeAction = level.getBlockEntity(pos) instanceof ClimateStationBlockEntity station && station.setFavorite(player.getItemInHand(hand));
        final StructureCheckResult result = flgp$checkStructure(level, pos, state);
        if (result == null)
        {
            cir.setReturnValue(willConsumeAction ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS);
            return;
        }

        if (!level.isClientSide())
        {
            if (result.greenhouseResult() != null)
            {
                final MixedGreenhouseDetector.Result greenhouse = result.greenhouseResult();
                if (greenhouse.positions().size() > 200 && player instanceof ServerPlayer server && greenhouse.isRepresentativeStainless())
                {
                    FLAdvancements.BIG_STAINLESS_GREENHOUSE.trigger(server);
                }
                player.displayClientMessage(Component.translatable("firmalife.greenhouse.found", greenhouse.foundTitle(), greenhouse.positions().size()), true);
            }
            else if (result.cellarPositions() != null)
            {
                if (result.cellarPositions().size() > 200 && player instanceof ServerPlayer server)
                {
                    FLAdvancements.BIG_CELLAR.trigger(server);
                }
                player.displayClientMessage(Component.translatable("firmalife.cellar.found", result.cellarPositions().size()), true);
            }
        }

        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }

    @Inject(method = "addHoeOverlayInfo", at = @At("TAIL"))
    private void flgp$addTemperatureTooltip(Level level, BlockPos pos, BlockState state, List<Component> tooltip, boolean debug, CallbackInfo ci)
    {
        if (level.getBlockEntity(pos) instanceof ClimateStationBlockEntity blockEntity && blockEntity instanceof ClimateStationAccess station)
        {
            if (!ClimateStationRegistry.isActiveStation(blockEntity, station))
            {
                tooltip.add(Component.translatable("screen.firmalife_greenhouse_patch.inactive"));
                return;
            }
            tooltip.add(Component.translatable("firmalife_greenhouse_patch.tooltip.current_effective_temperature", station.flgp$getTargetTemperature()));
            if (station.flgp$getClimateType() == ClimateType.CELLAR)
            {
                tooltip.add(Component.translatable(
                    "firmalife_greenhouse_patch.tooltip.current_preservation",
                    GreenhouseTemperatureHelper.formatFactor(CellarPreservationHelper.getCellarPreservationMultiplier(level, pos)),
                    CellarPreservationHelper.getCellarDecayPercent(level, pos)
                ));
            }
            tooltip.add(Component.translatable(
                "firmalife_greenhouse_patch.tooltip.temperature_range",
                GreenhouseTemperatureHelper.getAmbientTemperature(level, pos),
                GreenhouseTemperatureHelper.getMinAllowedTemperature(level, pos, station),
                GreenhouseTemperatureHelper.getMaxAllowedTemperature(level, pos, station)
            ));
            tooltip.add(Component.translatable(
                "firmalife_greenhouse_patch.tooltip.heating_items",
                GreenhouseTemperatureHelper.getDisplayedMaxTemperatureLimit(station)
            ));
            tooltip.add(Component.translatable(
                "firmalife_greenhouse_patch.tooltip.cooling_items",
                GreenhouseTemperatureHelper.getDisplayedMinTemperatureLimit(station)
            ));
            tooltip.add(Component.translatable("firmalife_greenhouse_patch.tooltip.open_menu"));
        }
    }

    @Inject(method = {"onRemove", "m_6810_"}, at = @At("HEAD"), require = 0)
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

    @Unique
    @Nullable
    private static StructureCheckResult flgp$checkStructure(Level level, BlockPos pos, BlockState state)
    {
        final MixedGreenhouseDetector.Result greenhouse = MixedGreenhouseDetector.detect(level, pos);
        if (greenhouse != null)
        {
            if (level.getBlockEntity(pos) instanceof ClimateStationBlockEntity station)
            {
                if (station instanceof ClimateStationAccess access)
                {
                    access.flgp$setGreenhouseStructureData(greenhouse.structureData());
                    if (greenhouse.representativeType() == null)
                    {
                        access.flgp$clearFavoriteClimateHints();
                    }
                }
                if (greenhouse.representativeType() != null)
                {
                    station.setFavorite(greenhouse.representativeType());
                }
                station.setPositions(greenhouse.positions());
                station.updateValidity(true, greenhouse.tier());
                station.setType(ClimateType.GREENHOUSE);
            }
            flgp$updateState(level, pos, state, true);
            return new StructureCheckResult(greenhouse, null);
        }

        final Set<BlockPos> cellarPositions = ConfiguredCellarDetector.detect(level, pos);
        if (cellarPositions != null)
        {
            if (level.getBlockEntity(pos) instanceof ClimateStationBlockEntity station)
            {
                if (station instanceof ClimateStationAccess access)
                {
                    access.flgp$setGreenhouseStructureData(null);
                }
                station.setPositions(cellarPositions);
                station.updateValidity(true, 0);
                station.setType(ClimateType.CELLAR);
            }
            flgp$updateState(level, pos, state, true);
            return new StructureCheckResult(null, cellarPositions);
        }

        flgp$denyAll(level, pos);
        flgp$updateState(level, pos, state, false);
        return null;
    }

    @Unique
    private static void flgp$denyAll(Level level, BlockPos pos)
    {
        if (level.getBlockEntity(pos) instanceof ClimateStationBlockEntity station)
        {
            if (station instanceof ClimateStationAccess access)
            {
                access.flgp$setGreenhouseStructureData(null);
            }
            station.updateValidity(false, 0);
        }
    }

    @Unique
    private static void flgp$updateState(Level level, BlockPos pos, BlockState state, boolean valid)
    {
        final Boolean currentValue = state.getOptionalValue(com.eerussianguy.firmalife.common.blocks.greenhouse.ClimateStationBlock.STASIS).orElse(false);
        if (currentValue != valid)
        {
            level.setBlockAndUpdate(pos, state.setValue(com.eerussianguy.firmalife.common.blocks.greenhouse.ClimateStationBlock.STASIS, valid));
        }
    }

    @Unique
    @Nullable
    private static GreenhouseType flgp$getFallbackGreenhouseType()
    {
        GreenhouseType type = GreenhouseType.get(new ResourceLocation("firmalife", "treated_wood"));
        if (type != null)
        {
            return type;
        }
        for (GreenhouseType candidate : GreenhouseType.MANAGER.getValues())
        {
            return candidate;
        }
        return null;
    }

    @Unique
    private record StructureCheckResult(@Nullable MixedGreenhouseDetector.Result greenhouseResult, @Nullable Set<BlockPos> cellarPositions) {}
}
