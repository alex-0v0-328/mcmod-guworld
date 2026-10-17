package net.alex.guzhenrenworld.dimension.southernborder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Builds the four Southern Border [南疆] landform biomes of {@link BiomeLayout}: each a vanilla biome with
 * vanilla's climate, colors and features, plus the Terralith dressing ported for its kind of rock
 * ({@link Ports}), placed in the generation step Terralith places it in:
 * <ul>
 *   <li>Karst peak forest [喀斯特峰林]: vanilla's forest, with Terralith's stony spires dressing: moss and dripstone
 *   patches on the towers, bushes, oaks, azaleas and grass on their tops.</li>
 *   <li>Danxia [丹霞]: vanilla's forest; its red beds are surface rules ({@code Surface}).</li>
 *   <li>Sandstone pillars [砂岩峰林]: vanilla's taiga, with Terralith's windswept spires dressing: grass caps on the
 *   ledges, spruces, tall foliage and grass on the tops.</li>
 *   <li>Zhangshiyan cliffs [嶂石岩]: vanilla's meadow, with grass caps on the ledges and Terralith's Yosemite
 *   oaks.</li>
 * </ul>
 *
 * <p>All four drop lakes and fluid springs ({@link #getCliffSafe}), so no lava pool sits on a pillar top and no lava
 * spring pours down a wall, and none carves: each is rebuilt through {@link Replicas#getUncarved}, for the reason
 * {@code Replicas} gives.
 *
 * <p>⚠ The dressing is a first prototype for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see BiomeLayout
 * @since 1.0.0
 */

public final class BiomeSettings {

    private BiomeSettings() {}

    public static void registerBiomes(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        Biome forest = OverworldBiomes.forest(placed, carvers, false, false, false);
        register(context, BiomeLayout.KARST_PEAK_FOREST, forest, Map.of(
                GenerationStep.Decoration.RAW_GENERATION, getFeatures(placed, Ports.SPIRES_CLIFF),
                GenerationStep.Decoration.VEGETAL_DECORATION, getFeatures(placed, Ports.SHRUBLAND_BUSHES,
                        Ports.SPIRES_OAK_TREE, Ports.SPIRES_AZALEA, Ports.SPIRES_GRASS)));
        register(context, BiomeLayout.DANXIA, forest, Map.of());
        register(context, BiomeLayout.SANDSTONE_PILLARS, OverworldBiomes.taiga(placed, carvers, false),
                Map.of(GenerationStep.Decoration.RAW_GENERATION, getFeatures(placed, Ports.CLIFF_GRASS_UPPER,
                                Ports.CLIFF_GRASS_LOWER),
                        GenerationStep.Decoration.VEGETAL_DECORATION, getFeatures(placed, Ports.SPIRES_SPRUCE_TREES,
                                Ports.SPIRES_TALL_FOLIAGE, Ports.SPIRES_PATCH_GRASS)));
        register(context, BiomeLayout.ZHANGSHIYAN_CLIFFS,
                OverworldBiomes.meadowOrCherryGrove(placed, carvers, false), Map.of(
                        GenerationStep.Decoration.RAW_GENERATION, getFeatures(placed, Ports.CLIFF_GRASS_UPPER,
                                Ports.CLIFF_GRASS_LOWER),
                        GenerationStep.Decoration.VEGETAL_DECORATION, getFeatures(placed, Ports.YOSEMITE_OAKS)));
    }

    @SafeVarargs
    private static List<Holder<PlacedFeature>> getFeatures(HolderGetter<PlacedFeature> placed,
            ResourceKey<PlacedFeature>... keys) {
        List<Holder<PlacedFeature>> features = new ArrayList<>();
        for (ResourceKey<PlacedFeature> key : keys) {
            features.add(placed.getOrThrow(key));
        }
        return features;
    }

    private static void register(BootstrapContext<Biome> context, ResourceKey<Biome> key, Biome template,
            Map<GenerationStep.Decoration, List<Holder<PlacedFeature>>> dressing) {
        List<HolderSet<PlacedFeature>> features = new ArrayList<>();
        List<HolderSet<PlacedFeature>> steps = template.getGenerationSettings().features();
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            List<Holder<PlacedFeature>> holders = new ArrayList<>();
            if (step.ordinal() < steps.size()) {
                steps.get(step.ordinal()).forEach(holders::add);
            }
            holders.addAll(dressing.getOrDefault(step, List.of()));
            features.add(getCliffSafe(step, holders));
        }
        context.register(key, Replicas.getUncarved(template, features));
    }

    private static HolderSet<PlacedFeature> getCliffSafe(GenerationStep.Decoration step,
            List<Holder<PlacedFeature>> holders) {
        boolean open = step == GenerationStep.Decoration.LAKES || step == GenerationStep.Decoration.FLUID_SPRINGS;
        return open ? HolderSet.direct() : HolderSet.direct(holders);
    }
}
