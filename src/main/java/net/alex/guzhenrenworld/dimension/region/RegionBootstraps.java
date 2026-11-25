package net.alex.guzhenrenworld.dimension.region;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import net.alex.guzhenrenworld.SurfaceDimensionTypes;
import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.biome.SurfacePruning;
import net.alex.guzhenrenworld.biome.TerrainHooks;
import net.alex.guzhenrenworld.waterways.WaterwaysDensity;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * 四域（{@link Region}）数据包条目的统一注册引导逻辑。
 * <ul>
 *   <li>由 {@link net.alex.guzhenrenworld.datagen.DatapackProvider} 遍历
 *       {@link net.alex.guzhenrenworld.dimension.Regions#ALL}，逐域调用本类注册噪声、密度函数、噪声生成设置与维度层级；
 *       共用的维度类型 {@link Region#DIMENSION_TYPE} 则仅注册一次，属性对齐原版主世界但使用独立键名以防整合包修改主世界
 *       时波及四域。</li>
 *   <li>噪声生成设置采用与 Tectonic + Terralith 一致的 4×4 水平网格单元（{@code size_vertical = 1}），将本域的
 *       Tectonic 噪声路由副本（{@link Ports#getNoiseRouter}）外包水系河道雕刻（{@link WaterwaysDensity}），并拼接经
 *       {@link SurfacePruning} 按本域群系名单裁剪后的 Terralith 与原版复刻表层规则
 *       （{@link Replicas#getSurfaceRules}）。</li>
 *   <li>维度不设置出生参数（玩家默认出生在主世界）；由于本域群系均不加入原版结构群系标签，原版在首个区块生成前会自动过
 *       滤掉所有不匹配的结构集（{@code ChunkGeneratorStructureState.createForNormal}），实现零结构探测开销。</li>
 *   <li>{@link #registerDensityFunctions} 将各域自定义的 {@code ocean_offset}、{@code vertical_scale}、{@code dunes}
 *       （{@link Region.Hooks}）及维度高度上下限（{@code min_y}、{@code max_y}）注册到
 *       {@code guworld:terrain/<region>/hook/*}，同时对移植函数应用域重塑补丁与 {@link ColumnCaches} 列缓存。</li>
 *   <li>噪声与密度函数的注册另有按名字、钩子与高度上下限的重载，狐仙福地（{@code dimension.blissfulland}）借它注册自己
 *       的 Tectonic 副本，开关与列缓存同四域。</li>
 *   <li>{@link #JUNGLE_PILLARS}、{@link #UNDERGROUND_RIVERS} 与 {@link #LAVA_TUNNELS} 三项高开销 Tectonic 特性在四域中
 *       统一关闭，通过 {@link TerrainHooks} 将对应钩子绑定为常量 {@code 0}；配合 {@link Ports#registerDensityFunctions}
 *       的可达性裁剪，未启用特性的密度函数与噪声不会写入数据包。</li>
 * </ul>
 */

public final class RegionBootstraps {

    private static final boolean RESPAWN_ANCHORS_WORK = false;
    private static final int NOISE_SIZE_HORIZONTAL = 1;
    private static final int NOISE_SIZE_VERTICAL = 1;
    private static final boolean MOB_GENERATION_DISABLED = false;
    private static final boolean AQUIFERS = true;
    private static final boolean ORE_VEINS = true;
    private static final boolean LEGACY_RANDOM_SOURCE = false;
    private static final boolean JUNGLE_PILLARS = false;
    private static final boolean UNDERGROUND_RIVERS = false;
    private static final boolean LAVA_TUNNELS = false;
    private static final List<Switch> SWITCHES = List.of(
            new Switch("underground_rivers", "terrain/tectonic/underground_river/total", UNDERGROUND_RIVERS),
            new Switch("underground_river_water", "terrain/tectonic/underground_river/parameters",
                    UNDERGROUND_RIVERS),
            new Switch("lava_tunnels", "terrain/tectonic/lava_tunnel/total", LAVA_TUNNELS));

    private RegionBootstraps() {}

    public static void registerDimensionType(BootstrapContext<DimensionType> context) {
        context.register(Region.DIMENSION_TYPE, SurfaceDimensionTypes.create(Region.MIN_Y, Region.HEIGHT,
                RESPAWN_ANCHORS_WORK, BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)));
    }

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context, Region region) {
        registerNoises(context, region.name(), region.hooks());
    }

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context, String name,
            Region.Hooks hooks) {
        Ports.registerNoises(context, name, getPatch(hooks), getHooked());
    }

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context, Region region) {
        registerDensityFunctions(context, region.name(), region.hooks(), Region.MIN_Y, Region.TOP_Y);
    }

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context, String name,
            Region.Hooks hooks, int minY, int topY) {
        Ports.registerDensityFunctions(context, name, getPatch(hooks), getHooked());
        context.register(Ports.getHookKey(name, "ocean_offset"), hooks.oceanOffset());
        TerrainHooks.constant(context, name, "vertical_scale", hooks.verticalScale());
        TerrainHooks.constant(context, name, "min_y", minY);
        TerrainHooks.constant(context, name, "max_y", topY);
        TerrainHooks.constant(context, name, "dunes", hooks.dunes());
        TerrainHooks.constant(context, name, "jungle_pillars", JUNGLE_PILLARS ? 1.0 : 0.0);
        SWITCHES.forEach(feature -> TerrainHooks.switched(context, name, feature.hook(), feature.ported(),
                feature.on()));
    }

    private static BiFunction<String, JsonElement, JsonElement> getPatch(Region.Hooks hooks) {
        BiFunction<String, JsonElement, JsonElement> columnCaches = ColumnCaches.ofPorted();
        return (function, json) -> columnCaches.apply(function, hooks.reshape().apply(function, json));
    }

    private static Set<String> getHooked() {
        return SWITCHES.stream().filter(Switch::on).map(Switch::ported).collect(Collectors.toUnmodifiableSet());
    }

    public static void registerNoiseSettings(BootstrapContext<NoiseGeneratorSettings> context, Region region) {
        List<Replicas.Template> replicas = new ArrayList<>(Replicas.SHARED);
        replicas.addAll(region.biomes().replicas());
        context.register(region.noiseSettings(), new NoiseGeneratorSettings(
                NoiseSettings.create(Region.MIN_Y, Region.HEIGHT, NOISE_SIZE_HORIZONTAL, NOISE_SIZE_VERTICAL),
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                WaterwaysDensity.getCarved(context, Ports.getNoiseRouter(context, region.name()),
                        region.waterways()),
                SurfacePruning.getPruned(SurfaceRules.sequence(Ports.getSurfaceRules(),
                        Replicas.getSurfaceRules(replicas)), region.biomes().roster()),
                List.of(),
                region.seaLevel(),
                MOB_GENERATION_DISABLED,
                AQUIFERS,
                ORE_VEINS,
                LEGACY_RANDOM_SOURCE
        ));
    }

    public static void registerLevelStem(BootstrapContext<LevelStem> context, Region region) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(
                region.biomes().parameters().apply(context.lookup(Registries.BIOME))));
        context.register(region.levelStem(), new LevelStem(
                dimensionTypes.getOrThrow(Region.DIMENSION_TYPE),
                new NoiseBasedChunkGenerator(biomeSource, noiseSettings.getOrThrow(region.noiseSettings()))
        ));
    }

    private record Switch(String hook, String ported, boolean on) {}
}
