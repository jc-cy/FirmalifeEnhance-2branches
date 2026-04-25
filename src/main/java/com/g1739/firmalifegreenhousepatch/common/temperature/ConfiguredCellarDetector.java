package com.g1739.firmalifegreenhousepatch.common.temperature;

import com.eerussianguy.firmalife.common.blockentities.ClimateStationBlockEntity;
import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.eerussianguy.firmalife.common.util.Mechanics;
import com.eerussianguy.firmalife.config.FLConfig;
import com.g1739.firmalifegreenhousepatch.common.config.PatchConfig;
import java.util.HashSet;
import java.util.Set;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

public final class ConfiguredCellarDetector
{
    private ConfiguredCellarDetector() {}

    @Nullable
    public static Set<BlockPos> detect(Level level, BlockPos pos)
    {
        return detect(level, pos, -1);
    }

    @Nullable
    public static Set<BlockPos> detect(Level level, BlockPos pos, int lastSize)
    {
        final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (Direction direction : Helpers.DIRECTIONS)
        {
            mutable.setWithOffset(pos, direction);
            if (PatchConfig.isConfiguredCellarWall(level.getBlockState(mutable)))
            {
                return tryFindCellar(level, pos, lastSize, mutable);
            }
        }
        if (level.getBlockEntity(pos) instanceof ClimateStationBlockEntity station && station.favoriteIsCellar())
        {
            return tryFindCellar(level, pos, lastSize, mutable);
        }
        return null;
    }

    @Nullable
    private static Set<BlockPos> tryFindCellar(Level level, BlockPos pos, int lastSize, BlockPos.MutableBlockPos mutable)
    {
        final int radius = Math.min(128, Mth.ceil(FLConfig.SERVER.cellarRadius.get() * PatchConfig.getCellarRadiusMultiplier()));
        final BoundingBox box = new BoundingBox(pos).inflatedBy(radius);
        final ShellAccumulator accumulator = new ShellAccumulator();
        final Set<BlockPos> filled = Mechanics.floodfill(
            level,
            pos,
            mutable,
            box,
            accumulator::testWall,
            state -> !Helpers.isBlock(state, FLBlocks.CLIMATE_STATION.get()),
            false,
            lastSize,
            Helpers.DIRECTIONS
        );
        return filled.isEmpty() || !accumulator.isValid() ? null : filled;
    }

    private static final class ShellAccumulator
    {
        private final Set<BlockPos> shellBlocks = new HashSet<>();
        private final Set<BlockPos> thermalBlocks = new HashSet<>();

        private boolean testWall(BlockState wallState, BlockPos wallPos, Direction direction)
        {
            final boolean thermal = PatchConfig.isConfiguredCellarThermalWall(wallState);
            final boolean sealOnly = PatchConfig.isConfiguredCellarSealOnlyWall(wallState);
            if (!thermal && !sealOnly)
            {
                return false;
            }

            final BlockPos immutablePos = wallPos.immutable();
            shellBlocks.add(immutablePos);
            if (thermal)
            {
                thermalBlocks.add(immutablePos);
            }
            return true;
        }

        private boolean isValid()
        {
            if (thermalBlocks.isEmpty() || shellBlocks.isEmpty())
            {
                return false;
            }
            final double coverage = thermalBlocks.size() / (double) shellBlocks.size();
            return coverage >= PatchConfig.getCellarMinimumThermalCoverage();
        }
    }
}
