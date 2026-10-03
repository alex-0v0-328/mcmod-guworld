package net.alex.guzhenrenworld.dimension;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * The Treasure Yellow Heaven [宝黄天] dimension: its keys, and the datapack entries and strings behind them.
 *
 * <p>Every key shares the one location {@code guworld:treasure_yellow_heaven}. {@link #registerDimensionType},
 * {@link #registerBiome} and {@link #registerLevelStem} are the bootstraps {@code datagen.DatapackProvider}
 * runs: the dimension type, its one biome, and its level stem, a flat generator with no layers, so the
 * dimension is pure void. {@link #EFFECTS} is the special-effects id the dimension type names and
 * {@code client.dimension.TreasureYellowHeavenEffects} registers. {@link #COLOR} paints the sky, the fog
 * and the water, and is the title color {@link #addTranslations} writes.
 *
 * <p>Beds and respawn anchors work here (Alex, 2026-10-03): players cannot place blocks in this
 * dimension, but should one ever get here, it works instead of exploding.
 *
 * <p>{@link #addTranslations} builds every key from the registered keys, never from a raw string, so a
 * renamed dimension cannot leave a key behind pointing at nothing; each language provider passes only
 * its own rendering of the name. The {@code travelerstitles.*} pair is the Traveler's Titles
 * convention: the dimension title, and its color as lowercase hex without {@code #}.
 *
 * <p>⚠ Every key shares the location of Guzhenren's level key
 * {@code net.alex.guzhenren.registry.world.ModDimensions#TREASURE_YELLOW_HEAVEN}; the level stem is
 * what turns that key into a loaded level, so a renamed key here leaves Guzhenren's
 * {@code /guworld enter} pointing at nothing until that key is renamed too.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class TreasureYellowHeaven {

    private static final ResourceLocation ID = GuWorld.id("treasure_yellow_heaven");
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, ID);
    public static final ResourceKey<LevelStem> LEVEL_STEM = ResourceKey.create(Registries.LEVEL_STEM, ID);
    public static final ResourceLocation EFFECTS = ID;
    private static final int COLOR = 0xF4D35E;

    private TreasureYellowHeaven() {}

    public static void registerDimensionType(BootstrapContext<DimensionType> context) {
        context.register(DIMENSION_TYPE, new DimensionType(
                OptionalLong.empty(),
                true,
                false,
                false,
                false,
                1.0,
                true,
                true,
                0,
                256,
                256,
                BlockTags.INFINIBURN_OVERWORLD,
                EFFECTS,
                0.0F,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)
        ));
    }

    public static void registerBiome(BootstrapContext<Biome> context) {
        context.register(BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(COLOR)
                        .fogColor(COLOR)
                        .waterColor(COLOR)
                        .waterFogColor(COLOR)
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build());
    }

    public static void registerLevelStem(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        Holder<Biome> biome = context.lookup(Registries.BIOME).getOrThrow(BIOME);
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(Optional.empty(), biome, List.of());
        context.register(LEVEL_STEM, new LevelStem(
                dimensionTypes.getOrThrow(DIMENSION_TYPE),
                new FlatLevelSource(settings)
        ));
    }

    public static void addTranslations(LanguageProvider languageProvider, String name) {
        ResourceLocation dimensionId = LEVEL_STEM.location();
        languageProvider.add(dimensionId.toLanguageKey("dimension"), name);
        languageProvider.add(BIOME.location().toLanguageKey("biome"), name);
        languageProvider.add(dimensionId.toLanguageKey("travelerstitles"), name);
        languageProvider.add(dimensionId.toLanguageKey("travelerstitles", "color"), "%06x".formatted(COLOR));
    }
}
