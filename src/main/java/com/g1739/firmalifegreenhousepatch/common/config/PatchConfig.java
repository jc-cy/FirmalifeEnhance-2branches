package com.g1739.firmalifegreenhousepatch.common.config;

import com.eerussianguy.firmalife.common.util.GreenhouseType;
import com.g1739.firmalifegreenhousepatch.FirmalifeGreenhousePatch;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class PatchConfig
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.IntValue DEFAULT_TARGET_TEMPERATURE;
    private static final ForgeConfigSpec.IntValue MIN_TARGET_TEMPERATURE;
    private static final ForgeConfigSpec.IntValue MAX_TARGET_TEMPERATURE;
    private static final ForgeConfigSpec.IntValue MIN_CELLAR_TEMPERATURE;
    private static final ForgeConfigSpec.IntValue CELLAR_BASE_CONTROL_RANGE;
    private static final ForgeConfigSpec.DoubleValue GREENHOUSE_RADIUS_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue CELLAR_RADIUS_MULTIPLIER;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> HEATING_ITEMS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> COOLING_ITEMS;

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> GREENHOUSE_THERMAL_WALLS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> GREENHOUSE_SEAL_ONLY_WALLS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> GREENHOUSE_ALWAYS_VALID_WALLS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CELLAR_THERMAL_WALLS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> CELLAR_SEAL_ONLY_WALLS;
    private static final ForgeConfigSpec.DoubleValue GREENHOUSE_MINIMUM_THERMAL_COVERAGE;
    private static final ForgeConfigSpec.IntValue GREENHOUSE_MINIMUM_THERMAL_BLOCKS;
    private static final ForgeConfigSpec.DoubleValue CELLAR_MINIMUM_THERMAL_COVERAGE;
    private static final ForgeConfigSpec.ConfigValue<String> GREENHOUSE_TIER_MODE;

    private static final ForgeConfigSpec.IntValue CELLAR_LEVEL_1_MAX_TEMPERATURE;
    private static final ForgeConfigSpec.IntValue CELLAR_LEVEL_2_MAX_TEMPERATURE;
    private static final ForgeConfigSpec.IntValue CELLAR_LEVEL_3_MAX_TEMPERATURE;
    private static final ForgeConfigSpec.IntValue CELLAR_LEVEL_4_MAX_TEMPERATURE;
    private static final ForgeConfigSpec.IntValue CELLAR_LEVEL_5_MAX_TEMPERATURE;
    private static final ForgeConfigSpec.IntValue CELLAR_LEVEL_6_MAX_TEMPERATURE;
    private static final ForgeConfigSpec.IntValue CELLAR_LEVEL_7_MAX_TEMPERATURE;

    private static final ForgeConfigSpec.DoubleValue CELLAR_LEVEL_1_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue CELLAR_LEVEL_2_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue CELLAR_LEVEL_3_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue CELLAR_LEVEL_4_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue CELLAR_LEVEL_5_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue CELLAR_LEVEL_6_MULTIPLIER;
    private static final ForgeConfigSpec.DoubleValue CELLAR_LEVEL_7_MULTIPLIER;

    private static volatile Map<Item, Float> heatingItemFactors = Map.of();
    private static volatile Map<Item, Float> coolingItemFactors = Map.of();
    private static volatile GreenhouseRuleSet greenhouseRuleSet = GreenhouseRuleSet.empty();
    private static volatile CellarRuleSet cellarRuleSet = CellarRuleSet.empty();
    private static volatile List<CellarLevel> cellarPreservationLevels = List.of(
        new CellarLevel(-24, 8.0f),
        new CellarLevel(-21, 7.0f),
        new CellarLevel(-18, 6.0f),
        new CellarLevel(-15, 5.0f),
        new CellarLevel(-12, 4.0f),
        new CellarLevel(-1, 3.0f),
        new CellarLevel(2048, 2.5f)
    );

    static
    {
        final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        pushSection(builder, "temperature", "temperature");
        DEFAULT_TARGET_TEMPERATURE = translate(builder, "temperature.default_target_temperature")
            .worldRestart()
            .comment(
                "气象站还没保存过目标温度时，界面默认显示的温度。",
                "Default target temperature shown when a climate station has not stored a value yet."
            )
            .defineInRange("defaultTargetTemperature", 20, -4096, 4096);
        MIN_TARGET_TEMPERATURE = translate(builder, "temperature.minimum_target_temperature")
            .worldRestart()
            .comment(
                "补丁允许输入的最低目标温度硬下限。",
                "Minimum value accepted by the patch temperature clamp."
            )
            .defineInRange("minimumTargetTemperature", -100, -4096, 4096);
        MAX_TARGET_TEMPERATURE = translate(builder, "temperature.maximum_target_temperature")
            .worldRestart()
            .comment(
                "补丁允许输入的最高目标温度硬上限。",
                "Maximum value accepted by the patch temperature clamp."
            )
            .defineInRange("maximumTargetTemperature", 100, -4096, 4096);
        MIN_CELLAR_TEMPERATURE = translate(builder, "temperature.minimum_cellar_temperature")
            .worldRestart()
            .comment(
                "地窖模式在降温后允许达到的最低温度。",
                "Lowest cellar temperature the patch will allow after cooling."
            )
            .defineInRange("minimumCellarTemperature", -24, -4096, 4096);
        builder.pop();

        pushSection(builder, "searchRadius", "search_radius");
        GREENHOUSE_RADIUS_MULTIPLIER = translate(builder, "search_radius.greenhouse_radius_multiplier")
            .worldRestart()
            .comment(
                "应用到 Firmalife 温室搜索半径的倍率。",
                "Multiplier applied to Firmalife greenhouse search radius."
            )
            .defineInRange("greenhouseRadiusMultiplier", 3.0d, 0.1d, 16.0d);
        CELLAR_RADIUS_MULTIPLIER = translate(builder, "search_radius.cellar_radius_multiplier")
            .worldRestart()
            .comment(
                "应用到 Firmalife 地窖搜索半径的倍率。",
                "Multiplier applied to Firmalife cellar search radius."
            )
            .defineInRange("cellarRadiusMultiplier", 2.0d, 0.1d, 16.0d);
        builder.pop();

        pushSection(builder, "thermalItems", "thermal_items");
        HEATING_ITEMS = translate(builder, "thermal_items.heating_items")
            .worldRestart()
            .comment(
                "气象站加热槽可接受的物品列表。",
                "格式：modid:item=倍率",
                "默认包含原版岩浆块和常见 TFC 岩浆岩方块。",
                "Heating items accepted by the climate station.",
                "Format: modid:item=factor",
                "Default includes vanilla magma block and common TFC magma-rock blocks."
            )
            .defineListAllowEmpty(
                List.of("heatingItems"),
                PatchConfig::defaultHeatingItems,
                value -> value instanceof String
            );
        COOLING_ITEMS = translate(builder, "thermal_items.cooling_items")
            .worldRestart()
            .comment(
                "气象站降温槽可接受的物品列表。",
                "格式：modid:item=倍率",
                "Cooling items accepted by the climate station.",
                "Format: modid:item=factor"
            )
            .defineListAllowEmpty(
                List.of("coolingItems"),
                PatchConfig::defaultCoolingItems,
                value -> value instanceof String
            );
        builder.pop();

        pushSection(builder, "greenhouseWalls", "greenhouse_walls");
        GREENHOUSE_THERMAL_WALLS = translate(builder, "thermal_items.greenhouse_thermal_walls")
            .worldRestart()
            .comment(
                "混合温室检测使用的保温温室墙规则。",
                "格式：方块ID或标签=tier",
                "可选扩展：方块ID或标签=tier,额外控温值",
                "例子：",
                "  #firmalife:all_treated_wood_greenhouse=5",
                "  #firmalife:stainless_steel_greenhouse=25",
                "如果同一个方块命中多条规则，只取最前面的一条。",
                "第二个数字只有在你想手动额外补控温范围时才需要写。",
                "Insulated greenhouse wall rules used by the mixed greenhouse detector.",
                "Format: block_or_tag=tier",
                "Optional: block_or_tag=tier,controlBonus",
                "Examples:",
                "  #firmalife:all_treated_wood_greenhouse=5",
                "  #firmalife:stainless_steel_greenhouse=25",
                "First match wins when one block matches multiple rules.",
                "Only use controlBonus when you explicitly want extra heating/cooling range on top of tier."
            )
            .defineListAllowEmpty(
                List.of("greenhouseThermalWalls"),
                PatchConfig::defaultGreenhouseThermalWalls,
                value -> value instanceof String
            );
        GREENHOUSE_SEAL_ONLY_WALLS = translate(builder, "thermal_items.greenhouse_seal_only_walls")
            .worldRestart()
            .comment(
                "只负责封闭温室、但不提供保温倍率的功能温室墙规则。",
                "格式：方块ID或标签",
                "默认已加入 IE 的低压/中压/高压蓄电池。",
                "除非它们也被列进 greenhouseAlwaysValidWalls，否则仍需要满足结实面的判定。",
                "Functional greenhouse walls that only seal the structure but do not add insulation.",
                "Format: block_or_tag",
                "Defaults already include IE LV/MV/HV capacitors.",
                "These blocks still need a sturdy face unless they are also listed in greenhouseAlwaysValidWalls."
            )
            .defineListAllowEmpty(
                List.of("greenhouseSealOnlyWalls"),
                PatchConfig::defaultGreenhouseSealOnlyWalls,
                value -> value instanceof String
            );
        GREENHOUSE_ALWAYS_VALID_WALLS = translate(builder, "thermal_items.greenhouse_always_valid_walls")
            .worldRestart()
            .comment(
                "匹配这些标签或方块后，可跳过温室墙的结实面检查。",
                "格式：方块ID或标签",
                "默认跳过门和活板门，也适合端口、电缆、管道等功能接口方块。",
                "Functional greenhouse walls that are allowed to seal the greenhouse without a sturdy-face check.",
                "Format: block_or_tag",
                "Defaults skip doors and trapdoors, and it also works for ports, cables, pipes, and similar interface blocks."
            )
            .defineListAllowEmpty(
                List.of("greenhouseAlwaysValidWalls"),
                PatchConfig::defaultGreenhouseAlwaysValidWalls,
                value -> value instanceof String
            );
        GREENHOUSE_MINIMUM_THERMAL_COVERAGE = translate(builder, "thermal_items.greenhouse_minimum_thermal_coverage")
            .worldRestart()
            .comment(
                "保温温室墙在所有被计入的侧墙/屋顶壳体中的最低占比。",
                "例如 0.95 = 95%。",
                "地板不参与这个比例的计算。",
                "Minimum ratio of insulated greenhouse walls among all counted side/roof shell blocks.",
                "For example, 0.95 = 95%.",
                "The floor is not counted in this ratio."
            )
            .defineInRange("greenhouseMinimumThermalCoverage", 0.95d, 0d, 1d);
        GREENHOUSE_MINIMUM_THERMAL_BLOCKS = translate(builder, "thermal_items.greenhouse_minimum_thermal_blocks")
            .worldRestart()
            .comment(
                "温室被判定为有效时所需的最少保温温室墙数量。",
                "Minimum number of insulated greenhouse walls required for a greenhouse to become valid."
            )
            .defineInRange("greenhouseMinimumThermalBlocks", 8, 0, 65536);
        GREENHOUSE_TIER_MODE = translate(builder, "thermal_items.greenhouse_tier_mode")
            .worldRestart()
            .comment(
                "多种保温温室墙混合时，如何计算最终生效的温室等级。",
                "weighted_average = 按各种保温温室墙占比算平均",
                "minimum = 取最差的那种保温温室墙",
                "maximum = 取最好的那种保温温室墙",
                "How mixed insulated greenhouse walls are merged into one effective greenhouse tier.",
                "weighted_average = weighted average by wall count",
                "minimum = use the weakest insulated wall",
                "maximum = use the strongest insulated wall"
            )
            .define("greenhouseTierMode", "weighted_average");
        builder.pop();

        pushSection(builder, "cellarWalls", "cellar_walls");
        CELLAR_THERMAL_WALLS = translate(builder, "thermal_items.cellar_thermal_walls")
            .worldRestart()
            .comment(
                "用于地窖检测的保温墙规则。",
                "格式：方块ID或标签",
                "默认包含 Firmalife 原版 #firmalife:cellar_insulation。",
                "只有匹配这里的方块，才会被计入地窖保温墙占比。",
                "Insulated cellar wall rules used by cellar detection.",
                "Format: block id or tag",
                "Defaults include Firmalife's #firmalife:cellar_insulation.",
                "Only matching blocks count toward the insulated cellar wall coverage."
            )
            .defineListAllowEmpty(
                List.of("cellarThermalWalls"),
                PatchConfig::defaultCellarThermalWalls,
                value -> value instanceof String
            );
        CELLAR_SEAL_ONLY_WALLS = translate(builder, "thermal_items.cellar_seal_only_walls")
            .worldRestart()
            .comment(
                "只负责封闭地窖、但不计入保温墙占比的功能地窖墙规则。",
                "格式：方块ID或标签",
                "默认已加入 IE 的低压/中压/高压蓄电池。",
                "这类方块可以参与地窖壳体检测，但不会提供地窖保温墙占比。",
                "Functional cellar walls that seal the cellar but do not count as insulated walls.",
                "Format: block id or tag",
                "Defaults already include IE LV/MV/HV capacitors.",
                "These blocks can seal the cellar shell but do not contribute to insulated wall coverage."
            )
            .defineListAllowEmpty(
                List.of("cellarSealOnlyWalls"),
                PatchConfig::defaultCellarSealOnlyWalls,
                value -> value instanceof String
            );
        CELLAR_MINIMUM_THERMAL_COVERAGE = translate(builder, "thermal_items.cellar_minimum_thermal_coverage")
            .worldRestart()
            .comment(
                "地窖保温墙在所有被计入的地窖壳体中的最低占比。",
                "例如 0.95 = 95%。",
                "纯功能墙地窖或保温墙占比太低的地窖会直接失效。",
                "Minimum ratio of insulated cellar walls among all counted cellar shell blocks.",
                "For example, 0.95 = 95%.",
                "Pure functional-wall cellars or cellars with too little insulated coverage will fail."
            )
            .defineInRange("cellarMinimumThermalCoverage", 0.95d, 0d, 1d);
        CELLAR_BASE_CONTROL_RANGE = translate(builder, "cellar_walls.cellar_base_control_range")
            .worldRestart()
            .comment(
                "地窖模式的基础保温倍率。",
                "默认 10 就是当前常说的“10倍控温”。",
                "最终可升温/降温范围还会再乘热源或冷源物品因子。",
                "Base insulation multiplier used by cellar mode.",
                "The default 10 is the current commonly mentioned \"10x cellar control\".",
                "The final heating/cooling range is still multiplied by the thermal item factor."
            )
            .defineInRange("cellarBaseControlRange", 10, 0, 256);
        builder.pop();

        pushSection(builder, "cellarPreservation", "cellar_preservation");
        CELLAR_LEVEL_1_MAX_TEMPERATURE = translate(builder, "cellar_preservation.level1_max_temperature").worldRestart().comment("当地窖温度 <= 这个值时，使用第 1 档保鲜倍率。", "Level 1 applies when cellar temperature is <= this value.").defineInRange("level1MaxTemperature", -24, -4096, 4096);
        CELLAR_LEVEL_1_MULTIPLIER = translate(builder, "cellar_preservation.level1_multiplier").worldRestart().comment("第 1 档保鲜倍率。", "Level 1 preservation multiplier.").defineInRange("level1Multiplier", 8.0d, 0.01d, 1024.0d);
        CELLAR_LEVEL_2_MAX_TEMPERATURE = translate(builder, "cellar_preservation.level2_max_temperature").worldRestart().comment("当地窖温度 <= 这个值时，使用第 2 档保鲜倍率。", "Level 2 applies when cellar temperature is <= this value.").defineInRange("level2MaxTemperature", -21, -4096, 4096);
        CELLAR_LEVEL_2_MULTIPLIER = translate(builder, "cellar_preservation.level2_multiplier").worldRestart().comment("第 2 档保鲜倍率。", "Level 2 preservation multiplier.").defineInRange("level2Multiplier", 7.0d, 0.01d, 1024.0d);
        CELLAR_LEVEL_3_MAX_TEMPERATURE = translate(builder, "cellar_preservation.level3_max_temperature").worldRestart().comment("当地窖温度 <= 这个值时，使用第 3 档保鲜倍率。", "Level 3 applies when cellar temperature is <= this value.").defineInRange("level3MaxTemperature", -18, -4096, 4096);
        CELLAR_LEVEL_3_MULTIPLIER = translate(builder, "cellar_preservation.level3_multiplier").worldRestart().comment("第 3 档保鲜倍率。", "Level 3 preservation multiplier.").defineInRange("level3Multiplier", 6.0d, 0.01d, 1024.0d);
        CELLAR_LEVEL_4_MAX_TEMPERATURE = translate(builder, "cellar_preservation.level4_max_temperature").worldRestart().comment("当地窖温度 <= 这个值时，使用第 4 档保鲜倍率。", "Level 4 applies when cellar temperature is <= this value.").defineInRange("level4MaxTemperature", -15, -4096, 4096);
        CELLAR_LEVEL_4_MULTIPLIER = translate(builder, "cellar_preservation.level4_multiplier").worldRestart().comment("第 4 档保鲜倍率。", "Level 4 preservation multiplier.").defineInRange("level4Multiplier", 5.0d, 0.01d, 1024.0d);
        CELLAR_LEVEL_5_MAX_TEMPERATURE = translate(builder, "cellar_preservation.level5_max_temperature").worldRestart().comment("当地窖温度 <= 这个值时，使用第 5 档保鲜倍率。", "Level 5 applies when cellar temperature is <= this value.").defineInRange("level5MaxTemperature", -12, -4096, 4096);
        CELLAR_LEVEL_5_MULTIPLIER = translate(builder, "cellar_preservation.level5_multiplier").worldRestart().comment("第 5 档保鲜倍率。", "Level 5 preservation multiplier.").defineInRange("level5Multiplier", 4.0d, 0.01d, 1024.0d);
        CELLAR_LEVEL_6_MAX_TEMPERATURE = translate(builder, "cellar_preservation.level6_max_temperature").worldRestart().comment("当地窖温度 <= 这个值时，使用第 6 档保鲜倍率。", "Level 6 applies when cellar temperature is <= this value.").defineInRange("level6MaxTemperature", -1, -4096, 4096);
        CELLAR_LEVEL_6_MULTIPLIER = translate(builder, "cellar_preservation.level6_multiplier").worldRestart().comment("第 6 档保鲜倍率。", "Level 6 preservation multiplier.").defineInRange("level6Multiplier", 3.0d, 0.01d, 1024.0d);
        CELLAR_LEVEL_7_MAX_TEMPERATURE = translate(builder, "cellar_preservation.level7_max_temperature").worldRestart().comment("当地窖温度 <= 这个值时，使用兜底档保鲜倍率。", "Fallback level applies when cellar temperature is <= this value.").defineInRange("level7MaxTemperature", 2048, -4096, 4096);
        CELLAR_LEVEL_7_MULTIPLIER = translate(builder, "cellar_preservation.level7_multiplier").worldRestart().comment("兜底档保鲜倍率。", "Fallback level preservation multiplier.").defineInRange("level7Multiplier", 2.5d, 0.01d, 1024.0d);
        builder.pop();

        SPEC = builder.build();
    }

    private PatchConfig() {}

    private static ForgeConfigSpec.Builder pushSection(ForgeConfigSpec.Builder builder, String path, String translationSuffix)
    {
        return builder.translation(configTranslationKey("category." + translationSuffix)).push(path);
    }

    private static ForgeConfigSpec.Builder translate(ForgeConfigSpec.Builder builder, String translationSuffix)
    {
        return builder.translation(configTranslationKey(translationSuffix));
    }

    private static String configTranslationKey(String path)
    {
        return FirmalifeGreenhousePatch.MOD_ID + ".config." + path;
    }

    public static void register(IEventBus bus)
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, FirmalifeGreenhousePatch.MOD_ID + "-common.toml");
        bus.addListener(PatchConfig::onConfigLoading);
        bus.addListener(PatchConfig::onConfigReloading);
    }

    public static void refreshCaches()
    {
        heatingItemFactors = parseItemFactors(HEATING_ITEMS.get(), "heatingItems");
        coolingItemFactors = parseItemFactors(COOLING_ITEMS.get(), "coolingItems");
        greenhouseRuleSet = parseGreenhouseRuleSet();
        cellarRuleSet = new CellarRuleSet(
            List.copyOf(parseBlockMatchers(CELLAR_THERMAL_WALLS.get(), "cellarThermalWalls")),
            List.copyOf(parseBlockMatchers(CELLAR_SEAL_ONLY_WALLS.get(), "cellarSealOnlyWalls"))
        );

        final List<CellarLevel> levels = new ArrayList<>();
        levels.add(new CellarLevel(CELLAR_LEVEL_1_MAX_TEMPERATURE.get(), CELLAR_LEVEL_1_MULTIPLIER.get().floatValue()));
        levels.add(new CellarLevel(CELLAR_LEVEL_2_MAX_TEMPERATURE.get(), CELLAR_LEVEL_2_MULTIPLIER.get().floatValue()));
        levels.add(new CellarLevel(CELLAR_LEVEL_3_MAX_TEMPERATURE.get(), CELLAR_LEVEL_3_MULTIPLIER.get().floatValue()));
        levels.add(new CellarLevel(CELLAR_LEVEL_4_MAX_TEMPERATURE.get(), CELLAR_LEVEL_4_MULTIPLIER.get().floatValue()));
        levels.add(new CellarLevel(CELLAR_LEVEL_5_MAX_TEMPERATURE.get(), CELLAR_LEVEL_5_MULTIPLIER.get().floatValue()));
        levels.add(new CellarLevel(CELLAR_LEVEL_6_MAX_TEMPERATURE.get(), CELLAR_LEVEL_6_MULTIPLIER.get().floatValue()));
        levels.add(new CellarLevel(CELLAR_LEVEL_7_MAX_TEMPERATURE.get(), CELLAR_LEVEL_7_MULTIPLIER.get().floatValue()));
        levels.sort(Comparator.comparingInt(CellarLevel::maxTemperature));
        cellarPreservationLevels = List.copyOf(levels);
    }

    public static int getDefaultTemperature()
    {
        return DEFAULT_TARGET_TEMPERATURE.get();
    }

    public static int getMinimumTemperature()
    {
        return Math.min(MIN_TARGET_TEMPERATURE.get(), MAX_TARGET_TEMPERATURE.get());
    }

    public static int getMaximumTemperature()
    {
        return Math.max(MIN_TARGET_TEMPERATURE.get(), MAX_TARGET_TEMPERATURE.get());
    }

    public static int getMinimumCellarTemperature()
    {
        return MIN_CELLAR_TEMPERATURE.get();
    }

    public static int getCellarBaseControlRange()
    {
        return CELLAR_BASE_CONTROL_RANGE.get();
    }

    public static double getGreenhouseRadiusMultiplier()
    {
        return GREENHOUSE_RADIUS_MULTIPLIER.get();
    }

    public static double getCellarRadiusMultiplier()
    {
        return CELLAR_RADIUS_MULTIPLIER.get();
    }

    public static float getHeatingItemFactor(ItemStack stack)
    {
        return stack.isEmpty() ? 0f : heatingItemFactors.getOrDefault(stack.getItem(), 0f);
    }

    public static float getCoolingItemFactor(ItemStack stack)
    {
        return stack.isEmpty() ? 0f : coolingItemFactors.getOrDefault(stack.getItem(), 0f);
    }

    @Nullable
    public static ThermalWallDefinition getGreenhouseThermalWall(BlockState state)
    {
        return greenhouseRuleSet.findThermalWall(state);
    }

    public static boolean isGreenhouseSealOnlyWall(BlockState state)
    {
        return greenhouseRuleSet.isSealOnlyWall(state);
    }

    public static boolean isGreenhouseAlwaysValidWall(BlockState state)
    {
        return greenhouseRuleSet.isAlwaysValidWall(state);
    }

    public static boolean isConfiguredGreenhouseWall(BlockState state)
    {
        return getGreenhouseThermalWall(state) != null || isGreenhouseSealOnlyWall(state) || isGreenhouseAlwaysValidWall(state);
    }

    public static boolean isConfiguredCellarWall(BlockState state)
    {
        return cellarRuleSet.matches(state);
    }

    public static boolean isConfiguredCellarThermalWall(BlockState state)
    {
        return cellarRuleSet.isThermalWall(state);
    }

    public static boolean isConfiguredCellarSealOnlyWall(BlockState state)
    {
        return cellarRuleSet.isSealOnlyWall(state);
    }

    public static double getGreenhouseMinimumThermalCoverage()
    {
        return GREENHOUSE_MINIMUM_THERMAL_COVERAGE.get();
    }

    public static int getGreenhouseMinimumThermalBlocks()
    {
        return GREENHOUSE_MINIMUM_THERMAL_BLOCKS.get();
    }

    public static double getCellarMinimumThermalCoverage()
    {
        return CELLAR_MINIMUM_THERMAL_COVERAGE.get();
    }

    public static GreenhouseTierMode getGreenhouseTierMode()
    {
        return greenhouseRuleSet.tierMode();
    }

    public static List<CellarLevel> getCellarPreservationLevels()
    {
        return cellarPreservationLevels;
    }

    private static void onConfigLoading(ModConfigEvent.Loading event)
    {
        if (event.getConfig().getSpec() == SPEC)
        {
            refreshCaches();
        }
    }

    private static void onConfigReloading(ModConfigEvent.Reloading event)
    {
        if (event.getConfig().getSpec() == SPEC)
        {
            refreshCaches();
        }
    }

    private static Map<Item, Float> parseItemFactors(List<? extends String> entries, String keyName)
    {
        final Map<Item, Float> parsed = new LinkedHashMap<>();
        for (String rawEntry : entries)
        {
            final String entry = rawEntry.trim();
            final int separator = entry.indexOf('=');
            if (separator <= 0 || separator >= entry.length() - 1)
            {
                LOGGER.warn("Ignoring invalid {} entry '{}': expected modid:item=factor", keyName, entry);
                continue;
            }

            final ResourceLocation itemId = parseResourceLocation(entry.substring(0, separator).trim());
            if (itemId == null)
            {
                LOGGER.warn("Ignoring invalid {} entry '{}': bad item id", keyName, entry);
                continue;
            }

            final float factor;
            try
            {
                factor = Float.parseFloat(entry.substring(separator + 1).trim());
            }
            catch (NumberFormatException e)
            {
                LOGGER.warn("Ignoring invalid {} entry '{}': bad factor", keyName, entry);
                continue;
            }

            if (!Float.isFinite(factor) || factor <= 0f)
            {
                LOGGER.warn("Ignoring invalid {} entry '{}': factor must be > 0", keyName, entry);
                continue;
            }

            final Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item == null)
            {
                LOGGER.warn("Ignoring invalid {} entry '{}': item not found", keyName, entry);
                continue;
            }
            parsed.put(item, factor);
        }
        return Map.copyOf(parsed);
    }

    private static GreenhouseRuleSet parseGreenhouseRuleSet()
    {
        final List<ThermalWallDefinition> thermalWalls = parseThermalWalls(GREENHOUSE_THERMAL_WALLS.get());
        final List<BlockMatcher> sealOnlyWalls = parseBlockMatchers(GREENHOUSE_SEAL_ONLY_WALLS.get(), "greenhouseSealOnlyWalls");
        final List<BlockMatcher> alwaysValidWalls = parseBlockMatchers(GREENHOUSE_ALWAYS_VALID_WALLS.get(), "greenhouseAlwaysValidWalls");
        final GreenhouseTierMode tierMode = GreenhouseTierMode.parse(GREENHOUSE_TIER_MODE.get());
        return new GreenhouseRuleSet(
            List.copyOf(thermalWalls),
            List.copyOf(sealOnlyWalls),
            List.copyOf(alwaysValidWalls),
            tierMode
        );
    }

    private static List<ThermalWallDefinition> parseThermalWalls(List<? extends String> entries)
    {
        final List<ThermalWallDefinition> parsed = new ArrayList<>();
        final Set<String> seenMatchers = new HashSet<>();
        for (String rawEntry : entries)
        {
            final String entry = rawEntry.trim();
            final int separator = entry.indexOf('=');
            if (separator <= 0 || separator >= entry.length() - 1)
            {
                LOGGER.warn("Ignoring invalid greenhouseThermalWalls entry '{}': expected block_or_tag=tier[,controlBonus]", entry);
                continue;
            }

            final String matcherToken = entry.substring(0, separator).trim();
            final BlockMatcher matcher = parseBlockMatcher(matcherToken, "greenhouseThermalWalls");
            if (matcher == null)
            {
                continue;
            }

            final String matcherKey = matcher.describe();
            if (!seenMatchers.add(matcherKey))
            {
                LOGGER.warn("Ignoring duplicate greenhouseThermalWalls matcher '{}'", matcherKey);
                continue;
            }

            final String[] parts = entry.substring(separator + 1).trim().split(",");
            if (parts.length == 0 || parts.length > 2)
            {
                LOGGER.warn("Ignoring invalid greenhouseThermalWalls entry '{}': expected tier[,controlBonus]", entry);
                continue;
            }

            final int tier;
            try
            {
                tier = Integer.parseInt(parts[0].trim());
            }
            catch (NumberFormatException e)
            {
                LOGGER.warn("Ignoring invalid greenhouseThermalWalls entry '{}': bad tier", entry);
                continue;
            }

            if (tier <= 0)
            {
                LOGGER.warn("Ignoring invalid greenhouseThermalWalls entry '{}': tier must be > 0", entry);
                continue;
            }

            final GreenhouseRuleMetadata metadata = inferGreenhouseRuleMetadata(matcher.describe());
            final int controlBonus;
            if (parts.length == 2)
            {
                try
                {
                    controlBonus = Integer.parseInt(parts[1].trim());
                }
                catch (NumberFormatException e)
                {
                    LOGGER.warn("Ignoring invalid greenhouseThermalWalls entry '{}': bad controlBonus", entry);
                    continue;
                }
            }
            else
            {
                controlBonus = 0;
            }

            if (controlBonus < 0)
            {
                LOGGER.warn("Ignoring invalid greenhouseThermalWalls entry '{}': controlBonus must be >= 0", entry);
                continue;
            }

            parsed.add(new ThermalWallDefinition(tier, controlBonus, metadata.displayNameKey(), metadata.greenhouseTypeId(), matcher));
        }
        return parsed;
    }

    private static List<BlockMatcher> parseBlockMatchers(List<? extends String> entries, String keyName)
    {
        final List<BlockMatcher> parsed = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (String rawEntry : entries)
        {
            final BlockMatcher matcher = parseBlockMatcher(rawEntry.trim(), keyName);
            if (matcher == null)
            {
                continue;
            }
            if (seen.add(matcher.describe()))
            {
                parsed.add(matcher);
            }
            else
            {
                LOGGER.warn("Ignoring duplicate {} matcher '{}'", keyName, matcher.describe());
            }
        }
        return parsed;
    }

    @Nullable
    private static BlockMatcher parseBlockMatcher(String token, String keyName)
    {
        if (token.isEmpty())
        {
            LOGGER.warn("Ignoring empty {} entry", keyName);
            return null;
        }

        if (token.charAt(0) == '#')
        {
            final ResourceLocation tagId = parseResourceLocation(token.substring(1).trim());
            if (tagId == null)
            {
                LOGGER.warn("Ignoring invalid {} tag '{}'", keyName, token);
                return null;
            }
            return new TagBlockMatcher(tagId, TagKey.create(Registries.BLOCK, tagId));
        }

        final ResourceLocation blockId = parseResourceLocation(token);
        if (blockId == null)
        {
            LOGGER.warn("Ignoring invalid {} block '{}'", keyName, token);
            return null;
        }
        final Block block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
        if (block == null)
        {
            LOGGER.warn("Ignoring invalid {} block '{}': block not found", keyName, token);
            return null;
        }
        return new DirectBlockMatcher(blockId, block);
    }

    @Nullable
    private static ResourceLocation parseResourceLocation(String value)
    {
        try
        {
            return new ResourceLocation(value);
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private static GreenhouseRuleMetadata inferGreenhouseRuleMetadata(String matcherDescription)
    {
        final String normalized = matcherDescription.charAt(0) == '#'
            ? matcherDescription.substring(1)
            : matcherDescription;
        if (normalized.contains("stainless_steel_greenhouse"))
        {
            return new GreenhouseRuleMetadata("screen.firmalife_greenhouse_patch.greenhouse.stainless_steel", new ResourceLocation("firmalife", "stainless_steel"));
        }
        if (normalized.contains("all_iron_greenhouse") || normalized.contains("iron_greenhouse"))
        {
            return new GreenhouseRuleMetadata("screen.firmalife_greenhouse_patch.greenhouse.iron", new ResourceLocation("firmalife", "iron"));
        }
        if (normalized.contains("all_copper_greenhouse") || normalized.contains("copper_greenhouse"))
        {
            return new GreenhouseRuleMetadata("screen.firmalife_greenhouse_patch.greenhouse.copper", new ResourceLocation("firmalife", "copper"));
        }
        if (normalized.contains("all_treated_wood_greenhouse") || normalized.contains("treated_wood_greenhouse"))
        {
            return new GreenhouseRuleMetadata("screen.firmalife_greenhouse_patch.greenhouse.wood", new ResourceLocation("firmalife", "treated_wood"));
        }
        return new GreenhouseRuleMetadata("screen.firmalife_greenhouse_patch.greenhouse.custom", null);
    }

    private static List<String> defaultHeatingItems()
    {
        return List.of(
            "minecraft:magma_block=0.1",
            "tfc:rock/magma/andesite=0.1",
            "tfc:rock/magma/basalt=0.1",
            "tfc:rock/magma/dacite=0.1",
            "tfc:rock/magma/diorite=0.1",
            "tfc:rock/magma/gabbro=0.1",
            "tfc:rock/magma/granite=0.1",
            "tfc:rock/magma/rhyolite=0.1"
        );
    }

    private static List<String> defaultCoolingItems()
    {
        return List.of(
            "minecraft:ice=0.01",
            "minecraft:packed_ice=0.1",
            "minecraft:blue_ice=1.0"
        );
    }

    private static List<String> defaultGreenhouseThermalWalls()
    {
        return List.of(
            "#firmalife:all_treated_wood_greenhouse=5",
            "#firmalife:all_copper_greenhouse=10",
            "#firmalife:all_iron_greenhouse=15",
            "#firmalife:stainless_steel_greenhouse=25"
        );
    }

    private static List<String> defaultGreenhouseAlwaysValidWalls()
    {
        return List.of(
            "#firmalife:always_valid_greenhouse_wall",
            "#minecraft:doors",
            "#minecraft:trapdoors"
        );
    }

    private static List<String> defaultGreenhouseSealOnlyWalls()
    {
        return List.of(
            "immersiveengineering:capacitor_lv",
            "immersiveengineering:capacitor_mv",
            "immersiveengineering:capacitor_hv"
        );
    }

    private static List<String> defaultCellarThermalWalls()
    {
        return List.of(
            "#firmalife:cellar_insulation"
        );
    }

    private static List<String> defaultCellarSealOnlyWalls()
    {
        return List.of(
            "immersiveengineering:capacitor_lv",
            "immersiveengineering:capacitor_mv",
            "immersiveengineering:capacitor_hv"
        );
    }

    public record CellarLevel(int maxTemperature, float preservationMultiplier) {}

    public enum GreenhouseTierMode
    {
        WEIGHTED_AVERAGE("weighted_average"),
        MINIMUM("minimum"),
        MAXIMUM("maximum");

        private final String configValue;

        GreenhouseTierMode(String configValue)
        {
            this.configValue = configValue;
        }

        public String configValue()
        {
            return configValue;
        }

        public static GreenhouseTierMode parse(String rawValue)
        {
            for (GreenhouseTierMode mode : values())
            {
                if (mode.configValue.equalsIgnoreCase(rawValue))
                {
                    return mode;
                }
            }
            LOGGER.warn("Unknown greenhouseTierMode '{}', falling back to weighted_average", rawValue);
            return WEIGHTED_AVERAGE;
        }
    }

    public static final class ThermalWallDefinition
    {
        private final int tier;
        private final int controlBonus;
        private final String displayNameKey;
        @Nullable private final ResourceLocation greenhouseTypeId;
        private final BlockMatcher matcher;

        private ThermalWallDefinition(int tier, int controlBonus, String displayNameKey, @Nullable ResourceLocation greenhouseTypeId, BlockMatcher matcher)
        {
            this.tier = tier;
            this.controlBonus = controlBonus;
            this.displayNameKey = displayNameKey;
            this.greenhouseTypeId = greenhouseTypeId;
            this.matcher = matcher;
        }

        public int tier()
        {
            return tier;
        }

        public int controlBonus()
        {
            return controlBonus;
        }

        public String displayNameKey()
        {
            return displayNameKey;
        }

        @Nullable
        public ResourceLocation greenhouseTypeId()
        {
            return greenhouseTypeId;
        }

        @Nullable
        public GreenhouseType greenhouseType()
        {
            return greenhouseTypeId != null ? GreenhouseType.get(greenhouseTypeId) : null;
        }

        private boolean matches(BlockState state)
        {
            return matcher.matches(state);
        }
    }

    private interface BlockMatcher
    {
        boolean matches(BlockState state);

        String describe();
    }

    private record DirectBlockMatcher(ResourceLocation id, Block block) implements BlockMatcher
    {
        @Override
        public boolean matches(BlockState state)
        {
            return state.is(block);
        }

        @Override
        public String describe()
        {
            return id.toString();
        }
    }

    private record TagBlockMatcher(ResourceLocation id, TagKey<Block> tag) implements BlockMatcher
    {
        @Override
        public boolean matches(BlockState state)
        {
            return state.is(tag);
        }

        @Override
        public String describe()
        {
            return "#" + id;
        }
    }

    private record GreenhouseRuleMetadata(String displayNameKey, @Nullable ResourceLocation greenhouseTypeId) {}

    private static final class GreenhouseRuleSet
    {
        private final List<ThermalWallDefinition> thermalWalls;
        private final List<BlockMatcher> sealOnlyWalls;
        private final List<BlockMatcher> alwaysValidWalls;
        private final GreenhouseTierMode tierMode;
        private final Map<Block, ThermalWallDefinition> thermalCache = new ConcurrentHashMap<>();
        private final Set<Block> noThermalCache = ConcurrentHashMap.newKeySet();
        private final Set<Block> sealOnlyCache = ConcurrentHashMap.newKeySet();
        private final Set<Block> noSealOnlyCache = ConcurrentHashMap.newKeySet();
        private final Set<Block> alwaysValidCache = ConcurrentHashMap.newKeySet();
        private final Set<Block> noAlwaysValidCache = ConcurrentHashMap.newKeySet();

        private GreenhouseRuleSet(List<ThermalWallDefinition> thermalWalls, List<BlockMatcher> sealOnlyWalls, List<BlockMatcher> alwaysValidWalls, GreenhouseTierMode tierMode)
        {
            this.thermalWalls = thermalWalls;
            this.sealOnlyWalls = sealOnlyWalls;
            this.alwaysValidWalls = alwaysValidWalls;
            this.tierMode = tierMode;
        }

        private static GreenhouseRuleSet empty()
        {
            return new GreenhouseRuleSet(List.of(), List.of(), List.of(), GreenhouseTierMode.WEIGHTED_AVERAGE);
        }

        @Nullable
        private ThermalWallDefinition findThermalWall(BlockState state)
        {
            final Block block = state.getBlock();
            final ThermalWallDefinition cached = thermalCache.get(block);
            if (cached != null)
            {
                return cached;
            }
            if (noThermalCache.contains(block))
            {
                return null;
            }

            for (ThermalWallDefinition rule : thermalWalls)
            {
                if (rule.matches(state))
                {
                    thermalCache.put(block, rule);
                    return rule;
                }
            }

            noThermalCache.add(block);
            return null;
        }

        private boolean isSealOnlyWall(BlockState state)
        {
            return matchesBooleanRule(state, sealOnlyWalls, sealOnlyCache, noSealOnlyCache);
        }

        private boolean isAlwaysValidWall(BlockState state)
        {
            return matchesBooleanRule(state, alwaysValidWalls, alwaysValidCache, noAlwaysValidCache);
        }

        private boolean matchesBooleanRule(BlockState state, List<BlockMatcher> rules, Set<Block> positiveCache, Set<Block> negativeCache)
        {
            final Block block = state.getBlock();
            if (positiveCache.contains(block))
            {
                return true;
            }
            if (negativeCache.contains(block))
            {
                return false;
            }
            for (BlockMatcher matcher : rules)
            {
                if (matcher.matches(state))
                {
                    positiveCache.add(block);
                    return true;
                }
            }
            negativeCache.add(block);
            return false;
        }

        private GreenhouseTierMode tierMode()
        {
            return tierMode;
        }
    }

    private static final class CellarRuleSet
    {
        private final List<BlockMatcher> thermalWalls;
        private final List<BlockMatcher> sealOnlyWalls;
        private final Set<Block> thermalCache = ConcurrentHashMap.newKeySet();
        private final Set<Block> noThermalCache = ConcurrentHashMap.newKeySet();
        private final Set<Block> sealOnlyCache = ConcurrentHashMap.newKeySet();
        private final Set<Block> noSealOnlyCache = ConcurrentHashMap.newKeySet();

        private CellarRuleSet(List<BlockMatcher> thermalWalls, List<BlockMatcher> sealOnlyWalls)
        {
            this.thermalWalls = thermalWalls;
            this.sealOnlyWalls = sealOnlyWalls;
        }

        private static CellarRuleSet empty()
        {
            return new CellarRuleSet(List.of(), List.of());
        }

        private boolean matches(BlockState state)
        {
            return isThermalWall(state) || isSealOnlyWall(state);
        }

        private boolean isThermalWall(BlockState state)
        {
            return matchesBooleanRule(state, thermalWalls, thermalCache, noThermalCache);
        }

        private boolean isSealOnlyWall(BlockState state)
        {
            return matchesBooleanRule(state, sealOnlyWalls, sealOnlyCache, noSealOnlyCache);
        }

        private boolean matchesBooleanRule(BlockState state, List<BlockMatcher> rules, Set<Block> positiveCache, Set<Block> negativeCache)
        {
            final Block block = state.getBlock();
            if (positiveCache.contains(block))
            {
                return true;
            }
            if (negativeCache.contains(block))
            {
                return false;
            }
            for (BlockMatcher matcher : rules)
            {
                if (matcher.matches(state))
                {
                    positiveCache.add(block);
                    return true;
                }
            }
            negativeCache.add(block);
            return false;
        }
    }
}
