package net.alex.guzhenrenworld.dimension.southernborder;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.biome.WildCropClimate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/**
 * Where the Southern Border [南疆] places its biomes: Terralith's overworld multi-noise table as shipped
 * ({@link Ports#getBiomeParameters}), with every source biome mapped onto the region's own ({@link #SOURCES}). The
 * terrain under it is Tectonic's ({@link Terrain}), which remaps its own erosion and ridges into the bands Terralith's
 * table expects, so each parameter cell keeps the terrain archetype Terralith gave it and gets the Southern Border
 * biome of its kind.
 *
 * <p>The roster, by terrain band:
 * <ul>
 *   <li>Mountains: misty mountains, cloud forest, rocky peaks, emerald peaks, jungle mountains, painted mountains,
 *   granite cliffs, alpine meadow; and the two landforms, Zhangshiyan cliffs on Terralith's cliff and canyon cells and
 *   karst peak forest on its spire cells.</li>
 *   <li>Hills and plateaus: bamboo sea ({@link BiomeSettings}), rainforest, plateau, forested plateau, peach
 *   grove.</li>
 *   <li>Flats: plains, orchid swamp, hot springs, and the river.</li>
 *   <li>Underground: lush caves and dripstone caves.</li>
 * </ul>
 * A source biome goes to the roster biome of the same landform and the nearest vegetation; cold sources take the
 * mountain-forest and meadow biomes, deserts and dry shrubland the plains, badlands the painted mountains. Terralith
 * keeps its haze mountain, emerald peaks and jungle mountains rare, so each also takes a common neighbor: the
 * misty mountains vanilla's mountain slopes, the emerald peaks the jagged peaks, the jungle mountains the rocky
 * jungle. Ocean,
 * beach and island sources are mapped for completeness only: {@link Terrain}'s continentalness never reaches their
 * cells. {@link #ROSTER} is every biome the region can place, the spire cells' karst peak forest included, and
 * {@link #LAND} every surface biome of it but the river, where the spirit spring [元泉] may stand. {@link #CROPS}
 * sorts the land into the wild crop climates of Farmer's Delight by the temperature and downfall rule of
 * {@link WildCropClimate}.
 *
 * <p>Terrain follows erosion, not the biome: every cell of Terralith's spire band, erosion {@link #SPIRE_EROSION} and
 * up, which Tectonic reaches only where its own erosion runs past 1, grows Tectonic's low-factor spire terrain
 * whatever biome sits there. So a land cell there becomes karst peak forest ({@link #isSpireCell}) unless it holds
 * one of {@link #KEPT_ON_SPIRES}: the river, the caves, the swamp, the hot springs and the Zhangshiyan cliffs keep
 * their own.
 *
 * <p>⚠ {@link #getParameters} throws on a source biome {@link #SOURCES} does not map, so a re-port that adds one fails
 * {@code runData} instead of dropping cells.
 *
 * @author Alex
 * @version 1.0.0
 * @see Terrain
 * @since 1.0.0
 */

public final class BiomeLayout {

