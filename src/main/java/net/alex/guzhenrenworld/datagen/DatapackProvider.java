package net.alex.guzhenrenworld.datagen;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.registry.WorldDimensions;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

/**
 * The single provider for every datapack registry this mod writes.
 *
 * <p>Extends {@link net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider}. Builds the
 * Treasure Yellow Heaven dimension -- its dimension type, its one biome and its level stem, a flat
 * generator with no layers, so the dimension is pure void -- in one {@code RegistrySetBuilder}.
 *
 * <p>⚠ There can only be one per data run. The builtin-entries provider reports a fixed name, so a
 * second instance fails datagen outright; add a registry to this one's builder instead.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class DatapackProvider extends DatapackBuiltinEntriesProvider {

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, DatapackProvider::dimensionTypes)
            .add(Registries.BIOME, DatapackProvider::biomes)
            .add(Registries.LEVEL_STEM, DatapackProvider::levelStems);

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
}
