package net.alex.guzhenrenworld.dimension.blissfulland;

import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.SurfaceDimensionTypes;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.biome.SurfacePruning;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.alex.guzhenrenworld.waterways.Waterways;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Music;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.Nullable;

/**
 * 狐仙福地[Fox Immortal Blissful Land]维度的注册键与数据包引导入口：六转奴道蛊仙的仙窍落地而成，中心立着荡魂山。
 * <ul>
 *   <li>维度、维度层级、群系、噪声设置共用路径 {@code guworld:fox_immortal_blissful_land}；维度类型
 *       {@link #DIMENSION_TYPE}（{@code guworld:blissful_land}）留给以后的福地共用。</li>
 *   <li>疆域是以原点为中心、边长 {@link #HALF_SIZE_CHUNKS} × 2 个区块的正方形（一区块记 4 亩，1224² 区块约 599 万
 *       亩）；{@link BoundedNoiseGenerator} 让界外区块整块留空，世界边界由
 *       {@link net.alex.guzhenrenworld.dimension.RegionBorders} 按 {@link #BORDER_SIZE} 施加。</li>
 *   <li>地形是本福地自己的一份 Tectonic 副本，取给原版群系调的变体（{@link Ports#getVariant}），钩子见
 *       {@link #getHooks}（变体在数据生成时才读移植文件，故钩子按需构造而不放静态常量）：不出海，竖直拉伸
 *       {@link #VERTICAL_SCALE} 让山峰最高约 y 250；平地圈、湖和水系刻河见 {@link BlissfulLandTerrain}。</li>
 *   <li>群系照中州，名单与多噪声表见 {@link BlissfulLandBiomes}；{@link #BIOME} 是平地圈的群系，原版平原同款（刷怪表、
 *       草花、矿石、软质圆盘、地下变化），只去掉雕刻器和地表熔岩湖，荡魂山只认它。</li>
 *   <li>水系：{@link #WATERWAYS} 边长同疆域，疆域为四域的 5.7 倍，16 格网格首次建图要 30–60 秒，故用
 *       {@link #WATERWAYS_CELL}（32 格）将耗时降至约四分之一（Alex 定）；平地圈外 {@link #KEEP_OUT} 格内和疆域最外一圈
 *       是墙，河床砾石为主夹黏土和沙。</li>
 *   <li>荡魂山经结构集落在原点区块，见 {@code net.alex.guzhenrenworld.secretrealm.SoulsunderMountain}。</li>
 *   <li>⚠ 维度路径必须与主模组 {@code ModDimensions} 中的同名维度键一致，否则 {@code /guworld enter} 找不到目标维度。
 *       </li>
 * </ul>
 */

public final class FoxImmortalBlissfulLand {

    private static final String NAME = "fox_immortal_blissful_land";
    public static final ResourceKey<DimensionType> DIMENSION_TYPE =
            GuWorld.key(Registries.DIMENSION_TYPE, "blissful_land");
    public static final ResourceKey<Biome> BIOME = GuWorld.key(Registries.BIOME, NAME);
    public static final ResourceKey<NoiseGeneratorSettings> NOISE_SETTINGS =
            GuWorld.key(Registries.NOISE_SETTINGS, NAME);
    public static final ResourceKey<LevelStem> LEVEL_STEM = GuWorld.key(Registries.LEVEL_STEM, NAME);
    public static final ResourceKey<Level> LEVEL = GuWorld.key(Registries.DIMENSION, NAME);
    public static final String TITLE = "狐仙福地";
    public static final int COLOR = 0xF8ACC4;
    public static final int HALF_SIZE_CHUNKS = 612;
    public static final double BORDER_SIZE = HALF_SIZE_CHUNKS * 2 * 16;
    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    public static final int SEA_LEVEL = 63;
    private static final boolean RESPAWN_ANCHORS_WORK = false;
    private static final int NOISE_SIZE_HORIZONTAL = 1;
    private static final int NOISE_SIZE_VERTICAL = 1;
    private static final boolean MOB_GENERATION_DISABLED = false;
    private static final boolean AQUIFERS = true;
    private static final boolean ORE_VEINS = true;
    private static final boolean LEGACY_RANDOM_SOURCE = false;
    private static final float TEMPERATURE = 0.8F;
    private static final float DOWNFALL = 0.4F;
    private static final int SKY_COLOR = 0x78A7FF;
    private static final int FOG_COLOR = 0xC0D8FF;
    private static final int WATER_COLOR = 0x3F76E4;
    private static final int WATER_FOG_COLOR = 0x050533;
    private static final @Nullable Music MUSIC = null;
    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 0.8;
    private static final double KEEP_OUT = BlissfulLandTerrain.MARGIN + 32;
    private static final int WATERWAYS_CELL = 32;
    public static final Waterways.Region WATERWAYS = new Waterways.Region(NAME, BORDER_SIZE, false,
            Waterways.Bed.GRAVEL_CLAY_SAND, KEEP_OUT, true, WATERWAYS_CELL);

