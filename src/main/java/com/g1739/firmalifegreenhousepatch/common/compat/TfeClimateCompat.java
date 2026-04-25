package com.g1739.firmalifegreenhousepatch.common.compat;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

public final class TfeClimateCompat
{
    public static final String MOD_ID = "tfe";

    private TfeClimateCompat() {}

    public static boolean isEnabled()
    {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static BlockPos getBananaRootPos(LevelReader level, BlockPos pos)
    {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir())
        {
            return pos;
        }

        final BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 16; i++)
        {
            final BlockPos belowPos = cursor.below();
            if (level.getBlockState(belowPos).getBlock() != state.getBlock())
            {
                break;
            }
            cursor.move(Direction.DOWN);
        }
        return cursor.immutable();
    }

    public static int getFruitBushHydrationFromRootPos(Level level, BlockPos rootPos)
    {
        return FarmlandBlock.getHydration(level, rootPos);
    }

    public static BlockPos getFruitTreeStemPos(LevelReader level, BlockPos pos)
    {
        if (Helpers.isBlock(level.getBlockState(pos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
        {
            return findFruitTreeBase(level, pos);
        }
        if (Helpers.isBlock(level.getBlockState(pos.below()), TFCTags.Blocks.FRUIT_TREE_BRANCH))
        {
            return findFruitTreeBase(level, pos.below());
        }

        for (Direction direction : Helpers.DIRECTIONS)
        {
            final BlockPos adjacentPos = pos.relative(direction);
            if (Helpers.isBlock(level.getBlockState(adjacentPos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
            {
                return findFruitTreeBase(level, adjacentPos);
            }
        }
        return pos;
    }

    public static BlockPos getFruitTreeSaplingStemPos(LevelReader level, BlockPos pos)
    {
        final BlockPos belowPos = pos.below();
        return Helpers.isBlock(level.getBlockState(belowPos), TFCTags.Blocks.FRUIT_TREE_BRANCH) ? findFruitTreeBase(level, belowPos) : pos;
    }

    public static BlockPos resolveFruitTreeTooltipPos(LevelReader level, BlockPos originalPos)
    {
        if (isEnabled() && Helpers.isBlock(level.getBlockState(originalPos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
        {
            return originalPos;
        }
        return originalPos.above();
    }

    private static BlockPos findFruitTreeBase(LevelReader level, BlockPos startPos)
    {
        final BlockPos.MutableBlockPos cursor = startPos.mutable();
        for (int i = 0; i < 32; i++)
        {
            final BlockPos belowPos = cursor.below();
            if (!Helpers.isBlock(level.getBlockState(belowPos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
            {
                break;
            }
            cursor.move(Direction.DOWN);
        }

        final BlockState state = level.getBlockState(cursor);
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            final var property = PipeBlock.PROPERTY_BY_DIRECTION.get(direction);
            if (state.hasProperty(property) && state.getValue(property))
            {
                final BlockPos sidePos = cursor.relative(direction);
                if (Helpers.isBlock(level.getBlockState(sidePos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
                {
                    final BlockPos.MutableBlockPos sideCursor = sidePos.mutable();
                    for (int i = 0; i < 32; i++)
                    {
                        final BlockPos belowPos = sideCursor.below();
                        if (!Helpers.isBlock(level.getBlockState(belowPos), TFCTags.Blocks.FRUIT_TREE_BRANCH))
                        {
                            break;
                        }
                        sideCursor.move(Direction.DOWN);
                    }
                    if (sideCursor.getY() < cursor.getY())
                    {
                        return sideCursor.immutable();
                    }
                }
            }
        }
        return cursor.immutable();
    }
}
