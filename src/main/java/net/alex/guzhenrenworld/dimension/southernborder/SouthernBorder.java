package net.alex.guzhenrenworld.dimension.southernborder;

import java.util.List;
import java.util.OptionalLong;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.terrain.HeightfieldRouter;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The Southern Border [南疆] dimension, one of the four regions beyond the Central Continent [中州], which is
 * the overworld: its keys, and the datapack entries behind them.
 *
 * <p>Every key shares the one location {@code guworld:southern_border}. {@link #registerDimensionType}, {@link
 * #registerNoiseSettings} and {@link #registerLevelStem} are the bootstraps {@code datagen.DatapackProvider} runs. The
 * dimension type is the overworld's (sky, effects, beds, raids, monster light) with a taller column, -64..383 ({@link
 * #MIN_Y}, {@link #HEIGHT}), so peaks, capped at y 320, keep 63 blocks of air; both are read off the router's noise
 * column, which the type must cover. The noise settings are a {@link HeightfieldRouter} over {@link Terrain}, with
 * {@link Surface}'s rules ahead of vanilla's overworld ones, sea level {@link Rivers#SEA_LEVEL}, aquifers and ore
 * veins; the level stem pairs them with the multi-noise biome source of {@link BiomeLayout}. The settings name no spawn
 * target: the overworld stays the spawn dimension. The region's parts name their noises and density functions under
 * that location through {@link #noiseKey} and {@link #functionKey}, the landform ones one folder deeper through
 * {@link #landformNoiseKey} and {@link #landformFunctionKey}.
 *
 * <p>Built-in structure sets apply as in any noise dimension, but every structure checks its biome tags and none of
 * this region's own biomes is in one, so no vanilla structure generates here, strongholds included.
 *
 * <p>The region is finite: {@link #BORDER_SIZE} is the side of its square world border around 0, 0, eight kilometers
 * (about 262,000 chunks), which {@link net.alex.guzhenrenworld.dimension.RegionBorders} applies to {@link #LEVEL}. The
 * vanilla border wall stands in until the regional walls [界壁] are built (wiki《南疆》).
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
    private static final int MIN_Y = HeightfieldRouter.NOISE_SETTINGS.minY();
    private static final int HEIGHT = HeightfieldRouter.NOISE_SETTINGS.height();

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
                HeightfieldRouter.NOISE_SETTINGS,
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                Terrain.createRouter(context.lookup(Registries.DENSITY_FUNCTION),
                        context.lookup(Registries.NOISE)),
                Surface.getRules(),
                List.of(),
                Rivers.SEA_LEVEL,
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
                BiomeLayout.getParameters(context.lookup(Registries.BIOME)));
        context.register(LEVEL_STEM, new LevelStem(
                dimensionTypes.getOrThrow(DIMENSION_TYPE),
                new NoiseBasedChunkGenerator(biomeSource, noiseSettings.getOrThrow(NOISE_SETTINGS))
        ));
    }

    static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
        return ResourceKey.create(Registries.NOISE, getPartId(name));
    }

    static ResourceKey<DensityFunction> functionKey(String name) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, getPartId(name));
    }

    static ResourceKey<NormalNoise.NoiseParameters> landformNoiseKey(String name) {
        return noiseKey("landform/" + name);
    }

    static ResourceKey<DensityFunction> landformFunctionKey(String name) { return functionKey("landform/" + name); }

    private static ResourceLocation getPartId(String name) { return ID.withSuffix("/" + name); }
}
