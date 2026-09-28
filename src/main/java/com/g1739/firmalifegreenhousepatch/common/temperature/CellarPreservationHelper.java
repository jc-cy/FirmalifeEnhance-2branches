package com.g1739.firmalifegreenhousepatch.common.temperature;

import com.eerussianguy.firmalife.common.blockentities.ClimateReceiver;
import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.blockentities.FoodShelfBlockEntity;
import com.eerussianguy.firmalife.common.items.FLFoodTraits;
import com.g1739.firmalifegreenhousepatch.common.ModFoodTraits;
import com.g1739.firmalifegreenhousepatch.common.config.PatchConfig;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.List;
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
import net.dries007.tfc.common.component.food.FoodTraits;
import net.dries007.tfc.common.component.mold.Vessel;
import net.dries007.tfc.util.climate.Climate;

public final class CellarPreservationHelper
{
    /**
     * 总保鲜倍率上限：地窖档位与其它来源（例如 TFC 自带的 PRESERVED）相乘后不得超过该值，
     * 超过时自动降档。与二代 `群峦现代化生活` 的口径一致。
     */
    private static final float MAX_TOTAL_PRESERVATION_MULTIPLIER = 10f;

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
    private static final String IE_WOODEN_CRATE_BLOCK_ENTITY = "blusunrize.immersiveengineering.common.blocks.wooden.WoodenCrateBlockEntity";

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
        else if (target instanceof Container container && shouldHandleExternalContainer(target))
        {
            syncContainerBlockEntity(target, container);
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

    /**
     * 食物架 / 吊架的保鲜档位归一化。Firmalife 上游的 {@code updatePreservation} 在
     * {@code preserved == true} 时只 {@code applyTrait(新档位)}、不删除旧档位，
     * 温度档位变化后会叠加（例如同时挂 10x 与 7x，保鲜时间被成倍拉长）。
     * 这里整体接管该方法：先清掉其它托管档位，再写入当前应生效的档位。
     */
    public static void syncFoodShelfBlockEntity(FoodShelfBlockEntity shelf, boolean preserved)
    {
        final Level level = shelf.getLevel();
        if (level == null || level.isClientSide())
        {
            return;
        }

        syncFoodShelfBlockEntity(shelf, preserved ? getCellarTrait(level, shelf.getBlockPos()) : null);
    }

    private static void syncFoodShelfBlockEntity(FoodShelfBlockEntity shelf, @Nullable Holder<FoodTrait> trait)
    {
        if (!SYNCING.add(shelf))
        {
            return;
        }

        boolean changed = false;
        try
        {
            final var internal = shelf.getInventory();
            for (int slot = 0; slot < internal.getSlots(); slot++)
            {
                changed |= normalizeCellarTraits(internal.getStackInSlot(slot), trait);
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

    public static void syncExternalContainer(Container container)
    {
        if (container instanceof BlockEntity blockEntity && shouldHandleExternalContainer(blockEntity))
        {
            syncContainerBlockEntity(blockEntity, container);
        }
    }

    public static void syncContainerBlockEntity(BlockEntity blockEntity, Container container)
    {
        final Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide())
        {
            return;
        }

        final ClimateStationAccess station = ClimateStationRegistry.findControllingCellarStation(level, blockEntity.getBlockPos());
        final @Nullable Holder<FoodTrait> trait = station != null ? getCellarTrait(level, blockEntity.getBlockPos()) : null;
        syncContainer(blockEntity, container, trait);
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

    public static void sanitizeContainerBlockEntityForDrop(BlockEntity blockEntity, Container container)
    {
        sanitizeContainerForDrop(blockEntity, container);
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

    private static boolean shouldHandleExternalContainer(BlockEntity blockEntity)
    {
        return IE_WOODEN_CRATE_BLOCK_ENTITY.equals(blockEntity.getClass().getName());
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
        else if (target instanceof Container container && shouldHandleExternalContainer(target))
        {
            syncContainer(target, container, trait);
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
        return normalizeStackAndNestedContainers(stack, trait, 0);
    }

    private static boolean normalizeStackAndNestedContainers(ItemStack stack, @Nullable Holder<FoodTrait> trait, int depth)
    {
        if (stack.isEmpty())
        {
            return false;
        }

        boolean changed = normalizeFoodStack(stack, trait);
        if (depth < 2 && isNestedItemContainer(stack))
        {
            changed |= normalizeNestedContainerContents(stack, trait, depth + 1);
        }
        return changed;
    }

    private static boolean normalizeFoodStack(ItemStack stack, @Nullable Holder<FoodTrait> trait)
    {
        if (FoodCapability.get(stack) == null)
        {
            return false;
        }

        final @Nullable Holder<FoodTrait> effectiveTrait = getEffectiveCellarTrait(stack, trait);
        boolean changed = false;
        for (Holder<FoodTrait> possible : CELLAR_TRAITS)
        {
            if (effectiveTrait != possible && FoodCapability.hasTrait(stack, possible))
            {
                FoodCapability.removeTrait(stack, possible);
                changed = true;
            }
        }

        if (effectiveTrait != null && !FoodCapability.hasTrait(stack, effectiveTrait))
        {
            FoodCapability.applyTrait(stack, effectiveTrait);
            changed = true;
        }
        return changed;
    }

    /**
     * 嵌套容器保鲜：地窖里的箱子里再放可装物品的容器（缸、罐之类）时，
     * 递归处理其中的食物，避免"外层容器保鲜了、内层食物却不算数"。
     */
    private static boolean normalizeNestedContainerContents(ItemStack stack, @Nullable Holder<FoodTrait> trait, int depth)
    {
        final @Nullable Vessel vessel = Vessel.get(stack);
        if (vessel == null || !vessel.isInventory())
        {
            return false;
        }

        boolean changed = false;
        for (int slot = 0; slot < vessel.getSlots(); slot++)
        {
            final ItemStack contained = vessel.getStackInSlot(slot);
            if (contained.isEmpty())
            {
                continue;
            }
            if (normalizeStackAndNestedContainers(contained, trait, depth))
            {
                vessel.setStackInSlot(slot, contained);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isNestedItemContainer(ItemStack stack)
    {
        final @Nullable Vessel vessel = Vessel.get(stack);
        return vessel != null && vessel.isInventory();
    }

    /**
     * 按"总倍率不超过 {@link #MAX_TOTAL_PRESERVATION_MULTIPLIER}"折算实际应生效的地窖档位。
     */
    @Nullable
    private static Holder<FoodTrait> getEffectiveCellarTrait(ItemStack stack, @Nullable Holder<FoodTrait> trait)
    {
        if (trait == null)
        {
            return null;
        }
        final float existingMultiplier = getExistingContainerPreservationMultiplier(stack);
        if (!Float.isFinite(existingMultiplier) || existingMultiplier >= MAX_TOTAL_PRESERVATION_MULTIPLIER)
        {
            return null;
        }
        final float allowedCellarMultiplier = MAX_TOTAL_PRESERVATION_MULTIPLIER / existingMultiplier;
        final float desiredMultiplier = Math.min(getTraitMultiplier(trait.value()), allowedCellarMultiplier);
        return getCellarTraitAtMost(desiredMultiplier);
    }

    private static float getExistingContainerPreservationMultiplier(ItemStack stack)
    {
        return FoodCapability.hasTrait(stack, FoodTraits.PRESERVED) ? getTraitMultiplier(FoodTraits.PRESERVED.value()) : 1f;
    }

    private static float getTraitMultiplier(FoodTrait trait)
    {
        final float decayModifier = trait.getDecayModifier();
        return decayModifier <= 0f ? Float.POSITIVE_INFINITY : 1f / decayModifier;
    }

    private static Holder<FoodTrait> getCellarTraitAtMost(float multiplier)
    {
        for (DeferredHolder<FoodTrait, FoodTrait> candidate : ModFoodTraits.getCellarTraits())
        {
            if (getTraitMultiplier(candidate.value()) <= multiplier)
            {
                return candidate;
            }
        }
        return ModFoodTraits.getDefaultCellarTrait();
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
