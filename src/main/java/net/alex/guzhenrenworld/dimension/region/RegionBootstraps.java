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
 * The datapack entries every {@link Region} has, which {@code datagen.DatapackProvider} runs for each of
 * {@link net.alex.guzhenrenworld.dimension.Regions#ALL}: the noises, the density functions, the noise settings and
 * the level stem, and once for all of them the dimension type.
 *
 * <p>The dimension type, {@link Region#DIMENSION_TYPE}, is the overworld's (sky, effects, beds, raids, monster light,
 * and the regions' height range) under this mod's own key, so a pack that reshapes the overworld leaves the regions
 * alone; the regions differ in nothing it holds, so all four point at the one entry. The noise column runs the
 * 4-block cells Tectonic takes beside Terralith, {@code size_vertical} 1, over the whole height.
 *
 * <p>The noise settings run the region's copy of Tectonic's overworld router ({@link Ports#getNoiseRouter}) over the
 * hooks {@link #registerDensityFunctions} fills, wrapped in the waterways' carving ({@link WaterwaysDensity}), with
 * the ported surface rules ahead of vanilla's, both cut to the region's roster ({@link SurfacePruning}), the region's
 * sea level, aquifers and ore veins; the level stem pairs them with the region's multi-noise source. The settings name
 * no spawn target: the overworld stays the spawn dimension. Every structure checks its biome tags and none of a
 * region's biomes is in one, so vanilla drops every structure set before the first chunk generates
 * ({@code ChunkGeneratorStructureState.createForNormal}): no vanilla structure generates or costs time in a region.
 *
 * <p>The hooks, {@code guworld:terrain/<region>/hook/*}:
 * <ul>
 *   <li>{@code ocean_offset}, {@code vertical_scale} and {@code dunes}: the region's own ({@link Region.Hooks}).</li>
 *   <li>{@code min_y} and {@code max_y}: Tectonic's height limits, the dimension's floor and top.</li>
 *   <li>{@code jungle_pillars}, {@code underground_rivers} with {@code underground_river_water}, and
 *   {@code lava_tunnels}: three Tectonic features, each switched by one constant here ({@link #JUNGLE_PILLARS},
 *   {@link #UNDERGROUND_RIVERS}, {@link #LAVA_TUNNELS}) and off in every region. Jungle pillars are extruded discs
 *   about 11 blocks wide and up to 100 tall with flat tops and sheer sides, the regular pillar shape Alex turned down
 *   for the regions' own landforms. Underground rivers carve water channels at y 32 to 72 under mountain valleys and
 *   flood the aquifers there, lava tunnels run along the floor; both read their noises at every cell corner or block
 *   of the column, about a quarter of the noise fill. A switch that is on points its hook back at the ported
 *   function, one that is off at zero, so an off feature costs nothing: a region registers only the ported functions
 *   and noises its router reaches, the switched-on features' among them ({@link #SWITCHES} names the functions),
 *   so an off feature's files are not in the datapack either ({@link Ports#registerDensityFunctions}).</li>
 * </ul>
 * {@link ColumnCaches} wraps the ported column functions, the ones a region reshapes included, so a noise chunk
 * computes each once per column.
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
