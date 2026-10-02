package net.alex.guzhenrenworld.datagen;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.registry.WorldBiomeTags;
import net.alex.guzhenrenworld.registry.WorldDimensions;
import net.alex.guzhenrenworld.registry.WorldFeatures;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The single provider for every datapack registry this mod writes.
 *
 * <p>Extends {@link net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider}. Builds in one
 * {@code RegistrySetBuilder} the Treasure Yellow Heaven dimension -- its dimension type, its one biome
 * and its level stem, a flat generator with no layers, so the dimension is pure void -- and the Spirit
 * Spring [元泉] worldgen: a configured and a placed feature per {@link WorldFeatures} placement, and the
 * biome modifiers adding them to {@link WorldBiomeTags#SPIRIT_SPRING_GENERATES}, the surface one at
 * {@code TOP_LAYER_MODIFICATION} (after vegetation) and the cave one at {@code UNDERGROUND_DECORATION}.
 * The tag provider takes {@code getRegistryProvider()} from this instance, not the plain lookup, so the
 * tag pass sees the biome this run generates.
 *
 * <p>⚠ There can only be one per data run. The builtin-entries provider reports a fixed name, so a
 * second instance fails datagen outright; add a registry to this one's builder instead.
 *
 * <p>⚠ {@link #SPIRIT_SPRING_RARITY} is Alex's pick (2026-09-23): desert-well scale, but across 39 land
 * biomes instead of one. ⚠ {@link #SPIRIT_SPRING_UNDERGROUND_RARITY} is Alex's constraint (2026-09-26):
 * strictly rarer than the surface roll. 3000 is the initial pick, his to tune -- the cave-floor scan
 * also fails most sampled attempts, so the effective underground rate lands far below the surface one.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class DatapackProvider extends DatapackBuiltinEntriesProvider {

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, DatapackProvider::dimensionTypes)
            .add(Registries.BIOME, DatapackProvider::biomes)
            .add(Registries.LEVEL_STEM, DatapackProvider::levelStems)
            .add(Registries.CONFIGURED_FEATURE, DatapackProvider::configuredFeatures)
            .add(Registries.PLACED_FEATURE, DatapackProvider::placedFeatures)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, DatapackProvider::biomeModifiers);

    public DatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(GuWorld.MOD_ID));
    }

    //region Dimension type [维度类型]
    private static void dimensionTypes(BootstrapContext<DimensionType> context) {
        context.register(WorldDimensions.TREASURE_YELLOW_HEAVEN_TYPE, new DimensionType(
                OptionalLong.empty(),
                true,
                false,
                false,
                false,
                1.0,
                false,
                false,
                0,
                256,
                256,
                BlockTags.INFINIBURN_OVERWORLD,
                WorldDimensions.TREASURE_YELLOW_HEAVEN_EFFECTS,
                0.0F,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)
        ));
    }
    //endregion

    //region Biome [生物群系]
    private static final int TREASURE_YELLOW_HEAVEN_SKY_COLOR = 0xF4D35E;

    private static void biomes(BootstrapContext<Biome> context) {
        context.register(WorldDimensions.TREASURE_YELLOW_HEAVEN_BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(TREASURE_YELLOW_HEAVEN_SKY_COLOR)
                        .fogColor(TREASURE_YELLOW_HEAVEN_SKY_COLOR)
                        .waterColor(TREASURE_YELLOW_HEAVEN_SKY_COLOR)
                        .waterFogColor(TREASURE_YELLOW_HEAVEN_SKY_COLOR)
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build());
    }
    //endregion

    //region Level stem [维度层级源]
    private static void levelStems(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        Holder<Biome> biome = biomes.getOrThrow(WorldDimensions.TREASURE_YELLOW_HEAVEN_BIOME);
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), biome, List.of());
        context.register(WorldDimensions.TREASURE_YELLOW_HEAVEN_STEM, new LevelStem(
                dimensionTypes.getOrThrow(WorldDimensions.TREASURE_YELLOW_HEAVEN_TYPE),
                new FlatLevelSource(settings)
        ));
    }
    //endregion

    //region Spirit Spring [元泉] -- the worldgen features and the biomes they generate in
    private static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_SPRING_CONFIGURED = ResourceKey.create(
            Registries.CONFIGURED_FEATURE, GuWorld.id("spirit_spring"));
    private static final ResourceKey<PlacedFeature> SPIRIT_SPRING_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, GuWorld.id("spirit_spring"));
    private static final ResourceKey<BiomeModifier> GENERATE_SPIRIT_SPRING = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS, GuWorld.id("spirit_spring"));
    private static final ResourceKey<ConfiguredFeature<?, ?>> SPIRIT_SPRING_UNDERGROUND_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, GuWorld.id("spirit_spring_underground"));
    private static final ResourceKey<PlacedFeature> SPIRIT_SPRING_UNDERGROUND_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE, GuWorld.id("spirit_spring_underground"));
    private static final ResourceKey<BiomeModifier> GENERATE_SPIRIT_SPRING_UNDERGROUND = ResourceKey.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIERS, GuWorld.id("spirit_spring_underground"));
    private static final int SPIRIT_SPRING_RARITY = 1000;
    private static final int SPIRIT_SPRING_UNDERGROUND_RARITY = 3000;

    private static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(SPIRIT_SPRING_CONFIGURED,
                new ConfiguredFeature<>(WorldFeatures.SPIRIT_SPRING.get(), NoneFeatureConfiguration.INSTANCE));
        context.register(SPIRIT_SPRING_UNDERGROUND_CONFIGURED, new ConfiguredFeature<>(
                WorldFeatures.SPIRIT_SPRING_UNDERGROUND.get(), NoneFeatureConfiguration.INSTANCE));
    }

    private static void placedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(SPIRIT_SPRING_PLACED, new PlacedFeature(
                configured.getOrThrow(SPIRIT_SPRING_CONFIGURED),
                List.of(RarityFilter.onAverageOnceEvery(SPIRIT_SPRING_RARITY),
                        InSquarePlacement.spread(), BiomeFilter.biome())));
        context.register(SPIRIT_SPRING_UNDERGROUND_PLACED, new PlacedFeature(
                configured.getOrThrow(SPIRIT_SPRING_UNDERGROUND_CONFIGURED),
                List.of(RarityFilter.onAverageOnceEvery(SPIRIT_SPRING_UNDERGROUND_RARITY),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(8), VerticalAnchor.belowTop(8)),
                        BiomeFilter.biome())));
    }

    private static void biomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderSet<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(WorldBiomeTags.SPIRIT_SPRING_GENERATES);
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        context.register(GENERATE_SPIRIT_SPRING, new BiomeModifiers.AddFeaturesBiomeModifier(biomes,
                HolderSet.direct(placedFeatures.getOrThrow(SPIRIT_SPRING_PLACED)),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
        context.register(GENERATE_SPIRIT_SPRING_UNDERGROUND, new BiomeModifiers.AddFeaturesBiomeModifier(biomes,
                HolderSet.direct(placedFeatures.getOrThrow(SPIRIT_SPRING_UNDERGROUND_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_DECORATION));
    }
    //endregion
}