    private static final Map<ResourceKey<Biome>, List<String>> GROUPS = Map.ofEntries(
            Map.entry(Replicas.RIVER, List.of("minecraft:river", "minecraft:frozen_river", "terralith:warm_river")),
            Map.entry(Replicas.PLAINS, List.of("minecraft:plains", "minecraft:sunflower_plains",
                    "minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:savanna", "minecraft:desert",
                    "minecraft:beach", "minecraft:snowy_beach", "minecraft:stony_shore", "minecraft:mushroom_fields",
                    "minecraft:ocean", "minecraft:deep_ocean", "minecraft:cold_ocean", "minecraft:deep_cold_ocean",
                    "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean", "minecraft:lukewarm_ocean",
                    "minecraft:deep_lukewarm_ocean", "minecraft:warm_ocean", "terralith:deep_warm_ocean",
                    "terralith:gravel_beach", "terralith:alpha_islands", "terralith:alpha_islands_winter",
                    "terralith:mirage_isles", "terralith:skylands_autumn", "terralith:skylands_spring",
                    "terralith:skylands_summer", "terralith:skylands_winter", "terralith:steppe",
                    "terralith:shrubland", "terralith:cold_shrubland", "terralith:hot_shrubland",
                    "terralith:rocky_shrubland", "terralith:brushland", "terralith:gravel_desert",
                    "terralith:lush_desert", "terralith:ancient_sands", "terralith:ashen_savanna",
                    "terralith:wintry_lowlands", "terralith:yosemite_lowlands", "terralith:valley_clearing",
                    "terralith:desert_oasis", "terralith:red_oasis")),
            Map.entry(Ports.ORCHID_SWAMP, List.of("minecraft:swamp", "minecraft:mangrove_swamp",
                    "terralith:orchid_swamp", "terralith:ice_marsh")),
            Map.entry(Ports.HOT_SPRINGS, List.of("terralith:yellowstone")),
            Map.entry(Ports.FORESTED_PLATEAU, List.of("minecraft:forest", "minecraft:flower_forest",
                    "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "terralith:birch_taiga",
                    "terralith:lavender_forest", "terralith:forested_highlands")),
            Map.entry(BiomeSettings.BAMBOO_SEA, List.of("minecraft:bamboo_jungle", "minecraft:sparse_jungle")),
            Map.entry(Ports.RAINFOREST, List.of("minecraft:jungle", "minecraft:dark_forest",
                    "terralith:tropical_jungle", "terralith:amethyst_rainforest")),
            Map.entry(Ports.CLOUD_FOREST, List.of("minecraft:taiga", "minecraft:snowy_taiga",
                    "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                    "terralith:siberian_taiga", "terralith:siberian_grove", "terralith:wintry_forest",
                    "terralith:snowy_maple_forest", "terralith:moonlight_grove", "terralith:moonlight_valley",
                    "terralith:cloud_forest")),
            Map.entry(Ports.PLATEAU, List.of("minecraft:savanna_plateau", "minecraft:windswept_hills",
                    "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills", "minecraft:windswept_savanna",
                    "terralith:highlands", "terralith:temperate_highlands", "terralith:arid_highlands",
                    "terralith:shield", "terralith:shield_clearing", "terralith:snowy_shield",
                    "terralith:savanna_slopes", "terralith:blooming_plateau")),
            Map.entry(Ports.PEACH_GROVE, List.of("minecraft:cherry_grove", "terralith:sakura_grove",
                    "terralith:sakura_valley", "terralith:snowy_cherry_grove", "terralith:blooming_valley",
                    "terralith:lavender_valley", "terralith:lush_valley")),
            Map.entry(Ports.ALPINE_MEADOW, List.of("minecraft:meadow", "terralith:alpine_highlands")),
            Map.entry(Ports.ROCKY_PEAKS, List.of("minecraft:frozen_peaks", "minecraft:stony_peaks",
                    "terralith:rocky_mountains")),
            Map.entry(Ports.EMERALD_PEAKS, List.of("minecraft:jagged_peaks", "terralith:emerald_peaks",
                    "terralith:scarlet_mountains")),
            Map.entry(Ports.MISTY_MOUNTAINS, List.of("minecraft:grove", "minecraft:snowy_slopes",
                    "terralith:alpine_grove", "terralith:haze_mountain")),
            Map.entry(Ports.JUNGLE_MOUNTAINS, List.of("terralith:jungle_mountains", "terralith:rocky_jungle")),
            Map.entry(Ports.PAINTED_MOUNTAINS, List.of("minecraft:badlands", "minecraft:eroded_badlands",
                    "minecraft:wooded_badlands", "terralith:painted_mountains", "terralith:snowy_badlands",
                    "terralith:savanna_badlands", "terralith:warped_mesa")),
            Map.entry(Ports.GRANITE_CLIFFS, List.of("terralith:granite_cliffs", "terralith:frozen_cliffs",
                    "terralith:volcanic_peaks", "terralith:volcanic_crater", "terralith:caldera",
                    "terralith:basalt_cliffs", "terralith:white_cliffs")),
            Map.entry(Ports.ZHANGSHIYAN_CLIFFS, List.of("terralith:yosemite_cliffs", "terralith:white_mesa",
                    "terralith:sandstone_valley", "terralith:glacial_chasm", "terralith:amethyst_canyon",
                    "terralith:desert_canyon", "terralith:bryce_canyon")),
            Map.entry(Ports.KARST_PEAK_FOREST, List.of("terralith:stony_spires", "terralith:windswept_spires",
                    "terralith:desert_spires", "terralith:fractured_savanna")),
            Map.entry(Replicas.LUSH_CAVES, List.of("minecraft:lush_caves", "terralith:cave/underground_jungle",
                    "terralith:cave/fungal_caves")),
            Map.entry(Replicas.DRIPSTONE_CAVES, List.of("minecraft:dripstone_caves", "minecraft:deep_dark",
                    "terralith:cave/andesite_caves", "terralith:cave/deep_caves", "terralith:cave/diorite_caves",
                    "terralith:cave/frostfire_caves", "terralith:cave/granite_caves", "terralith:cave/infested_caves",
                    "terralith:cave/mantle_caves", "terralith:cave/thermal_caves", "terralith:cave/tuff_caves")));
    public static final Set<ResourceKey<Biome>> ROSTER = GROUPS.keySet();
    public static final Map<ResourceLocation, ResourceKey<Biome>> SOURCES = getSources();
    private static final float SPIRE_EROSION = 0.565F;
    private static final Set<ResourceKey<Biome>> WATER_AND_CAVES = Set.of(Replicas.RIVER, Replicas.LUSH_CAVES,
            Replicas.DRIPSTONE_CAVES);
    private static final Set<ResourceKey<Biome>> KEPT_ON_SPIRES = Set.of(Replicas.RIVER, Replicas.LUSH_CAVES,
            Replicas.DRIPSTONE_CAVES, Ports.ORCHID_SWAMP, Ports.HOT_SPRINGS, Ports.ZHANGSHIYAN_CLIFFS);
    public static final List<ResourceKey<Biome>> LAND = ROSTER.stream()
            .filter(biome -> !WATER_AND_CAVES.contains(biome))
            .sorted(Comparator.comparing(ResourceKey::location)).toList();
    public static final Map<WildCropClimate, List<ResourceKey<Biome>>> CROPS = Map.of(
            WildCropClimate.HOT, List.of(Ports.PAINTED_MOUNTAINS),
            WildCropClimate.WET, List.of(Ports.CLOUD_FOREST, Ports.HOT_SPRINGS, Ports.ORCHID_SWAMP, Ports.PEACH_GROVE,
                    Ports.RAINFOREST, Ports.JUNGLE_MOUNTAINS, Ports.ZHANGSHIYAN_CLIFFS, BiomeSettings.BAMBOO_SEA),
            WildCropClimate.BEACH, List.of(),
            WildCropClimate.TEMPERATE, List.of(Replicas.PLAINS, Ports.PLATEAU, Ports.ORCHID_SWAMP, Ports.PEACH_GROVE,
                    Ports.GRANITE_CLIFFS, Ports.ALPINE_MEADOW, Ports.KARST_PEAK_FOREST),
            WildCropClimate.COLD, List.of(Ports.CLOUD_FOREST, Ports.MISTY_MOUNTAINS, Ports.HOT_SPRINGS,
                    Ports.ROCKY_PEAKS, Ports.EMERALD_PEAKS));

