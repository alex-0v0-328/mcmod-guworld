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
 * 南疆自制而非移植的竹海：竹子稀疏化的原版竹林。
 * <ul>
 *   <li>原版 {@link VegetationPlacements#BAMBOO} 在噪声峰值处每区块至多 160 丛，密不可行；{@link #BAMBOO} 按同一噪声放
 *       同样的灰化土底竹子，每区块至多 {@link #BAMBOO_COUNT} 次，峰值处仍密、丛间留空。</li>
 *   <li>其余（丛林树、花、熊猫、鹦鹉）照原版；同所有 Gu World 群系不雕刻洞穴（{@link Replicas#getUncarved}）。</li>
 *   <li>⚠ {@link #BAMBOO_COUNT} 是初值，待 Alex 进游戏判断。</li>
 * </ul>
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
