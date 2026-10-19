package net.alex.guzhenrenworld.dimension.southernborder;

import java.util.List;
import java.util.OptionalLong;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
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
 * The Southern Border [南疆] dimension, one of the four regions beyond the Central Continent [中州], which is
 * the overworld: its keys, and the datapack entries behind them.
 *
 * <p>Every key shares the one location {@code guworld:southern_border}. {@link #registerDimensionType}, {@link
 * #registerNoiseSettings} and {@link #registerLevelStem} are the bootstraps {@code datagen.DatapackProvider} runs. The
 * dimension type is the overworld's (sky, effects, beds, raids, monster light) with a taller column, y {@link #MIN_Y}
 * up to {@link #MAX_Y}: peaks reach about y 272 ({@link Terrain}), and a dimension's floor and height must both be
 * multiples of 16, so the build limit is 367 rather than any value in between. The noise column runs Terralith's
 * 4-block cells, {@code size_vertical} 1, {@link #NOISE_HEIGHT} blocks up from the floor, through y 335: the noise
 * height steps by 16, and that is the first step past {@link Terrain}'s top slide. Every cell above the slide is
 * air, so the 32 blocks up to the build limit are never sampled and the terrain is the same.
 *
 * <p>The noise settings run Terralith's overworld router ({@link Ports#getNoiseRouter}) over {@link Terrain}'s hooks,
 * with the ported surface rules ahead of vanilla's, sea level 63, aquifers and ore veins; the level stem pairs them
 * with {@link BiomeLayout}'s multi-noise source. The settings name no spawn target: the overworld stays the spawn
 * dimension. Built-in structure sets apply as in any noise dimension, but every structure checks its biome tags and
 * none of this region's biomes is in one, so no vanilla structure generates here.
 *
 * <p>The region is finite: {@link #BORDER_SIZE} is the side of its square world border around 0, 0, eight kilometers
 * (about 262,000 chunks), which {@link net.alex.guzhenrenworld.dimension.RegionBorders} applies to {@link #LEVEL}.
 *
 * <p>⚠ Every key shares the location of Guzhenren's level key
 * {@code net.alex.guzhenren.registry.world.ModDimensions#SOUTHERN_BORDER}, the admin travel there (Guzhenren
 * owns travel): {@code /guworld enter guworld:southern_border} lands players on the surface above 0, 0. A renamed
 * key here leaves that command pointing at nothing until that key is renamed too.
 *
 * @author Alex
 * @version 1.0.0
 * @see Terrain
 * @since 1.0.0
 */

public final class SouthernBorder {

    private static final ResourceLocation ID = GuWorld.id("southern_border");
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceKey<NoiseGeneratorSettings> NOISE_SETTINGS =
            ResourceKey.create(Registries.NOISE_SETTINGS, ID);
    public static final ResourceKey<LevelStem> LEVEL_STEM = ResourceKey.create(Registries.LEVEL_STEM, ID);
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, ID);
    public static final double BORDER_SIZE = 8192.0;
    public static final int MIN_Y = -64;
    public static final int MAX_Y = 367;
    private static final int HEIGHT = MAX_Y + 1 - MIN_Y;
    private static final int NOISE_HEIGHT = Mth.roundToward(Terrain.TOP_SLIDE_END + 1 - MIN_Y, 16);
    private static final int SEA_LEVEL = 63;

    private SouthernBorder() {}

    public static void registerDimensionType(BootstrapContext<DimensionType> context) {
        context.register(DIMENSION_TYPE, new DimensionType(
                OptionalLong.empty(),
                true,
                false,
                false,
                true,
                1.0,
                true,
                false,
                MIN_Y,
                HEIGHT,
                HEIGHT,
                BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                0.0F,
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
        ));
    }

    public static void registerNoiseSettings(BootstrapContext<NoiseGeneratorSettings> context) {
        context.register(NOISE_SETTINGS, new NoiseGeneratorSettings(
                NoiseSettings.create(MIN_Y, NOISE_HEIGHT, 1, 1),
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                Ports.getNoiseRouter(context),
                SurfaceRules.sequence(Ports.getSurfaceRules(), Replicas.getSurfaceRules()),
                List.of(),
                SEA_LEVEL,
                false,
                true,
                true,
                false
        ));
    }

    public static void registerLevelStem(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(
                new Climate.ParameterList<>(BiomeLayout.getParameters(context.lookup(Registries.BIOME))));
        context.register(LEVEL_STEM, new LevelStem(
                dimensionTypes.getOrThrow(DIMENSION_TYPE),
                new NoiseBasedChunkGenerator(biomeSource, noiseSettings.getOrThrow(NOISE_SETTINGS))
        ));
    }
}
