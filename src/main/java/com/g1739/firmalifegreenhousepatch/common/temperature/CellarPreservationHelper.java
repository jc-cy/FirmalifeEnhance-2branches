package com.g1739.firmalifegreenhousepatch.common.temperature;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.items.FLFoodTraits;
import com.g1739.firmalifegreenhousepatch.common.ModFoodTraits;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.dries007.tfc.common.blockentities.TFCChestBlockEntity;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.FoodTrait;
import net.dries007.tfc.util.climate.Climate;

public final class CellarPreservationHelper
{
    private static final Set<Holder<FoodTrait>> CELLAR_TRAITS = Set.of(
        FLFoodTraits.SHELVED,
        FLFoodTraits.SHELVED_2,
        FLFoodTraits.SHELVED_3,
        FLFoodTraits.HUNG,
        FLFoodTraits.HUNG_2,
        FLFoodTraits.HUNG_3,
        ModFoodTraits.CELLAR_2_5X,
        ModFoodTraits.CELLAR_3X,
        ModFoodTraits.CELLAR_4X,
        ModFoodTraits.CELLAR_5X,
        ModFoodTraits.CELLAR_6X,
        ModFoodTraits.CELLAR_7X,
        ModFoodTraits.CELLAR_8X
    );
    private static final Set<Object> SYNCING = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private CellarPreservationHelper() {}

    public static Set<DeferredHolder<FoodTrait, FoodTrait>> getPossibleTraits()
    {
        return ModFoodTraits.CELLAR_TRAITS;
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

        final @Nullable Holder<FoodTrait> trait = active ? getCellarTrait(level, blockEntity.getBlockPos()) : null;
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
        if (target instanceof InventoryBlockEntity<?> inventory && shouldHandleInventory(inventory))
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
        final @Nullable Holder<FoodTrait> trait = station != null ? getCellarTrait(level, inventory.getBlockPos()) : null;
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
        final @Nullable Holder<FoodTrait> trait = station != null ? getCellarTrait(level, chest.getBlockPos()) : null;
        syncTFCChestBlockEntity(chest, trait);
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
        final var internal = inventory.getInventory();
        for (int slot = 0; slot < internal.getSlots(); slot++)
        {
            final ItemStack stack = internal.getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            boolean hadTrait = false;
            for (Holder<FoodTrait> trait : CELLAR_TRAITS)
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

    public static Holder<FoodTrait> getCellarTrait(Level level, BlockPos pos)
    {
        final float temp = Math.max(
            GreenhouseTemperatureHelper.CELLAR_MIN_TEMPERATURE,
            GreenhouseTemperatureHelper.getControlledTemperature(level, pos, ClimateType.CELLAR, Climate.getAverageTemperature(level, pos))
        );
        if (temp <= -24f)
        {
            return ModFoodTraits.CELLAR_8X;
        }
        if (temp <= -21f)
        {
            return ModFoodTraits.CELLAR_7X;
        }
        if (temp <= -18f)
        {
            return ModFoodTraits.CELLAR_6X;
        }
        if (temp <= -15f)
        {
            return ModFoodTraits.CELLAR_5X;
        }
        if (temp <= -12f)
        {
            return ModFoodTraits.CELLAR_4X;
        }
        if (temp < 0f)
        {
            return ModFoodTraits.CELLAR_3X;
        }
        return ModFoodTraits.CELLAR_2_5X;
    }

    public static float getCellarDecayModifier(Level level, BlockPos pos)
    {
        return getCellarTrait(level, pos).value().getDecayModifier();
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

    private static void syncInventoryBlockEntity(InventoryBlockEntity<?> inventory, @Nullable Holder<FoodTrait> trait)
    {
        if (!SYNCING.add(inventory))
        {
            return;
        }

        boolean changed = false;
        try
        {
            final var internal = inventory.getInventory();
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

    private static void syncTFCChestBlockEntity(TFCChestBlockEntity chest, @Nullable Holder<FoodTrait> trait)
    {
        syncContainer(chest, chest, trait);
    }

    private static void syncBlockEntity(Level level, BlockPos pos, @Nullable Holder<FoodTrait> trait)
    {
        final BlockEntity target = level.getBlockEntity(pos);
        if (target instanceof InventoryBlockEntity<?> inventory && shouldHandleInventory(inventory))
        {
            syncInventoryBlockEntity(inventory, trait);
        }
        else if (target instanceof TFCChestBlockEntity chest)
        {
            syncTFCChestBlockEntity(chest, trait);
        }
    }

    private static void syncContainer(Object owner, Container container, @Nullable Holder<FoodTrait> trait)
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

    private static boolean normalizeCellarTraits(ItemStack stack, @Nullable Holder<FoodTrait> trait)
    {
        if (FoodCapability.get(stack) == null)
        {
            return false;
        }

        boolean changed = false;
        for (Holder<FoodTrait> possible : CELLAR_TRAITS)
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
        for (Holder<FoodTrait> trait : CELLAR_TRAITS)
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
        for (Holder<FoodTrait> trait : CELLAR_TRAITS)
        {
            if (FoodCapability.hasTrait(stack, trait))
            {
                return true;
            }
        }
        return false;
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
