package net.alex.guzhenrenworld.feature;

import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

/**
 * The Spirit Spring [元泉] worldgen: its keys, generation biomes and datapack entries.
 *
 * <p>Every key here shares {@link #ID}, {@code guworld:spirit_spring}, under which {@link SpiritSpringRegistration}
 * registers {@link SpiritSpringCluster}. {@link #registerConfiguredFeature} and {@link #registerPlacedFeature} are
 * the bootstraps {@code datagen.DatapackProvider} runs: the configured and placed feature, which
 * {@link SpiritSpringRegistration}'s biome modifier adds to {@link #BIOMES} at {@code TOP_LAYER_MODIFICATION} (after
 * vegetation); {@code datagen.BiomeTagsProvider} fills that tag from {@link #LAND_BIOMES} and the Southern Border's
 * land biomes ({@code BiomeLayout.LAND}). Guzhenren keeps the spring's block, fluid, item and client rendering; every
 * worldgen id here is in the {@code guworld} namespace.
 *
 * <p>{@link #LAND_BIOMES} is 39 overworld land biomes, moved here with the spring's worldgen as a copy of
 * Guzhenren's wild Gu land list; the two now belong to different mods and may diverge.
 *
 * <p>⚠ The spring is a land structure, so {@link #LAND_BIOMES} must NOT collapse to
 * {@code #minecraft:is_overworld}, which also carries the oceans and rivers.
 *
 * <p>⚠ {@link #RARITY} is Alex's pick, one roll per 300 chunks. The roll is not the rate: a roll grows a
 * cluster only where {@link SpiritSpringGround} finds level ground. In three 256-block overworld samples 1.2% of
 * columns pass its 15x15 rule, none in woods, so roughly 2% of rolls grow a cluster, about one per 15,000 overworld
 * chunks (an estimate from heightmaps, not a count of springs). There is no underground variant: a cave spot almost
 * never passes the level-ground check.
 *
 * @author Alex
 * @version 1.0.0
 * @see SpiritSpringCluster
 * @since 1.0.0
 */

public final class SpiritSpring {

    static final ResourceLocation ID = GuWorld.id("spirit_spring");
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, GuWorld.id("spirit_spring_generates"));
    public static final List<ResourceKey<Biome>> LAND_BIOMES = List.of(
            Biomes.PLAINS,
            Biomes.SUNFLOWER_PLAINS,
            Biomes.MEADOW,
            Biomes.CHERRY_GROVE,
            Biomes.FLOWER_FOREST,

            Biomes.FOREST,
            Biomes.BIRCH_FOREST,
            Biomes.DARK_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.WINDSWEPT_FOREST,

            Biomes.TAIGA,
            Biomes.SNOWY_TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA,
            Biomes.GROVE,

            Biomes.SAVANNA,
            Biomes.SAVANNA_PLATEAU,
            Biomes.WINDSWEPT_SAVANNA,

            Biomes.JUNGLE,
            Biomes.SPARSE_JUNGLE,
            Biomes.BAMBOO_JUNGLE,

            Biomes.DESERT,
            Biomes.BADLANDS,
            Biomes.WOODED_BADLANDS,
            Biomes.ERODED_BADLANDS,

            Biomes.SNOWY_PLAINS,
            Biomes.ICE_SPIKES,
            Biomes.SNOWY_SLOPES,
            Biomes.FROZEN_PEAKS,
            Biomes.JAGGED_PEAKS,
            Biomes.STONY_PEAKS,

            Biomes.WINDSWEPT_HILLS,
            Biomes.WINDSWEPT_GRAVELLY_HILLS,

            Biomes.SWAMP,
            Biomes.MANGROVE_SWAMP,

            Biomes.BEACH,
            Biomes.SNOWY_BEACH,
            Biomes.STONY_SHORE,

            Biomes.MUSHROOM_FIELDS);
    private static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ID);
    static final ResourceKey<PlacedFeature> PLACED_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, ID);
    private static final int RARITY = 300;

    private SpiritSpring() {}

    public static void registerConfiguredFeature(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(CONFIGURED_FEATURE,
                new ConfiguredFeature<>(SpiritSpringRegistration.FEATURE.get(), NoneFeatureConfiguration.INSTANCE));
    }

    public static void registerPlacedFeature(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(PLACED_FEATURE, new PlacedFeature(
                configuredFeatures.getOrThrow(CONFIGURED_FEATURE),
                List.of(RarityFilter.onAverageOnceEvery(RARITY), InSquarePlacement.spread(), BiomeFilter.biome())));
    }
}
