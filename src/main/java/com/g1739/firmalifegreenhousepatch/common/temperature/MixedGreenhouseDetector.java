package com.g1739.firmalifegreenhousepatch.common.temperature;

import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.eerussianguy.firmalife.common.util.GreenhouseType;
import com.eerussianguy.firmalife.common.util.Mechanics;
import com.eerussianguy.firmalife.config.FLConfig;
import com.g1739.firmalifegreenhousepatch.common.config.PatchConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

public final class MixedGreenhouseDetector
{
    private static final String MIXED_NAME_KEY = "screen.firmalife_greenhouse_patch.greenhouse.mixed";
    private static final String CUSTOM_NAME_KEY = "screen.firmalife_greenhouse_patch.greenhouse.custom";
    private static final String MIXED_FOUND_KEY = "greenhouse.firmalife_greenhouse_patch.mixed";
    private static final String CUSTOM_FOUND_KEY = "greenhouse.firmalife_greenhouse_patch.custom";

    private MixedGreenhouseDetector() {}

    @Nullable
    public static Result detect(Level level, BlockPos stationPos)
    {
        final boolean shouldTry = flgp$hasAdjacentWall(level, stationPos) || flgp$shouldRetryFromStationState(level, stationPos);
        if (!shouldTry)
        {
            return null;
        }

        final int radius = Math.min(128, Mth.ceil(FLConfig.SERVER.greenhouseRadius.get() * PatchConfig.getGreenhouseRadiusMultiplier()));
        final BoundingBox box = new BoundingBox(stationPos).inflatedBy(radius);
        final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        final ShellAccumulator accumulator = new ShellAccumulator(level);
        final Set<BlockPos> positions = Mechanics.floodfill(
            level,
            stationPos,
            mutable,
            box,
            accumulator::testWall,
            state -> !Helpers.isBlock(state, FLBlocks.CLIMATE_STATION.get()),
            false,
            -1,
            Helpers.DIRECTIONS
        );
        if (positions.isEmpty() || !accumulator.isValid())
        {
            return null;
        }
        return accumulator.createResult(positions);
    }

