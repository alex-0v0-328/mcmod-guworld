package net.alex.guzhenrenworld.dimension.easternsea;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/**
 * Where the Eastern Sea [东海] places its biomes: Terralith's overworld multi-noise table as shipped
 * ({@link Ports#getBiomeParameters}), with every source biome mapped onto the region's own ({@link #GROUPS}, a
 * roster biome to the source biomes it stands in for). The terrain under it is the region's copy of Tectonic's
 * ({@link EasternSeaTerrain}), which remaps its own erosion and ridges into the bands Terralith's table expects, so
 * each parameter cell keeps the terrain archetype Terralith gave it and gets the region's biome of its kind.
 *
 * <p>The roster, by kind:
 * <ul>
 *   <li>Sea: the warm ocean, the lukewarm ocean, the ocean, the deep warm ocean and the deep ocean. The sea is warm:
 *   every sea moves two temperature bands warmer, so the frozen seas become the ocean and the deep ocean, the cold
 *   sea the lukewarm ocean, and every other sea the warm ocean or the deep warm ocean (the region has no deep
 *   lukewarm ocean). Terralith's sky islands stand over open sea, which the region keeps water, so their cells take
 *   the shallow sea of their band.</li>
 *   <li>Shores: the beach, the stony shore and the gravel beach; the deserts become sand islands, the beach.</li>
 *   <li>Lowlands: the isles, the island biome every temperate, cold or dry lowland source turns into; the mirage
 *   isles, the far-sea islands of Terralith's deepest band beside the mushroom fields, and the flowering and
 *   moonlit sources; the rainforest (the Southern Border's [南疆], Terralith's tropical jungle) and the jungle,
 *   every humid source.</li>
 *   <li>Highlands: the volcanic peaks on the warm mountain sources and the hot springs, the volcanic crater on the
 *   crater and the caldera; the basalt cliffs on the hot, dry badlands, canyons and spires (sea stacks); the
 *   white cliffs on every cool or snowy mountain and cliff source.</li>
 *   <li>The river, and underground the lush caves and the dripstone caves.</li>
 * </ul>
 * No source maps to a cold or frozen biome, so the region has no snow or ice above the peaks Tectonic raises highest.
 *
 * <p>{@link #ROSTER} is every biome the region can place, {@link #LAND} every surface biome of it but the water
 * ({@link #WATER_AND_CAVES}), where the spirit spring [元泉] may stand.
 *
 * <p>⚠ {@link #getParameters} throws on a source biome {@link #GROUPS} does not map, so a re-port that adds one
 * fails {@code runData} instead of dropping cells.
 *
 * @author Alex
 * @version 1.0.0
 * @see EasternSeaTerrain
 * @since 1.0.0
 */

public final class EasternSeaLayout {

