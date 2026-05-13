package com.g1739.firmalifegreenhousepatch.common.temperature;

import com.eerussianguy.firmalife.common.blockentities.ClimateType;
import com.eerussianguy.firmalife.common.blocks.greenhouse.ClimateStationBlock;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ClimateStationRegistry
{
    private static final Map<Level, Set<ClimateStationAccess>> STATIONS = Collections.synchronizedMap(new WeakHashMap<>());

    private ClimateStationRegistry() {}

    public static void register(BlockEntity blockEntity, ClimateStationAccess station)
    {
        final Level level = blockEntity.getLevel();
        if (level == null)
        {
            return;
        }

        synchronized (STATIONS)
        {
            STATIONS.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(station);
        }
    }

    public static void unregister(BlockEntity blockEntity, ClimateStationAccess station)
    {
        synchronized (STATIONS)
        {
            final Level level = blockEntity.getLevel();
            if (level != null)
            {
                final Set<ClimateStationAccess> stations = STATIONS.get(level);
                if (stations != null)
                {
                    stations.remove(station);
                    if (stations.isEmpty())
                    {
                        STATIONS.remove(level);
                    }
                }
            }
            else
            {
                STATIONS.values().forEach(stations -> stations.remove(station));
            }
        }
    }

    @Nullable
    public static ClimateStationAccess findControllingStation(Level level, BlockPos targetPos)
    {
        return findControllingStation(level, targetPos, ClimateType.GREENHOUSE, true);
    }

    @Nullable
    public static ClimateStationAccess findControllingCellarStation(Level level, BlockPos targetPos)
    {
        return findControllingStation(level, targetPos, ClimateType.CELLAR, false);
    }

    public static boolean isActiveStation(BlockEntity blockEntity, ClimateStationAccess station)
    {
        final Level level = blockEntity.getLevel();
        if (level == null || blockEntity.isRemoved())
        {
            return false;
        }

        final BlockState state = blockEntity.getBlockState();
        return station.flgp$getGreenhousePositions() != null
            && state.getBlock() instanceof ClimateStationBlock
            && state.hasProperty(ClimateStationBlock.STASIS)
            && state.getValue(ClimateStationBlock.STASIS);
    }

    @Nullable
    private static ClimateStationAccess findControllingStation(Level level, BlockPos targetPos, ClimateType climateType, boolean includeBelow)
    {
        synchronized (STATIONS)
        {
            final Set<ClimateStationAccess> stations = STATIONS.get(level);
            if (stations == null || stations.isEmpty())
            {
                return null;
            }

            final BlockPos soilPos = targetPos.below();
            ClimateStationAccess closestStation = null;
            double closestDistance = Double.MAX_VALUE;

            final Iterator<ClimateStationAccess> iterator = stations.iterator();
            while (iterator.hasNext())
            {
                final ClimateStationAccess station = iterator.next();
                if (!(station instanceof BlockEntity blockEntity))
                {
                    iterator.remove();
                    continue;
                }
                if (blockEntity.isRemoved() || blockEntity.getLevel() != level)
                {
                    iterator.remove();
                    continue;
                }
                if (station.flgp$getClimateType() != climateType)
                {
                    continue;
                }
                if (!isActiveStation(blockEntity, station))
                {
                    continue;
                }
                final Set<BlockPos> positions = station.flgp$getGreenhousePositions();
                if (!positions.contains(targetPos) && (!includeBelow || !positions.contains(soilPos)))
                {
                    continue;
                }

                final double distance = blockEntity.getBlockPos().distSqr(targetPos);
                if (distance < closestDistance)
                {
                    closestDistance = distance;
                    closestStation = station;
                }
            }

            if (stations.isEmpty())
            {
                STATIONS.remove(level);
            }
            return closestStation;
        }
    }
}
