package com.g1739.firmalifegreenhousepatch.common.temperature;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public record GreenhouseStructureData(
    String displayNameKey,
    int controlBonus,
    int totalShellBlocks,
    int thermalShellBlocks,
    boolean mixedThermalWalls
)
{
    private static final String DISPLAY_NAME_KEY = "DisplayNameKey";
    private static final String CONTROL_BONUS_KEY = "ControlBonus";
    private static final String TOTAL_SHELL_BLOCKS_KEY = "TotalShellBlocks";
    private static final String THERMAL_SHELL_BLOCKS_KEY = "ThermalShellBlocks";
    private static final String MIXED_THERMAL_WALLS_KEY = "MixedThermalWalls";

    public int thermalCoveragePercent()
    {
        return totalShellBlocks <= 0 ? 0 : Math.round(thermalShellBlocks * 100f / totalShellBlocks);
    }

    public CompoundTag toTag()
    {
        final CompoundTag tag = new CompoundTag();
        tag.putString(DISPLAY_NAME_KEY, displayNameKey);
        tag.putInt(CONTROL_BONUS_KEY, controlBonus);
        tag.putInt(TOTAL_SHELL_BLOCKS_KEY, totalShellBlocks);
        tag.putInt(THERMAL_SHELL_BLOCKS_KEY, thermalShellBlocks);
        tag.putBoolean(MIXED_THERMAL_WALLS_KEY, mixedThermalWalls);
        return tag;
    }

    @Nullable
    public static GreenhouseStructureData fromTag(@Nullable CompoundTag tag)
    {
        if (tag == null || !tag.contains(DISPLAY_NAME_KEY))
        {
            return null;
        }
        return new GreenhouseStructureData(
            tag.getString(DISPLAY_NAME_KEY),
            tag.getInt(CONTROL_BONUS_KEY),
            tag.getInt(TOTAL_SHELL_BLOCKS_KEY),
            tag.getInt(THERMAL_SHELL_BLOCKS_KEY),
            tag.getBoolean(MIXED_THERMAL_WALLS_KEY)
        );
    }
}
