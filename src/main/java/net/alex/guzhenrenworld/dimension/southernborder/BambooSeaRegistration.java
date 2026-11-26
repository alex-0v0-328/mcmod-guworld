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
 * 南疆自制群系「竹海」（{@link #BAMBOO_SEA}）及其稀疏竹子放置地物的注册引导。
 * <ul>
 *   <li>原版竹林的 {@link VegetationPlacements#BAMBOO} 在噪声峰值处每区块最多生成 160 丛竹子，过于密集且阻碍通行；本类
 *       注册{@link #BAMBOO} 将每区块上限降至 {@link #BAMBOO_COUNT}（待实机验收微调），在保留成片竹林观感的同时留出林间
 *       穿行空隙。</li>
 *   <li>除替换竹子放置地物并经 {@link Replicas#getUncarved} 移除地形雕刻器外，其余丛林树木、花草与熊猫、鹦鹉刷怪配置均
 *       沿用原版竹林模板。</li>
 * </ul>
 */

public final class BambooSeaRegistration {

    public static final ResourceKey<Biome> BAMBOO_SEA = GuWorld.key(Registries.BIOME, "bamboo_sea");
    private static final ResourceKey<PlacedFeature> BAMBOO =
            GuWorld.key(Registries.PLACED_FEATURE, "bamboo_sea/bamboo");
    private static final int BAMBOO_COUNT = 60;
    private static final double BAMBOO_NOISE_SCALE = 80.0;
    private static final double BAMBOO_NOISE_OFFSET = 0.3;

    private BambooSeaRegistration() {}

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
