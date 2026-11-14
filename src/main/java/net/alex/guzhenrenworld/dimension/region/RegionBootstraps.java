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
 * 每个 {@link Region} 的数据包条目。
 * <ul>
 *   <li>{@code datagen.DatapackProvider} 对 {@link net.alex.guzhenrenworld.dimension.Regions#ALL} 逐域注册噪声、密度函
 *       数、噪声设置、层级；维度类型只注册一次。</li>
 *   <li>维度类型 {@link Region#DIMENSION_TYPE} 照搬主世界（天空、效果、床、袭击、刷怪光照、高度范围），键归本模组，整合
 *       包改主世界不波及四域；四域无差别，共用一条。</li>
 *   <li>噪声柱：Tectonic 与 Terralith 同装时的 4 格单元，{@code size_vertical} 1，覆盖全高；{@link ColumnCaches} 包住移
 *       植列函数（含改写的），每列只算一次。</li>
 *   <li>噪声设置：本域 Tectonic 路由副本（{@link Ports#getNoiseRouter}）读 {@link #registerDensityFunctions} 填的钩子，
 *       外包水系刻河（{@link WaterwaysDensity}）；移植表层规则先于原版，都按名单裁剪（{@link SurfacePruning}）；含本域
 *       海平面、含水层、矿脉；层级配上多噪声群系源。</li>
 *   <li>不设出生目标，出生维度仍是主世界；结构都查群系标签而本域群系不在其中，原版在首个区块生成前丢掉全部结构集
 *       （{@code ChunkGeneratorStructureState.createForNormal}），不生成、不耗时。</li>
 *   <li>钩子 {@code guworld:terrain/<region>/hook/*}：{@code ocean_offset}、{@code vertical_scale}、{@code dunes} 各域
 *       自定（{@link Region.Hooks}）；{@code min_y}、{@code max_y} 为 Tectonic 高度上下限，取维度底与顶。</li>
 *   <li>{@code jungle_pillars}、{@code underground_rivers}（含 {@code underground_river_water}）、
 *       {@code lava_tunnels}：三项 Tectonic 特性，各一个常量开关（{@link #JUNGLE_PILLARS}、
 *       {@link #UNDERGROUND_RIVERS}、{@link #LAVA_TUNNELS}），四域全关；开则钩子指回移植函数，关则指向 0，零开销；只注
 *       册路由够得到的移植函数与噪声（{@link #SWITCHES} 列出开关函数），关掉的特性文件不进数据包
 *       （{@link Ports#registerDensityFunctions}）。</li>
 *   <li>丛林石柱：约 11 格粗、高至 100 格、平顶直壁的挤出柱，正是 Alex 为各域地貌否掉的规整柱形；地下河在山谷底下 y 32
 *       至 72 挖水道、给含水层充水，熔岩隧道沿底部走，二者在整列每个单元角点或方块读噪声，约占噪声填充四分之一。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Region
 * @since 1.0.0
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
        Ports.registerNoises(context, region.name(), getPatch(region), getHooked());
    }

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context, Region region) {
        String name = region.name();
        Region.Hooks hooks = region.hooks();
        Ports.registerDensityFunctions(context, name, getPatch(region), getHooked());
        context.register(Ports.getHookKey(name, "ocean_offset"), hooks.oceanOffset());
        TerrainHooks.constant(context, name, "vertical_scale", hooks.verticalScale());
        TerrainHooks.constant(context, name, "min_y", Region.MIN_Y);
        TerrainHooks.constant(context, name, "max_y", Region.TOP_Y);
        TerrainHooks.constant(context, name, "dunes", hooks.dunes());
        TerrainHooks.constant(context, name, "jungle_pillars", JUNGLE_PILLARS ? 1.0 : 0.0);
        SWITCHES.forEach(feature -> TerrainHooks.switched(context, name, feature.hook(), feature.ported(),
                feature.on()));
    }

    private static BiFunction<String, JsonElement, JsonElement> getPatch(Region region) {
        BiFunction<String, JsonElement, JsonElement> columnCaches = ColumnCaches.ofPorted();
        Region.Hooks hooks = region.hooks();
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