    private FoxImmortalBlissfulLand() {}

    public static void registerDimensionType(BootstrapContext<DimensionType> context) {
        context.register(DIMENSION_TYPE, SurfaceDimensionTypes.create(MIN_Y, HEIGHT, RESPAWN_ANCHORS_WORK,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)));
    }

    public static void registerBiome(BootstrapContext<Biome> context) {
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.plainsSpawns(spawns);
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(
                context.lookup(Registries.PLACED_FEATURE), context.lookup(Registries.CONFIGURED_CARVER));
        generation.addFeature(GenerationStep.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
        BiomeDefaultFeatures.addPlainGrass(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        BiomeDefaultFeatures.addPlainVegetation(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation);
        context.register(BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(TEMPERATURE)
                .downfall(DOWNFALL)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(WATER_COLOR)
                        .waterFogColor(WATER_FOG_COLOR)
                        .fogColor(FOG_COLOR)
                        .skyColor(SKY_COLOR)
                        .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                        .backgroundMusic(MUSIC)
                        .build())
                .mobSpawnSettings(spawns.build())
                .generationSettings(generation.build())
                .build());
    }

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        BlissfulLandTerrain.registerNoises(context, NAME);
        RegionBootstraps.registerNoises(context, NAME, getHooks());
    }

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context) {
        RegionBootstraps.registerDensityFunctions(context, NAME, getHooks(), MIN_Y, MIN_Y + HEIGHT);
    }

    private static Region.Hooks getHooks() {
        return new Region.Hooks(DensityFunctions.constant(OCEAN_OFFSET), VERTICAL_SCALE, 0.0,
                Ports.getVariant("vanilla"));
    }

    public static void registerNoiseSettings(BootstrapContext<NoiseGeneratorSettings> context) {
        context.register(NOISE_SETTINGS, new NoiseGeneratorSettings(
                NoiseSettings.create(MIN_Y, HEIGHT, NOISE_SIZE_HORIZONTAL, NOISE_SIZE_VERTICAL),
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                BlissfulLandTerrain.getRouter(context, NAME, WATERWAYS),
                SurfacePruning.getPruned(Replicas.getSurfaceRules(BlissfulLandBiomes.getSurfaceTemplates()),
                        BlissfulLandBiomes.ROSTER),
                List.of(),
                SEA_LEVEL,
                MOB_GENERATION_DISABLED,
                AQUIFERS,
                ORE_VEINS,
                LEGACY_RANDOM_SOURCE
        ));
    }

    public static void registerLevelStem(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        context.register(LEVEL_STEM, new LevelStem(dimensionTypes.getOrThrow(DIMENSION_TYPE),
                new BoundedNoiseGenerator(MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(
                        BlissfulLandBiomes.getParameters(context.lookup(Registries.BIOME)))),
                        context.lookup(Registries.NOISE_SETTINGS).getOrThrow(NOISE_SETTINGS), HALF_SIZE_CHUNKS)));
    }
}
