package net.alex.guzhenrenworld.dimension.southernborder;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Replicas;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.NoiseBasedCountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * The Southern Border [南疆] biome built here rather than ported: the bamboo sea [竹海], vanilla's bamboo jungle with
 * its bamboo thinned. Vanilla grows {@link VegetationPlacements#BAMBOO} up to 160 clumps a chunk where its noise peaks,
 * a wall nobody can walk through; {@link #BAMBOO} places the same podzol-floored bamboo at most {@link #BAMBOO_COUNT}
 * times a chunk on the same noise, so the stands stay dense where the noise peaks and open up between them. The rest
 * of the template, jungle trees, flowers, pandas and parrots, stays vanilla's, and like every Gu World biome it does
 * not carve ({@link Replicas#getUncarved}).
 *
 * <p>⚠ {@link #BAMBOO_COUNT} is a first pick for Alex to judge in game.
 *
 * @author Alex
 * @version 1.0.0
 * @see BiomeLayout
 * @since 1.0.0
 */

public final class BiomeSettings {

    public static final ResourceKey<Biome> BAMBOO_SEA = GuWorld.key(Registries.BIOME, "bamboo_sea");
    private static final ResourceKey<PlacedFeature> BAMBOO =
            GuWorld.key(Registries.PLACED_FEATURE, "bamboo_sea/bamboo");
    private static final int BAMBOO_COUNT = 60;
    private static final double BAMBOO_NOISE_SCALE = 80.0;
    private static final double BAMBOO_NOISE_OFFSET = 0.3;

    private BiomeSettings() {}

    public static void registerPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(BAMBOO, new PlacedFeature(configured.getOrThrow(VegetationFeatures.BAMBOO_SOME_PODZOL),
                List.of(NoiseBasedCountPlacement.of(BAMBOO_COUNT, BAMBOO_NOISE_SCALE, BAMBOO_NOISE_OFFSET),
                        InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome())));
    }

    public static void registerBiomes(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        Biome template = OverworldBiomes.bambooJungle(placed, context.lookup(Registries.CONFIGURED_CARVER));
        Holder<PlacedFeature> thinned = placed.getOrThrow(BAMBOO);
        List<HolderSet<PlacedFeature>> features = new ArrayList<>();
        for (HolderSet<PlacedFeature> step : template.getGenerationSettings().features()) {
            List<Holder<PlacedFeature>> holders = new ArrayList<>();
            step.forEach(holder -> holders.add(holder.is(VegetationPlacements.BAMBOO) ? thinned : holder));
            features.add(HolderSet.direct(holders));
        }
        context.register(BAMBOO_SEA, Replicas.getUncarved(template, features));
    }
}
