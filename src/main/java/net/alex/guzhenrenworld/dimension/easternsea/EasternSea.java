package net.alex.guzhenrenworld.dimension.easternsea;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenrenworld.DatapackBootstrap;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.SurfaceDimensionTypes;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.biome.SurfacePruning;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * The Eastern Sea [东海] dimension, one of the four regions beyond the Central Continent [中州], which is the
 * overworld: its keys, and the datapack entries behind them. The region is a warm archipelago, about four fifths
 * water, islands from two kilometers across down to a few dozen blocks.
 *
 * <p>Every key shares the one location {@code guworld:eastern_sea} ({@link #NAME} is its path, the prefix of
 * the region's copy of the ported terrain). {@link #registerDimensionType}, {@link #registerNoiseSettings} and
 * {@link #registerLevelStem} are the bootstraps {@code datagen.DatapackProvider} runs. The dimension type is the
 * overworld's (sky, effects, beds, raids, monster light, and its {@link #HEIGHT} blocks from y {@link #MIN_Y} up to
 * {@link #TOP_Y}), kept under its own key so a pack that reshapes the overworld leaves the region alone. The noise
 * column runs the 4-block cells Tectonic takes beside Terralith, {@code size_vertical} 1, over the whole height.
 *
 * <p>The noise settings run the region's copy of Tectonic's overworld router ({@link Ports#getNoiseRouter}) over
 * {@link EasternSeaTerrain}'s hooks, with the ported surface rules ahead of vanilla's, both cut to
 * {@link EasternSeaLayout#ROSTER} ({@link SurfacePruning}), sea level {@link #SEA_LEVEL}, aquifers and ore veins;
 * the level stem pairs them with {@link EasternSeaLayout}'s multi-noise source. The settings name no spawn target:
 * the overworld stays the spawn dimension. Every structure checks its biome tags and none of this region's biomes is
 * in one, so vanilla drops every structure set before the first chunk generates: no vanilla structure generates or
 * costs time here.
 *
 * <p>The region is finite: {@link #BORDER_SIZE} is the side of its square world border around 0, 0, eight
 * kilometers, which {@link net.alex.guzhenrenworld.dimension.RegionBorders} applies to {@link #LEVEL}.
 *
 * <p>⚠ Every key shares the location of Guzhenren's level key
 * {@code net.alex.guzhenren.registry.world.ModDimensions#EASTERN_SEA}, the admin travel there (Guzhenren
 * owns travel): {@code /guworld enter guworld:eastern_sea} lands players on the surface above 0, 0, the sea when
 * the origin is water. A renamed key here leaves that command pointing at nothing until that key is renamed too.
 *
 * @author Alex
 * @version 1.0.0
 * @see EasternSeaTerrain
 * @since 1.0.0
 */

public final class EasternSea {

    public static final String NAME = "eastern_sea";
    private static final ResourceLocation ID = GuWorld.id(NAME);
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceKey<NoiseGeneratorSettings> NOISE_SETTINGS =
            ResourceKey.create(Registries.NOISE_SETTINGS, ID);
    public static final ResourceKey<LevelStem> LEVEL_STEM = ResourceKey.create(Registries.LEVEL_STEM, ID);
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ID);
    public static final double BORDER_SIZE = 8192.0;
    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    public static final int TOP_Y = MIN_Y + HEIGHT;
    public static final int SEA_LEVEL = 63;
    private static final boolean RESPAWN_ANCHORS_WORK = false;
    private static final int NOISE_SIZE_HORIZONTAL = 1;
    private static final int NOISE_SIZE_VERTICAL = 1;
    private static final boolean MOB_GENERATION_DISABLED = false;
    private static final boolean AQUIFERS = true;
    private static final boolean ORE_VEINS = true;
    private static final boolean LEGACY_RANDOM_SOURCE = false;

    private EasternSea() {}

    public static void registerDimensionType(DatapackBootstrap<DimensionType> context) {
        context.register(DIMENSION_TYPE, SurfaceDimensionTypes.create(MIN_Y, HEIGHT, RESPAWN_ANCHORS_WORK,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)));
    }

    public static void registerNoiseSettings(DatapackBootstrap<NoiseGeneratorSettings> context) {
        List<Replicas.Template> replicas = new ArrayList<>(Replicas.SHARED);
        replicas.addAll(EasternSeaRoster.REPLICAS);
        context.register(NOISE_SETTINGS, new NoiseGeneratorSettings(
                NoiseSettings.create(MIN_Y, HEIGHT, NOISE_SIZE_HORIZONTAL, NOISE_SIZE_VERTICAL),
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                Ports.getNoiseRouter(context, NAME),
                SurfacePruning.getPruned(SurfaceRules.sequence(Ports.getSurfaceRules(),
                        Replicas.getSurfaceRules(replicas)), EasternSeaLayout.ROSTER),
                List.of(),
                SEA_LEVEL,
                MOB_GENERATION_DISABLED,
                AQUIFERS,
                ORE_VEINS,
                LEGACY_RANDOM_SOURCE
        ));
    }

    public static void registerLevelStem(DatapackBootstrap<LevelStem> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(
                new Climate.ParameterList<>(EasternSeaLayout.getParameters(context.lookup(Registries.BIOME))));
        context.register(LEVEL_STEM, new LevelStem(
                dimensionTypes.getOrThrow(DIMENSION_TYPE),
                new NoiseBasedChunkGenerator(biomeSource, noiseSettings.getOrThrow(NOISE_SETTINGS))
        ));
    }
}
