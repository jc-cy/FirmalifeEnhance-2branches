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
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.capabilities.VesselLike;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.common.capabilities.food.FoodTraits;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public final class CellarPreservationHelper
{
    /**
     * 总保鲜倍率上限：地窖档位与其它来源（例如 TFC 自带的 PRESERVED）相乘后不得超过该值，
     * 超过时自动降档。与二代 `群峦现代化生活` 的口径一致。
     */
    private static final float MAX_TOTAL_PRESERVATION_MULTIPLIER = 10f;

    private static final Set<Object> SYNCING = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static final String IE_WOODEN_CRATE_BLOCK_ENTITY = "blusunrize.immersiveengineering.common.blocks.wooden.WoodenCrateBlockEntity";

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
        else if (target instanceof ChestBlockEntity chest)
        {
            syncChestBlockEntity(chest);
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
        final @Nullable FoodTrait trait = station != null ? getCellarTrait(level, inventory.getBlockPos()) : null;
        syncInventoryBlockEntity(inventory, trait);
    }

    public static void syncChestBlockEntity(ChestBlockEntity chest)
    {
        final Level level = chest.getLevel();
        if (level == null || level.isClientSide())
        {
            return;
        }

        final ClimateStationAccess station = ClimateStationRegistry.findControllingCellarStation(level, chest.getBlockPos());
        final @Nullable FoodTrait trait = station != null ? getCellarTrait(level, chest.getBlockPos()) : null;
        syncChestBlockEntity(chest, trait);
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

        final ClimateStationAccess station = ClimateStationRegistry.findControllingCellarStation(level, shelf.getBlockPos());
        final @Nullable FoodTrait trait = preserved && station != null ? getCellarTrait(level, shelf.getBlockPos()) : null;
        syncFoodShelfBlockEntity(shelf, trait);
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
        final @Nullable FoodTrait trait = station != null ? getCellarTrait(level, blockEntity.getBlockPos()) : null;
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

    public static boolean isActiveCellarChest(BlockEntity chest)
    {
        final Level level = chest.getLevel();
        return level != null && !level.isClientSide()
            && ClimateStationRegistry.findControllingCellarStation(level, chest.getBlockPos()) != null;
    }

    /**
     * 自动化（漏斗 / 管道）把物品放进地窖箱子前，套上当前档位。
     */
    public static ItemStack tagInsertedStack(BlockEntity chest, ItemStack stack)
    {
        if (!stack.isEmpty() && isActiveCellarChest(chest))
        {
            normalizeCellarTraits(stack, getCellarTrait(chest.getLevel(), chest.getBlockPos()));
        }
        return stack;
    }

    /**
     * 从地窖箱子取出的物品剥掉档位，避免保鲜倍率跟着离开地窖。
     */
    public static ItemStack sanitizeExtractedStack(BlockEntity chest, ItemStack stack)
    {
        if (!stack.isEmpty() && isActiveCellarChest(chest))
        {
            removeCellarTraits(stack);
        }
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
            markChanged(inventory);
        }
    }

    public static void sanitizeChestForDrop(ChestBlockEntity chest)
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
        return wrapCellarHandler(inventory, handler);
    }

    /**
     * 原版箱子与 IE 储物箱的物品能力包装入口：管道 / 自动化插入带档位、取出剥档位。
     */
    public static IItemHandler wrapBlockEntityItemHandler(BlockEntity owner, @Nullable IItemHandler handler)
    {
        if (handler == null || !canWrapBlockEntityItemHandler(owner))
        {
            return handler;
        }
        return wrapCellarHandler(owner, handler);
    }

    public static boolean canWrapBlockEntityItemHandler(BlockEntity owner)
    {
        return owner instanceof ChestBlockEntity || shouldHandleExternalContainer(owner);
    }

    private static IItemHandler wrapCellarHandler(BlockEntity owner, IItemHandler handler)
    {
        if (handler instanceof CellarInventoryWrapper || handler instanceof CellarInventoryModifiableWrapper)
        {
            return handler;
        }
        return handler instanceof IItemHandlerModifiable modifiable
            ? new CellarInventoryModifiableWrapper(owner, modifiable)
            : new CellarInventoryWrapper(owner, handler);
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

    private static boolean shouldHandleExternalContainer(BlockEntity blockEntity)
    {
        return IE_WOODEN_CRATE_BLOCK_ENTITY.equals(blockEntity.getClass().getName());
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

    private static void syncChestBlockEntity(ChestBlockEntity chest, @Nullable FoodTrait trait)
    {
        syncContainer(chest, chest, trait);
        final var state = chest.getBlockState();
        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)
        {
            final BlockEntity partner = chest.getLevel().getBlockEntity(chest.getBlockPos().relative(ChestBlock.getConnectedDirection(state)));
            if (partner instanceof ChestBlockEntity other)
            {
                syncContainer(other, other, trait);
            }
        }
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
        else if (target instanceof ChestBlockEntity chest)
        {
            syncChestBlockEntity(chest, trait);
        }
        else if (target instanceof Container container && shouldHandleExternalContainer(target))
        {
            syncContainer(target, container, trait);
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

        if (changed)
        {
            markChanged(owner);
        }
    }

    /**
     * 内容改写后通知客户端：TFC 方块实体走 markForSync（内部会 setChanged），原版箱子这类只有 setChanged。
     */
    private static void markChanged(Object owner)
    {
        if (owner instanceof net.dries007.tfc.common.blockentities.TFCBlockEntity tfc)
        {
            tfc.markForSync();
        }
        else if (owner instanceof BlockEntity blockEntity)
        {
            blockEntity.setChanged();
        }
    }

    private static boolean normalizeCellarTraits(ItemStack stack, @Nullable FoodTrait trait)
    {
        return normalizeStackAndNestedContainers(stack, trait, 0);
    }

    private static boolean normalizeStackAndNestedContainers(ItemStack stack, @Nullable FoodTrait trait, int depth)
    {
        if (stack.isEmpty())
        {
            return false;
        }

        boolean changed = normalizeFoodStack(stack, trait);
        if (depth < 2 && isNestedItemContainer(stack))
        {
            changed |= normalizeSmallVesselContents(stack, trait, depth + 1);
        }
        return changed;
    }

    private static boolean normalizeFoodStack(ItemStack stack, @Nullable FoodTrait trait)
    {
        if (FoodCapability.get(stack) == null)
        {
            return false;
        }

        final @Nullable FoodTrait effectiveTrait = getEffectiveCellarTrait(stack, trait);
        boolean changed = false;
        for (FoodTrait possible : getManagedCellarTraits())
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
     * 嵌套容器保鲜：地窖容器里再放可装物品的容器（小缸等）时递归处理其中的食物，
     * 避免"外层容器保鲜了、内层食物却不算数"。
     */
    private static boolean normalizeSmallVesselContents(ItemStack stack, @Nullable FoodTrait trait, int depth)
    {
        final @Nullable VesselLike vessel = VesselLike.get(stack);
        if (vessel == null || vessel.mode() != VesselLike.Mode.INVENTORY)
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
        final @Nullable VesselLike vessel = VesselLike.get(stack);
        return vessel != null && vessel.mode() == VesselLike.Mode.INVENTORY;
    }

    /**
     * 按"总倍率不超过 {@link #MAX_TOTAL_PRESERVATION_MULTIPLIER}"折算实际应生效的地窖档位。
     */
    @Nullable
    private static FoodTrait getEffectiveCellarTrait(ItemStack stack, @Nullable FoodTrait trait)
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
        final float desiredMultiplier = Math.min(getTraitMultiplier(trait), allowedCellarMultiplier);
        return getCellarTraitAtMost(desiredMultiplier);
    }

    private static float getExistingContainerPreservationMultiplier(ItemStack stack)
    {
        return FoodCapability.hasTrait(stack, FoodTraits.PRESERVED) ? getTraitMultiplier(FoodTraits.PRESERVED) : 1f;
    }

    private static float getTraitMultiplier(FoodTrait trait)
    {
        final float decayModifier = trait.getDecayModifier();
        return decayModifier <= 0f ? Float.POSITIVE_INFINITY : 1f / decayModifier;
    }

    private static FoodTrait getCellarTraitAtMost(float multiplier)
    {
        for (FoodTrait candidate : ModFoodTraits.getCellarTraits())
        {
            if (getTraitMultiplier(candidate) <= multiplier)
            {
                return candidate;
            }
        }
        return ModFoodTraits.getDefaultCellarTrait();
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

    private record CellarInventoryWrapper(BlockEntity owner, IItemHandler delegate) implements IItemHandler
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

    private record CellarInventoryModifiableWrapper(BlockEntity owner, IItemHandlerModifiable delegate) implements IItemHandlerModifiable
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