    private static boolean flgp$hasAdjacentWall(Level level, BlockPos stationPos)
    {
        final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction direction : Helpers.DIRECTIONS)
        {
            mutable.setWithOffset(stationPos, direction);
            if (PatchConfig.isConfiguredGreenhouseWall(level.getBlockState(mutable)))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean flgp$shouldRetryFromStationState(Level level, BlockPos stationPos)
    {
        if (!(level.getBlockEntity(stationPos) instanceof ClimateStationAccess station))
        {
            return false;
        }
        return station.flgp$hasFavoriteGreenhouseType()
            || (station.flgp$getClimateType() == ClimateType.GREENHOUSE && station.flgp$getGreenhouseTier() > 0);
    }

    public record Result(Set<BlockPos> positions, int tier, GreenhouseStructureData structureData, @Nullable GreenhouseType representativeType)
    {
        public Component foundTitle()
        {
            if (representativeType != null)
            {
                return representativeType.getTitle();
            }
            return Component.translatable(structureData.mixedThermalWalls() ? MIXED_FOUND_KEY : CUSTOM_FOUND_KEY);
        }

        public boolean isRepresentativeStainless()
        {
            return representativeType != null && representativeType.id.getPath().contains("stainless_steel");
        }
    }

    private static final class ShellAccumulator
    {
        private final Level level;
        private final Map<PatchConfig.ThermalWallDefinition, Integer> thermalCounts = new LinkedHashMap<>();
        private final Set<BlockPos> shellBlocks = new java.util.HashSet<>();
        private final Set<BlockPos> thermalBlocks = new java.util.HashSet<>();

        private ShellAccumulator(Level level)
        {
            this.level = level;
        }

        private boolean testWall(BlockState wallState, BlockPos wallPos, Direction direction)
        {
            if (direction == Direction.DOWN)
            {
                // 贴地植物（浆果丛 / 蔓延丛 / 藤条 / 果树 / 香蕉、耕地作物）不能算作温室"地面墙"：
                // 向下扫描时若把它们当墙，它们自身就不会被计入温室内部位置集合，
                // 随后 ClimateStationRegistry 的 positions.contains(pos) 判定就会漏掉这些方块，
                // 表现为"温室对这类贴地植物不生效、温度显示的还是环境值"。
                if (isPlantBlock(wallState))
                {
                    return false;
                }
                return !wallState.isAir();
            }

            final PatchConfig.ThermalWallDefinition thermalRule = PatchConfig.getGreenhouseThermalWall(wallState);
            final boolean alwaysValid = PatchConfig.isGreenhouseAlwaysValidWall(wallState);
            final boolean sealOnly = PatchConfig.isGreenhouseSealOnlyWall(wallState);
            if (thermalRule == null && !sealOnly && !alwaysValid)
            {
                return false;
            }

            if (!alwaysValid && !(direction == Direction.UP && wallState.getBlock() instanceof SlabBlock) && !wallState.isFaceSturdy(level, wallPos, direction.getOpposite()))
            {
                return false;
            }

            final BlockPos immutablePos = wallPos.immutable();
            shellBlocks.add(immutablePos);
            if (thermalRule != null && thermalBlocks.add(immutablePos))
            {
                thermalCounts.merge(thermalRule, 1, Integer::sum);
            }
            return true;
        }

        private static boolean isPlantBlock(BlockState state)
        {
            return state.getBlock() instanceof net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock
                || state.getBlock() instanceof net.dries007.tfc.common.blocks.crop.CropBlock;
        }

        private boolean isValid()
        {
            if (thermalCounts.isEmpty())
            {
                return false;
            }
            if (thermalBlocks.size() < PatchConfig.getGreenhouseMinimumThermalBlocks())
            {
                return false;
            }
            if (shellBlocks.isEmpty())
            {
                return false;
            }
            final double coverage = thermalBlocks.size() / (double) shellBlocks.size();
            return coverage >= PatchConfig.getGreenhouseMinimumThermalCoverage();
        }

        private Result createResult(Set<BlockPos> positions)
        {
            final boolean mixed = thermalCounts.size() > 1;
            final int tier = flgp$computeValue(false);
            final int controlBonus = flgp$computeValue(true);

            PatchConfig.ThermalWallDefinition representativeRule = null;
            int bestCount = -1;
            for (Map.Entry<PatchConfig.ThermalWallDefinition, Integer> entry : thermalCounts.entrySet())
            {
                if (entry.getValue() > bestCount)
                {
                    representativeRule = entry.getKey();
                    bestCount = entry.getValue();
                }
            }

            final String displayNameKey;
            final GreenhouseType representativeType;
            if (!mixed && representativeRule != null)
            {
                displayNameKey = representativeRule.displayNameKey();
                representativeType = representativeRule.greenhouseType();
            }
            else
            {
                displayNameKey = mixed ? MIXED_NAME_KEY : CUSTOM_NAME_KEY;
                representativeType = null;
            }

            return new Result(
                new java.util.HashSet<>(positions),
                tier,
                new GreenhouseStructureData(displayNameKey, controlBonus, shellBlocks.size(), thermalBlocks.size(), mixed),
                representativeType
            );
        }

        private int flgp$computeValue(boolean controlBonus)
        {
            return switch (PatchConfig.getGreenhouseTierMode())
            {
                case MINIMUM -> thermalCounts.keySet().stream()
                    .mapToInt(rule -> controlBonus ? rule.controlBonus() : rule.tier())
                    .min()
                    .orElse(0);
                case MAXIMUM -> thermalCounts.keySet().stream()
                    .mapToInt(rule -> controlBonus ? rule.controlBonus() : rule.tier())
                    .max()
                    .orElse(0);
                case WEIGHTED_AVERAGE -> {
                    int total = 0;
                    int weighted = 0;
                    for (Map.Entry<PatchConfig.ThermalWallDefinition, Integer> entry : thermalCounts.entrySet())
                    {
                        final int value = controlBonus ? entry.getKey().controlBonus() : entry.getKey().tier();
                        weighted += value * entry.getValue();
                        total += entry.getValue();
                    }
                    yield total <= 0 ? 0 : Math.round(weighted / (float) total);
                }
            };
        }
    }
}
