package net.alex.guzhenrenworld.feature;

import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The Spirit Spring [元泉] worldgen: its feature registration, keys, generation biomes and datapack entries.
 *
 * <p>Annotated {@code @EventBusSubscriber}: {@link #onRegister} registers {@link SpiritSpringFeature} under
 * {@code guworld:spirit_spring}, which {@link #FEATURE} names and every key here shares.
 * {@link #registerConfiguredFeature}, {@link #registerPlacedFeature} and {@link #registerBiomeModifier} are
 * the bootstraps {@code datagen.DatapackProvider} runs: the configured and placed feature, and the biome
 * modifier adding it to {@link #BIOMES} at {@code TOP_LAYER_MODIFICATION} (after vegetation);
 * {@code datagen.BiomeTagsProvider} fills that tag from {@link #LAND_BIOMES}. The spring's worldgen moved
 * here from Guzhenren on 2026-10-02 (Alex), which keeps the block, fluid, item and client rendering, so
 * every worldgen id changed namespace from {@code guzhenren} to {@code guworld}.
 *
 * <p>{@link #LAND_BIOMES} is 39 overworld land biomes, moved here with the spring's worldgen as a copy of
 * Guzhenren's wild Gu land list; the two now belong to different mods and may diverge.
 *
 * <p>⚠ The spring is a land structure, so {@link #LAND_BIOMES} must NOT collapse to
 * {@code #minecraft:is_overworld}, which also carries the oceans and rivers.
 *
 * <p>⚠ {@link #RARITY} is Alex's pick (2026-10-02): about one cluster per 2,500 chunks of the 39 land
 * biomes. The roll is not the rate -- even with the anchor search only ≈4% of rolls find flat enough
 * ground (measured over 1,600 chunks), so 100 rolls per cluster, where the 2026-09-23 roll of 1000 with
 * the origin-only check left about one spring per 50,000 chunks. The underground cave variant
 * (2026-09-26, roll 3000) never grew in practice and was removed on 2026-10-02 (Alex).
 *
 * @author Alex
 * @version 1.0.0
 * @see SpiritSpringFeature
 * @since 1.0.0
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
public final class SpiritSpring {

    private static final ResourceLocation ID = GuWorld.id("spirit_spring");
    public static final DeferredHolder<Feature<?>, SpiritSpringFeature> FEATURE =
            DeferredHolder.create(Registries.FEATURE, ID);
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
    private static final ResourceKey<PlacedFeature> PLACED_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, ID);
    private static final ResourceKey<BiomeModifier> BIOME_MODIFIER =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ID);
    private static final int RARITY = 100;

    private SpiritSpring() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.FEATURE, ID, SpiritSpringFeature::new);
    }

    public static void registerConfiguredFeature(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(CONFIGURED_FEATURE,
                new ConfiguredFeature<>(FEATURE.get(), NoneFeatureConfiguration.INSTANCE));
    }

    public static void registerPlacedFeature(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(PLACED_FEATURE, new PlacedFeature(
                configuredFeatures.getOrThrow(CONFIGURED_FEATURE),
                List.of(RarityFilter.onAverageOnceEvery(RARITY), InSquarePlacement.spread(), BiomeFilter.biome())));
    }

    public static void registerBiomeModifier(BootstrapContext<BiomeModifier> context) {
        HolderSet<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(BIOMES);
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        context.register(BIOME_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(biomes,
                HolderSet.direct(placedFeatures.getOrThrow(PLACED_FEATURE)),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
    }
}
