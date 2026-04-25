package com.g1739.firmalifegreenhousepatch.common.temperature;

import com.eerussianguy.firmalife.common.blockentities.FoodShelfBlockEntity;
import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.items.FLFoodTraits;
import com.g1739.firmalifegreenhousepatch.common.ModFoodTraits;
import com.g1739.firmalifegreenhousepatch.common.config.PatchConfig;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.blockentities.TFCChestBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public final class CellarPreservationHelper
{
    private static final Set<Object> SYNCING = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private CellarPreservationHelper() {}

    public static FoodTrait[] getPossibleTraits()
    {
        return ModFoodTraits.getCellarTraits().toArray(FoodTrait[]::new);
    }

    public static void syncTrackedInventories(ClimateStationAccess station)
    {
        final boolean active = station instanceof BlockEntity blockEntity && ClimateStationRegistry.isActiveStation(blockEntity, station);
        syncTrackedInventories(station, active);
    }

    public static void syncTrackedInventories(ClimateStationAccess station, boolean active)
    {
        if (!(station instanceof BlockEntity blockEntity))
        {
            return;
        }
        final Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide() || station.flgp$getClimateType() != ClimateType.CELLAR)
        {
            return;
        }

        final @Nullable FoodTrait trait = active ? getCellarTrait(level, blockEntity.getBlockPos()) : null;
        for (BlockPos pos : station.flgp$getGreenhousePositions())
        {
            syncBlockEntity(level, pos, trait);
        }
    }

    public static void syncBlockEntities(Level level, Iterable<BlockPos> positions)
    {
        if (level.isClientSide())
        {
            return;
        }

        final Set<BlockPos> uniquePositions = new HashSet<>();
        for (BlockPos pos : positions)
        {
            if (uniquePositions.add(pos))
            {
                syncBlockEntity(level, pos);
            }
        }
    }

    public static void syncBlockEntity(Level level, BlockPos pos)
    {
        if (level.isClientSide())
        {
            return;
        }

        final BlockEntity target = level.getBlockEntity(pos);
        if (target instanceof FoodShelfBlockEntity shelf)
        {
            syncFoodShelfBlockEntity(shelf);
        }
        else if (target instanceof InventoryBlockEntity<?> inventory && shouldHandleInventory(inventory))
        {
            syncInventoryBlockEntity(inventory);
        }
        else if (target instanceof TFCChestBlockEntity chest)
        {
            syncTFCChestBlockEntity(chest);
        }
    }

    public static void syncInventoryBlockEntity(InventoryBlockEntity<?> inventory)
    {
        if (!shouldHandleInventory(inventory))
        {
            return;
        }

        final Level level = inventory.getLevel();
        if (level == null || level.isClientSide())
        {
            return;
        }

        final ClimateStationAccess station = ClimateStationRegistry.findControllingCellarStation(level, inventory.getBlockPos());
        final @Nullable FoodTrait trait = station != null ? getCellarTrait(level, inventory.getBlockPos()) : null;
        syncInventoryBlockEntity(inventory, trait);
    }

    public static void syncTFCChestBlockEntity(TFCChestBlockEntity chest)
    {
        final Level level = chest.getLevel();
        if (level == null || level.isClientSide())
        {
            return;
        }

        final ClimateStationAccess station = ClimateStationRegistry.findControllingCellarStation(level, chest.getBlockPos());
        final @Nullable FoodTrait trait = station != null ? getCellarTrait(level, chest.getBlockPos()) : null;
        syncTFCChestBlockEntity(chest, trait);
    }

    public static void syncFoodShelfBlockEntity(FoodShelfBlockEntity shelf)
    {
        final Level level = shelf.getLevel();
        if (level == null || level.isClientSide())
        {
            return;
        }

        final ClimateStationAccess station = ClimateStationRegistry.findControllingCellarStation(level, shelf.getBlockPos());
        final @Nullable FoodTrait trait = station != null ? getCellarTrait(level, shelf.getBlockPos()) : null;
        syncFoodShelfBlockEntity(shelf, trait);
    }

    public static void syncFoodShelfBlockEntity(FoodShelfBlockEntity shelf, boolean preserved)
    {
        final Level level = shelf.getLevel();
        if (level == null || level.isClientSide())
        {
            return;
        }

        final @Nullable FoodTrait trait = preserved ? getCellarTrait(level, shelf.getBlockPos()) : null;
        syncFoodShelfBlockEntity(shelf, trait);
    }

    public static ItemStack sanitizeTakenStack(InventoryBlockEntity<?> inventory, ItemStack stack)
    {
        if (!shouldHandleInventory(inventory) || stack.isEmpty())
        {
            return stack;
        }
        sanitizeTakenStack(stack);
        return stack;
    }

    public static ItemStack sanitizeTakenStack(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return stack;
        }
        removeCellarTraits(stack);
        return stack;
    }

    public static void sanitizeInventoryForDrop(InventoryBlockEntity<?> inventory)
    {
        if (!shouldHandleInventory(inventory))
        {
            return;
        }

        boolean changed = false;
        final IItemHandlerModifiable internal = flgp$getInventoryHandler(inventory);
        if (internal == null)
        {
            return;
        }
        for (int slot = 0; slot < internal.getSlots(); slot++)
        {
            final ItemStack stack = internal.getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            boolean hadTrait = false;
            for (FoodTrait trait : getManagedCellarTraits())
            {
                hadTrait |= FoodCapability.hasTrait(stack, trait);
            }
            removeCellarTraits(stack);
            changed |= hadTrait;
        }

        if (changed)
        {
            inventory.setChanged();
        }
    }

    public static void sanitizeChestForDrop(TFCChestBlockEntity chest)
    {
        sanitizeContainerForDrop(chest, chest);
    }

    public static IItemHandler wrapSidedInventory(InventoryBlockEntity<?> inventory, @Nullable IItemHandler handler)
    {
        if (handler == null || !shouldHandleInventory(inventory))
        {
            return handler;
        }
        if (handler instanceof CellarInventoryWrapper || handler instanceof CellarInventoryModifiableWrapper)
        {
            return handler;
        }
        return handler instanceof IItemHandlerModifiable modifiable
            ? new CellarInventoryModifiableWrapper(inventory, modifiable)
            : new CellarInventoryWrapper(inventory, handler);
    }

    public static FoodTrait getCellarTrait(Level level, BlockPos pos)
    {
        final float temp = Math.max(
            GreenhouseTemperatureHelper.getMinimumCellarTemperature(),
            GreenhouseTemperatureHelper.getControlledTemperature(level, pos, ClimateType.CELLAR, Climate.getAverageTemperature(level, pos))
        );
        final List<PatchConfig.CellarLevel> levels = PatchConfig.getCellarPreservationLevels();
        for (int index = 0; index < levels.size(); index++)
        {
            if (temp <= levels.get(index).maxTemperature())
            {
                return ModFoodTraits.getCellarTrait(index);
            }
        }
        return ModFoodTraits.getDefaultCellarTrait();
    }

    public static float getCellarDecayModifier(Level level, BlockPos pos)
    {
        return getCellarTrait(level, pos).getDecayModifier();
    }

    public static float getCellarPreservationMultiplier(Level level, BlockPos pos)
    {
        final float decayModifier = getCellarDecayModifier(level, pos);
        return decayModifier <= 0f ? Float.POSITIVE_INFINITY : 1f / decayModifier;
    }

    public static int getCellarDecayPercent(Level level, BlockPos pos)
    {
        final float decayModifier = getCellarDecayModifier(level, pos);
        return decayModifier <= 0f ? 0 : Math.round(decayModifier * 100f);
    }

    public static boolean shouldHandleInventory(InventoryBlockEntity<?> inventory)
    {
        return !(inventory instanceof ClimateReceiver) && !(inventory instanceof ClimateStationAccess);
    }

    private static void syncInventoryBlockEntity(InventoryBlockEntity<?> inventory, @Nullable FoodTrait trait)
    {
        if (!SYNCING.add(inventory))
        {
            return;
        }

        boolean changed = false;
        try
        {
            final IItemHandlerModifiable internal = flgp$getInventoryHandler(inventory);
            if (internal == null)
            {
                return;
            }
            for (int slot = 0; slot < internal.getSlots(); slot++)
            {
                final ItemStack stack = internal.getStackInSlot(slot);
                if (stack.isEmpty())
                {
                    continue;
                }
                changed |= normalizeCellarTraits(stack, trait);
            }
        }
        finally
        {
            SYNCING.remove(inventory);
        }

        if (changed)
        {
            inventory.setChanged();
        }
    }

    private static void syncTFCChestBlockEntity(TFCChestBlockEntity chest, @Nullable FoodTrait trait)
    {
        syncContainer(chest, chest, trait);
    }

    private static void syncBlockEntity(Level level, BlockPos pos, @Nullable FoodTrait trait)
    {
        final BlockEntity target = level.getBlockEntity(pos);
        if (target instanceof FoodShelfBlockEntity shelf)
        {
            syncFoodShelfBlockEntity(shelf, trait);
        }
        else if (target instanceof InventoryBlockEntity<?> inventory && shouldHandleInventory(inventory))
        {
            syncInventoryBlockEntity(inventory, trait);
        }
        else if (target instanceof TFCChestBlockEntity chest)
        {
            syncTFCChestBlockEntity(chest, trait);
        }
    }

    private static void syncFoodShelfBlockEntity(FoodShelfBlockEntity shelf, @Nullable FoodTrait trait)
    {
        if (!SYNCING.add(shelf))
        {
            return;
        }

        boolean changed = false;
        try
        {
            final IItemHandlerModifiable internal = flgp$getInventoryHandler(shelf);
            if (internal == null)
            {
                return;
            }
            for (int slot = 0; slot < internal.getSlots(); slot++)
            {
                final ItemStack stack = internal.getStackInSlot(slot);
                if (stack.isEmpty())
                {
                    continue;
                }
                changed |= normalizeCellarTraits(stack, trait);
            }
        }
        finally
        {
            SYNCING.remove(shelf);
        }

        if (changed)
        {
            shelf.setChanged();
            shelf.markForSync();
        }
    }

    private static void syncContainer(Object owner, Container container, @Nullable FoodTrait trait)
    {
        if (!SYNCING.add(owner))
        {
            return;
        }

        boolean changed = false;
        try
        {
            for (int slot = 0; slot < container.getContainerSize(); slot++)
            {
                final ItemStack stack = container.getItem(slot);
                if (stack.isEmpty())
                {
                    continue;
                }
                changed |= normalizeCellarTraits(stack, trait);
            }
        }
        finally
        {
            SYNCING.remove(owner);
        }

        if (changed && owner instanceof BlockEntity blockEntity)
        {
            blockEntity.setChanged();
        }
    }

    private static boolean normalizeCellarTraits(ItemStack stack, @Nullable FoodTrait trait)
    {
        if (FoodCapability.get(stack) == null)
        {
            return false;
        }

        boolean changed = false;
        for (FoodTrait possible : getManagedCellarTraits())
        {
            if (trait != possible && FoodCapability.hasTrait(stack, possible))
            {
                FoodCapability.removeTrait(stack, possible);
                changed = true;
            }
        }

        if (trait != null && !FoodCapability.hasTrait(stack, trait))
        {
            FoodCapability.applyTrait(stack, trait);
            changed = true;
        }
        return changed;
    }

    private static void removeCellarTraits(ItemStack stack)
    {
        for (FoodTrait trait : getManagedCellarTraits())
        {
            FoodCapability.removeTrait(stack, trait);
        }
    }

    private static void sanitizeContainerForDrop(Object owner, Container container)
    {
        if (!SYNCING.add(owner))
        {
            return;
        }

        boolean changed = false;
        try
        {
            for (int slot = 0; slot < container.getContainerSize(); slot++)
            {
                final ItemStack stack = container.getItem(slot);
                if (stack.isEmpty())
                {
                    continue;
                }
                final boolean hadTrait = hasCellarTrait(stack);
                removeCellarTraits(stack);
                changed |= hadTrait;
            }
        }
        finally
        {
            SYNCING.remove(owner);
        }

        if (changed && owner instanceof BlockEntity blockEntity)
        {
            blockEntity.setChanged();
        }
    }

    private static boolean hasCellarTrait(ItemStack stack)
    {
        for (FoodTrait trait : getManagedCellarTraits())
        {
            if (FoodCapability.hasTrait(stack, trait))
            {
                return true;
            }
        }
        return false;
    }

    private static @Nullable IItemHandlerModifiable flgp$getInventoryHandler(InventoryBlockEntity<?> inventory)
    {
        return inventory.getCapability(Capabilities.ITEM).resolve()
            .filter(IItemHandlerModifiable.class::isInstance)
            .map(IItemHandlerModifiable.class::cast)
            .orElse(null);
    }

    private static Set<FoodTrait> getManagedCellarTraits()
    {
        final Set<FoodTrait> traits = new LinkedHashSet<>();
        traits.add(FLFoodTraits.SHELVED);
        traits.add(FLFoodTraits.SHELVED_2);
        traits.add(FLFoodTraits.SHELVED_3);
        traits.add(FLFoodTraits.HUNG);
        traits.add(FLFoodTraits.HUNG_2);
        traits.add(FLFoodTraits.HUNG_3);
        traits.addAll(ModFoodTraits.getCellarTraits());
        return traits;
    }

    private record CellarInventoryWrapper(InventoryBlockEntity<?> owner, IItemHandler delegate) implements IItemHandler
    {
        @Override
        public int getSlots()
        {
            return delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return delegate.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            final Level level = owner.getLevel();
            if (level == null || level.isClientSide())
            {
                return delegate.insertItem(slot, stack, simulate);
            }

            final ClimateStationAccess station = ClimateStationRegistry.findControllingCellarStation(level, owner.getBlockPos());
            if (station == null || stack.isEmpty())
            {
                return delegate.insertItem(slot, stack, simulate);
            }

            final ItemStack copy = stack.copy();
            normalizeCellarTraits(copy, getCellarTrait(level, owner.getBlockPos()));
            return delegate.insertItem(slot, copy, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            final ItemStack extracted = delegate.extractItem(slot, amount, simulate);
            if (!extracted.isEmpty())
            {
                sanitizeTakenStack(extracted);
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return delegate.isItemValid(slot, stack);
        }
    }

    private record CellarInventoryModifiableWrapper(InventoryBlockEntity<?> owner, IItemHandlerModifiable delegate) implements IItemHandlerModifiable
    {
        @Override
        public int getSlots()
        {
            return delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return delegate.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            final Level level = owner.getLevel();
            if (level == null || level.isClientSide())
            {
                return delegate.insertItem(slot, stack, simulate);
            }

            final ClimateStationAccess station = ClimateStationRegistry.findControllingCellarStation(level, owner.getBlockPos());
            if (station == null || stack.isEmpty())
            {
                return delegate.insertItem(slot, stack, simulate);
            }

            final ItemStack copy = stack.copy();
            normalizeCellarTraits(copy, getCellarTrait(level, owner.getBlockPos()));
            return delegate.insertItem(slot, copy, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            final ItemStack extracted = delegate.extractItem(slot, amount, simulate);
            if (!extracted.isEmpty())
            {
                sanitizeTakenStack(extracted);
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return delegate.isItemValid(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack)
        {
            delegate.setStackInSlot(slot, stack);
        }
    }
}
