package com.g1739.firmalifegreenhousepatch.common.menu;

import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.blocks.greenhouse.ClimateStationBlock;
import com.g1739.firmalifegreenhousepatch.common.ModMenuTypes;
import com.g1739.firmalifegreenhousepatch.common.temperature.CellarPreservationHelper;
import com.g1739.firmalifegreenhousepatch.common.temperature.ClimateStationAccess;
import com.g1739.firmalifegreenhousepatch.common.temperature.ClimateStationRegistry;
import com.g1739.firmalifegreenhousepatch.common.temperature.GreenhouseTemperatureHelper;
import net.dries007.tfc.common.container.ButtonHandlerContainer;
import net.dries007.tfc.common.container.Container;
import net.dries007.tfc.common.container.ISlotCallback;
import net.dries007.tfc.common.container.slot.CallbackSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class ClimateStationTemperatureMenu extends Container implements ButtonHandlerContainer, ISlotCallback
{
    public static final int BUTTON_SET_TEMPERATURE = 0;
    public static final String TEMPERATURE_NBT_KEY = "temperature";

    public static final int HEATING_SLOT_X = 18;
    public static final int HEATING_SLOT_Y = 28;
    public static final int COOLING_SLOT_X = 142;
    public static final int COOLING_SLOT_Y = 28;

    private final Inventory playerInventory;
    private final BlockPos stationPos;
    private final IItemHandlerModifiable thermalInventory;

    public static ClimateStationTemperatureMenu create(int windowId, Inventory playerInventory, BlockPos stationPos)
    {
        return new ClimateStationTemperatureMenu(windowId, playerInventory, stationPos).init(playerInventory);
    }

    public static void open(ServerPlayer player, BlockPos stationPos)
    {
        final MenuProvider provider = new SimpleMenuProvider(
            (windowId, playerInventory, ignored) -> ClimateStationTemperatureMenu.create(windowId, playerInventory, stationPos),
            Component.translatable("screen.firmalife_greenhouse_patch.climate_station_temperature")
        );
        player.openMenu(provider, buffer -> buffer.writeBlockPos(stationPos));
    }

    private ClimateStationTemperatureMenu(int windowId, Inventory playerInventory, BlockPos stationPos)
    {
        super(ModMenuTypes.CLIMATE_STATION_TEMPERATURE.get(), windowId);
        this.playerInventory = playerInventory;
        this.stationPos = stationPos.immutable();
        this.callback = this;

        final ClimateStationAccess station = getStation();
        this.thermalInventory = station != null ? station.flgp$getThermalInventory() : new ItemStackHandler(2);
    }

    @Override
    protected void addContainerSlots()
    {
        addSlot(new CallbackSlot(this, thermalInventory, GreenhouseTemperatureHelper.HEATING_SLOT, HEATING_SLOT_X, HEATING_SLOT_Y));
        addSlot(new CallbackSlot(this, thermalInventory, GreenhouseTemperatureHelper.COOLING_SLOT, COOLING_SLOT_X, COOLING_SLOT_Y));
    }

    public BlockPos getStationPos()
    {
        return stationPos;
    }

    @Nullable
    public ClimateStationAccess getStation()
    {
        final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(stationPos);
        return blockEntity instanceof ClimateStationAccess station ? station : null;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return player.level().getBlockState(stationPos).getBlock() instanceof ClimateStationBlock
            && player.distanceToSqr(stationPos.getX() + 0.5D, stationPos.getY() + 0.5D, stationPos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    protected boolean moveStack(ItemStack stack, int slotIndex)
    {
        return switch (typeOf(slotIndex))
        {
            case CONTAINER -> !moveItemStackTo(stack, containerSlots, slots.size(), false);
            case MAIN_INVENTORY, HOTBAR -> {
                if (GreenhouseTemperatureHelper.isHeatingItem(stack))
                {
                    yield !moveItemStackTo(stack, GreenhouseTemperatureHelper.HEATING_SLOT, GreenhouseTemperatureHelper.HEATING_SLOT + 1, false);
                }
                if (GreenhouseTemperatureHelper.isCoolingItem(stack))
                {
                    yield !moveItemStackTo(stack, GreenhouseTemperatureHelper.COOLING_SLOT, GreenhouseTemperatureHelper.COOLING_SLOT + 1, false);
                }
                yield true;
            }
        };
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return switch (slot)
        {
            case GreenhouseTemperatureHelper.HEATING_SLOT -> GreenhouseTemperatureHelper.isHeatingItem(stack);
            case GreenhouseTemperatureHelper.COOLING_SLOT -> GreenhouseTemperatureHelper.isCoolingItem(stack);
            default -> false;
        };
    }

    public int getRequestedTemperature()
    {
        final ClimateStationAccess station = getStation();
        return station != null ? station.flgp$getRequestedTemperature() : GreenhouseTemperatureHelper.getDefaultTemperature();
    }

    public int getEffectiveTemperature()
    {
        final ClimateStationAccess station = getStation();
        return station != null ? station.flgp$getTargetTemperature() : GreenhouseTemperatureHelper.getDefaultTemperature();
    }

    public int getAmbientTemperature()
    {
        final ClimateStationAccess station = getStation();
        return station != null ? GreenhouseTemperatureHelper.getAmbientTemperature(playerInventory.player.level(), stationPos) : GreenhouseTemperatureHelper.getDefaultTemperature();
    }

    public int getBaseControlRange()
    {
        final ClimateStationAccess station = getStation();
        return station != null ? GreenhouseTemperatureHelper.getBaseControlRange(station) : 0;
    }

    public boolean isClimateStationActive()
    {
        final BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(stationPos);
        return blockEntity instanceof ClimateStationAccess station && ClimateStationRegistry.isActiveStation(blockEntity, station);
    }

    public int getHeatingControlRange()
    {
        final ClimateStationAccess station = getStation();
        return station != null ? GreenhouseTemperatureHelper.getHeatingControlRange(station) : 0;
    }

    public int getCoolingControlRange()
    {
        final ClimateStationAccess station = getStation();
        return station != null ? GreenhouseTemperatureHelper.getCoolingControlRange(station) : 0;
    }

    public int getHeatingItemCount()
    {
        return thermalInventory.getStackInSlot(GreenhouseTemperatureHelper.HEATING_SLOT).getCount();
    }

    public int getCoolingItemCount()
    {
        return thermalInventory.getStackInSlot(GreenhouseTemperatureHelper.COOLING_SLOT).getCount();
    }

    public float getHeatingUnitFactor()
    {
        return GreenhouseTemperatureHelper.getHeatingUnitFactor(thermalInventory.getStackInSlot(GreenhouseTemperatureHelper.HEATING_SLOT));
    }

    public float getCoolingUnitFactor()
    {
        final ItemStack stack = thermalInventory.getStackInSlot(GreenhouseTemperatureHelper.COOLING_SLOT);
        if (stack.isEmpty())
        {
            return 0f;
        }
        return GreenhouseTemperatureHelper.getCoolingUnitFactor(stack);
    }

    public boolean hasCoolingItem()
    {
        return !thermalInventory.getStackInSlot(GreenhouseTemperatureHelper.COOLING_SLOT).isEmpty();
    }

    public boolean hasHeatingItem()
    {
        return !thermalInventory.getStackInSlot(GreenhouseTemperatureHelper.HEATING_SLOT).isEmpty();
    }

    public Component getHeatingItemName()
    {
        final ItemStack stack = thermalInventory.getStackInSlot(GreenhouseTemperatureHelper.HEATING_SLOT);
        return stack.isEmpty()
            ? Component.translatable("screen.firmalife_greenhouse_patch.heating_item.none")
            : stack.getHoverName();
    }

    public Component getCoolingItemName()
    {
        final ItemStack stack = thermalInventory.getStackInSlot(GreenhouseTemperatureHelper.COOLING_SLOT);
        return stack.isEmpty()
            ? Component.translatable("screen.firmalife_greenhouse_patch.cooling_item.none")
            : stack.getHoverName();
    }

    public int getMinAllowedTemperature()
    {
        final ClimateStationAccess station = getStation();
        return station != null ? GreenhouseTemperatureHelper.getMinAllowedTemperature(playerInventory.player.level(), stationPos, station) : GreenhouseTemperatureHelper.getDefaultTemperature();
    }

    public int getMaxAllowedTemperature()
    {
        final ClimateStationAccess station = getStation();
        return station != null ? GreenhouseTemperatureHelper.getMaxAllowedTemperature(playerInventory.player.level(), stationPos, station) : GreenhouseTemperatureHelper.getDefaultTemperature();
    }

    public int clampRequestedTemperature(int temperature)
    {
        final ClimateStationAccess station = getStation();
        return station != null
            ? GreenhouseTemperatureHelper.clampRequestedTemperature(playerInventory.player.level(), stationPos, station, temperature)
            : GreenhouseTemperatureHelper.clampTemperature(temperature);
    }

    public boolean isCellarMode()
    {
        final ClimateStationAccess station = getStation();
        return station != null && station.flgp$getClimateType() == ClimateType.CELLAR;
    }

    public String getStructureNameKey()
    {
        if (isCellarMode())
        {
            return "screen.firmalife_greenhouse_patch.structure.cellar";
        }

        final int baseRange = getBaseControlRange();
        final ClimateStationAccess station = getStation();
        if (station != null && station.flgp$getGreenhouseStructureData() != null)
        {
            return station.flgp$getGreenhouseStructureData().displayNameKey();
        }
        if (baseRange >= 25)
        {
            return "screen.firmalife_greenhouse_patch.greenhouse.stainless_steel";
        }
        if (baseRange >= 20)
        {
            return "screen.firmalife_greenhouse_patch.greenhouse.steel";
        }
        if (baseRange >= 15)
        {
            return "screen.firmalife_greenhouse_patch.greenhouse.iron";
        }
        if (baseRange >= 10)
        {
            return "screen.firmalife_greenhouse_patch.greenhouse.copper";
        }
        if (baseRange >= 5)
        {
            return "screen.firmalife_greenhouse_patch.greenhouse.wood";
        }
        return "screen.firmalife_greenhouse_patch.greenhouse.unknown";
    }

    public String getStructureSummaryKey()
    {
        return isCellarMode()
            ? "screen.firmalife_greenhouse_patch.cellar_summary"
            : "screen.firmalife_greenhouse_patch.greenhouse_summary";
    }

    public float getCellarPreservationMultiplier()
    {
        return isCellarMode() ? CellarPreservationHelper.getCellarPreservationMultiplier(playerInventory.player.level(), stationPos) : 0f;
    }

    public int getCellarDecayPercent()
    {
        return isCellarMode() ? CellarPreservationHelper.getCellarDecayPercent(playerInventory.player.level(), stationPos) : 100;
    }

    @Override
    public void onButtonPress(int buttonID, @Nullable CompoundTag extraNBT)
    {
        if (buttonID != BUTTON_SET_TEMPERATURE || extraNBT == null || !extraNBT.contains(TEMPERATURE_NBT_KEY))
        {
            return;
        }

        final ClimateStationAccess station = getStation();
        if (station != null)
        {
            station.flgp$setTargetTemperature(extraNBT.getInt(TEMPERATURE_NBT_KEY));
        }
    }
}