    private static final Map<ResourceKey<Biome>, List<String>> GROUPS = Map.ofEntries(
            Map.entry(EasternSeaRoster.WARM_OCEAN, List.of("minecraft:warm_ocean", "minecraft:lukewarm_ocean",
                    "minecraft:ocean", "terralith:skylands_summer", "terralith:skylands_autumn",
                    "terralith:skylands_spring")),
            Map.entry(EasternSeaRoster.LUKEWARM_OCEAN, List.of("minecraft:cold_ocean", "terralith:skylands_winter")),
            Map.entry(EasternSeaRoster.OCEAN, List.of("minecraft:frozen_ocean")),
            Map.entry(EasternSeaRoster.DEEP_WARM_OCEAN, List.of("terralith:deep_warm_ocean",
                    "minecraft:deep_lukewarm_ocean", "minecraft:deep_ocean", "minecraft:deep_cold_ocean")),
            Map.entry(EasternSeaRoster.DEEP_OCEAN, List.of("minecraft:deep_frozen_ocean")),
            Map.entry(EasternSeaRoster.BEACH, List.of("minecraft:beach", "minecraft:snowy_beach", "minecraft:desert",
                    "terralith:lush_desert", "terralith:ancient_sands", "terralith:desert_oasis",
                    "terralith:red_oasis")),
            Map.entry(EasternSeaRoster.STONY_SHORE, List.of("minecraft:stony_shore")),
            Map.entry(EasternSeaRoster.GRAVEL_BEACH, List.of("terralith:gravel_beach", "terralith:gravel_desert")),
            Map.entry(EasternSeaRoster.ISLES, List.of("terralith:alpha_islands", "terralith:alpha_islands_winter",
                    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:snowy_plains", "minecraft:ice_spikes",
                    "minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                    "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga",
                    "minecraft:old_growth_spruce_taiga", "minecraft:savanna", "minecraft:savanna_plateau",
                    "minecraft:windswept_savanna", "minecraft:windswept_forest", "minecraft:swamp",
                    "minecraft:meadow", "terralith:orchid_swamp", "terralith:ice_marsh", "terralith:shrubland",
                    "terralith:cold_shrubland", "terralith:hot_shrubland", "terralith:rocky_shrubland",
                    "terralith:brushland", "terralith:steppe", "terralith:wintry_lowlands",
                    "terralith:wintry_forest", "terralith:siberian_taiga", "terralith:birch_taiga",
                    "terralith:highlands", "terralith:temperate_highlands", "terralith:forested_highlands",
                    "terralith:alpine_highlands", "terralith:arid_highlands", "terralith:shield",
                    "terralith:shield_clearing", "terralith:valley_clearing", "terralith:yosemite_lowlands")),
            Map.entry(EasternSeaRoster.MIRAGE_ISLES, List.of("terralith:mirage_isles", "terralith:warped_mesa",
                    "minecraft:mushroom_fields", "minecraft:flower_forest", "minecraft:cherry_grove",
                    "terralith:sakura_grove", "terralith:sakura_valley", "terralith:snowy_cherry_grove",
                    "terralith:blooming_plateau", "terralith:blooming_valley", "terralith:lavender_forest",
                    "terralith:lavender_valley", "terralith:lush_valley", "terralith:moonlight_grove",
                    "terralith:moonlight_valley", "terralith:amethyst_canyon")),
            Map.entry(Ports.RAINFOREST, List.of("terralith:tropical_jungle", "terralith:amethyst_rainforest",
                    "terralith:rocky_jungle", "terralith:jungle_mountains", "terralith:haze_mountain",
                    "minecraft:bamboo_jungle", "minecraft:mangrove_swamp")),
            Map.entry(EasternSeaRoster.JUNGLE, List.of("minecraft:jungle", "minecraft:sparse_jungle",
                    "minecraft:dark_forest", "terralith:cloud_forest")),
            Map.entry(EasternSeaRoster.VOLCANIC_PEAKS, List.of("terralith:volcanic_peaks", "minecraft:stony_peaks",
                    "terralith:savanna_slopes", "terralith:ashen_savanna", "terralith:painted_mountains",
                    "terralith:yellowstone")),
            Map.entry(EasternSeaRoster.VOLCANIC_CRATER, List.of("terralith:volcanic_crater", "terralith:caldera")),
            Map.entry(EasternSeaRoster.BASALT_CLIFFS, List.of("terralith:basalt_cliffs", "terralith:granite_cliffs",
                    "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands",
                    "terralith:savanna_badlands", "terralith:bryce_canyon", "terralith:white_mesa",
                    "terralith:desert_canyon", "terralith:sandstone_valley", "terralith:desert_spires",
                    "terralith:fractured_savanna", "terralith:stony_spires", "terralith:windswept_spires")),
            Map.entry(EasternSeaRoster.WHITE_CLIFFS, List.of("terralith:white_cliffs", "terralith:frozen_cliffs",
                    "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:snowy_slopes", "minecraft:grove",
                    "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "terralith:alpine_grove",
                    "terralith:emerald_peaks", "terralith:scarlet_mountains", "terralith:rocky_mountains",
                    "terralith:snowy_maple_forest", "terralith:snowy_shield", "terralith:siberian_grove",
                    "terralith:snowy_badlands", "terralith:yosemite_cliffs", "terralith:glacial_chasm")),
            Map.entry(Replicas.RIVER, List.of("minecraft:river", "minecraft:frozen_river", "terralith:warm_river")),
            Map.entry(Replicas.LUSH_CAVES, List.of("minecraft:lush_caves", "terralith:cave/underground_jungle",
                    "terralith:cave/fungal_caves")),
            Map.entry(Replicas.DRIPSTONE_CAVES, List.of("minecraft:dripstone_caves", "minecraft:deep_dark",
                    "terralith:cave/andesite_caves", "terralith:cave/deep_caves", "terralith:cave/diorite_caves",
                    "terralith:cave/frostfire_caves", "terralith:cave/granite_caves", "terralith:cave/infested_caves",
                    "terralith:cave/mantle_caves", "terralith:cave/thermal_caves", "terralith:cave/tuff_caves")));
    public static final Set<ResourceKey<Biome>> ROSTER = GROUPS.keySet();
    private static final Map<ResourceLocation, ResourceKey<Biome>> SOURCES = getSources();
    private static final Set<ResourceKey<Biome>> WATER_AND_CAVES = Set.of(Replicas.RIVER,
            EasternSeaRoster.WARM_OCEAN, EasternSeaRoster.LUKEWARM_OCEAN, EasternSeaRoster.OCEAN,
            EasternSeaRoster.DEEP_WARM_OCEAN, EasternSeaRoster.DEEP_OCEAN, Replicas.LUSH_CAVES,
            Replicas.DRIPSTONE_CAVES);
    public static final List<ResourceKey<Biome>> LAND = ROSTER.stream()
            .filter(biome -> !WATER_AND_CAVES.contains(biome))
            .sorted(Comparator.comparing(ResourceKey::location)).toList();

    private EasternSeaLayout() {}

    public static List<Pair<Climate.ParameterPoint, Holder<Biome>>> getParameters(HolderGetter<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters = new ArrayList<>();
        for (Map.Entry<Climate.ParameterPoint, ResourceLocation> entry : Ports.getBiomeParameters()) {
            ResourceKey<Biome> biome = SOURCES.get(entry.getValue());
            if (biome == null) throw new IllegalStateException("no Eastern Sea biome for " + entry.getValue());
            parameters.add(Pair.of(entry.getKey(), biomes.getOrThrow(biome)));
        }
        return parameters;
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