    private BiomeLayout() {}

    public static List<Pair<Climate.ParameterPoint, Holder<Biome>>> getParameters(HolderGetter<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters = new ArrayList<>();
        for (Map.Entry<Climate.ParameterPoint, ResourceLocation> entry : Ports.getBiomeParameters()) {
            ResourceKey<Biome> biome = SOURCES.get(entry.getValue());
            if (biome == null) throw new IllegalStateException("no Southern Border biome for " + entry.getValue());
            if (isSpireCell(entry.getKey()) && !KEPT_ON_SPIRES.contains(biome)) biome = Ports.KARST_PEAK_FOREST;
            parameters.add(Pair.of(entry.getKey(), biomes.getOrThrow(biome)));
        }
        return parameters;
    }

    private static boolean isSpireCell(Climate.ParameterPoint point) {
        return point.erosion().min() >= Climate.quantizeCoord(SPIRE_EROSION);
    }

    private static Map<ResourceLocation, ResourceKey<Biome>> getSources() {
        Map<ResourceLocation, ResourceKey<Biome>> sources = new HashMap<>();
        GROUPS.forEach((biome, names) -> names.forEach(name -> {
            if (sources.put(ResourceLocation.parse(name), biome) != null) {
                throw new IllegalStateException(name + " is mapped twice");
            }
        }));
        return Map.copyOf(sources);
    }
}
