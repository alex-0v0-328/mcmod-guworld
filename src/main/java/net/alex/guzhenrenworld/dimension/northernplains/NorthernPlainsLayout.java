package net.alex.guzhenrenworld.dimension.northernplains;

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
 * Where the Northern Plains [北原] places its biomes: Terralith's overworld multi-noise table as shipped
 * ({@link Ports#getBiomeParameters}), with every source biome mapped onto the region's own ({@link #GROUPS}, a
 * roster biome to the source biomes it stands in for). The terrain under it is the region's copy of Tectonic's
 * ({@link NorthernPlainsTerrain}), which remaps its own erosion and ridges into the bands Terralith's table expects,
 * so each parameter cell keeps the terrain archetype Terralith gave it and gets the region's biome of its kind.
 *
 * <p>The roster, by terrain band:
 * <ul>
 *   <li>Mountains ({@link #MOUNTAINS}): frozen peaks, frozen cliffs, and the glacial chasm, a landform.</li>
 *   <li>Hills and plateaus ({@link #HILLS}): grove, alpine highlands, shield, snowy shield.</li>
 *   <li>Flats: steppe, cold shrubland, wintry lowlands, snowy plains, ice spikes, ice marsh, the four taigas and
 *   forests (Siberian, birch, snowy, wintry), and the two rivers.</li>
 *   <li>Underground: dripstone caves, every cave source.</li>
 * </ul>
 * Terralith sorts its table by temperature first, and the region keeps that split: each frozen-band source (under
 * -0.45) goes to a biome that snows, each warmer source, but for the snowy shield and the slopes, to one that does
 * not, so the latitude gradient {@link NorthernPlainsTerrain} puts on the temperature turns into snow in the north
 * and grassland in the south without the table changing. Every peak goes to the frozen peaks and every cliff and
 * badland to the frozen cliffs, whatever its temperature, since the region's only mountains are icy; canyons go to
 * the glacial chasm. Terralith keeps its frozen cliffs rare, so the two frozen-band peak sources, emerald peaks and
 * scarlet mountains, take them too. Warm and hot sources, which the gradient leaves almost unreached, and the ocean,
 * beach and island sources, which the continentalness never reaches, are mapped for completeness to the nearest
 * landform. {@link #FROZEN} lists the biomes the frozen band reaches.
 *
 * <p>{@link #ROSTER} is every biome the region can place, {@link #LAND} every surface biome of it but the water and
 * the caves ({@link #WATER_AND_CAVES}), where the spirit spring [元泉] may stand.
 *
 * <p>⚠ {@link #getParameters} throws on a source biome {@link #GROUPS} does not map, so a re-port that adds one fails
 * {@code runData} instead of dropping cells.
 *
 * @author Alex
 * @version 1.0.0
 * @see NorthernPlainsTerrain
 * @since 1.0.0
 */

public final class NorthernPlainsLayout {

    private static final Map<ResourceKey<Biome>, List<String>> GROUPS = Map.ofEntries(
            Map.entry(Replicas.RIVER, List.of("minecraft:river", "terralith:warm_river")),
            Map.entry(NorthernPlainsRoster.FROZEN_RIVER, List.of("minecraft:frozen_river")),
            Map.entry(NorthernPlainsRoster.STEPPE, List.of("minecraft:plains", "minecraft:sunflower_plains",
                    "minecraft:savanna", "minecraft:desert", "minecraft:beach", "minecraft:stony_shore",
                    "minecraft:mushroom_fields", "minecraft:ocean", "minecraft:deep_ocean", "minecraft:cold_ocean",
                    "minecraft:deep_cold_ocean", "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean",
                    "minecraft:warm_ocean", "terralith:deep_warm_ocean", "terralith:gravel_beach",
                    "terralith:alpha_islands", "terralith:mirage_isles", "terralith:skylands_autumn",
                    "terralith:skylands_spring", "terralith:skylands_summer", "terralith:skylands_winter",
                    "terralith:steppe", "terralith:shrubland", "terralith:hot_shrubland", "terralith:brushland",
                    "terralith:ancient_sands", "terralith:lush_desert", "terralith:desert_oasis",
                    "terralith:red_oasis", "terralith:yosemite_lowlands", "terralith:valley_clearing",
                    "terralith:lush_valley", "terralith:moonlight_valley", "terralith:blooming_valley",
                    "terralith:lavender_valley", "terralith:sakura_valley")),
            Map.entry(NorthernPlainsRoster.BIRCH_TAIGA, List.of("minecraft:forest", "minecraft:flower_forest",
                    "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest",
                    "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                    "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle",
                    "terralith:birch_taiga", "terralith:lavender_forest", "terralith:moonlight_grove",
                    "terralith:sakura_grove", "terralith:tropical_jungle", "terralith:amethyst_rainforest",
                    "terralith:rocky_jungle", "terralith:forested_highlands")),
            Map.entry(NorthernPlainsRoster.SHIELD, List.of("minecraft:windswept_hills",
                    "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:windswept_savanna",
                    "minecraft:savanna_plateau", "terralith:shield", "terralith:shield_clearing",
                    "terralith:arid_highlands", "terralith:yellowstone", "terralith:cloud_forest",
                    "terralith:haze_mountain", "terralith:jungle_mountains", "terralith:stony_spires",
                    "terralith:windswept_spires", "terralith:desert_spires", "terralith:fractured_savanna")),
            Map.entry(NorthernPlainsRoster.SNOWY_SHIELD, List.of("terralith:snowy_shield",
                    "terralith:snowy_maple_forest")),
            Map.entry(NorthernPlainsRoster.ALPINE_HIGHLANDS, List.of("minecraft:meadow", "minecraft:cherry_grove",
                    "terralith:alpine_highlands", "terralith:temperate_highlands", "terralith:highlands",
                    "terralith:blooming_plateau", "terralith:savanna_slopes", "terralith:ashen_savanna")),
            Map.entry(NorthernPlainsRoster.COLD_SHRUBLAND, List.of("terralith:cold_shrubland",
                    "terralith:rocky_shrubland", "terralith:gravel_desert")),
            Map.entry(NorthernPlainsRoster.WINTRY_LOWLANDS, List.of("minecraft:snowy_beach",
                    "terralith:wintry_lowlands", "terralith:snowy_badlands", "terralith:alpha_islands_winter")),
            Map.entry(NorthernPlainsRoster.SNOWY_PLAINS, List.of("minecraft:snowy_plains", "minecraft:frozen_ocean",
                    "minecraft:deep_frozen_ocean")),
            Map.entry(NorthernPlainsRoster.ICE_SPIKES, List.of("minecraft:ice_spikes")),
            Map.entry(NorthernPlainsRoster.ICE_MARSH, List.of("minecraft:swamp", "minecraft:mangrove_swamp",
                    "terralith:orchid_swamp", "terralith:ice_marsh")),
            Map.entry(NorthernPlainsRoster.SNOWY_TAIGA, List.of("minecraft:snowy_taiga")),
            Map.entry(NorthernPlainsRoster.SIBERIAN_TAIGA, List.of("terralith:siberian_taiga",
                    "terralith:siberian_grove")),
            Map.entry(NorthernPlainsRoster.WINTRY_FOREST, List.of("terralith:wintry_forest")),
            Map.entry(NorthernPlainsRoster.GROVE, List.of("minecraft:grove", "minecraft:snowy_slopes",
                    "terralith:alpine_grove", "terralith:snowy_cherry_grove")),
            Map.entry(NorthernPlainsRoster.FROZEN_PEAKS, List.of("minecraft:frozen_peaks", "minecraft:jagged_peaks",
                    "minecraft:stony_peaks", "terralith:rocky_mountains")),
            Map.entry(NorthernPlainsRoster.FROZEN_CLIFFS, List.of("minecraft:badlands", "minecraft:eroded_badlands",
                    "minecraft:wooded_badlands", "terralith:frozen_cliffs", "terralith:emerald_peaks",
                    "terralith:scarlet_mountains", "terralith:white_cliffs",
                    "terralith:granite_cliffs", "terralith:basalt_cliffs", "terralith:volcanic_peaks",
                    "terralith:volcanic_crater", "terralith:caldera", "terralith:painted_mountains",
                    "terralith:savanna_badlands")),
            Map.entry(NorthernPlainsRoster.GLACIAL_CHASM, List.of("terralith:glacial_chasm",
                    "terralith:yosemite_cliffs", "terralith:desert_canyon", "terralith:amethyst_canyon",
                    "terralith:bryce_canyon", "terralith:sandstone_valley", "terralith:white_mesa",
                    "terralith:warped_mesa")),
            Map.entry(Replicas.DRIPSTONE_CAVES, List.of("minecraft:dripstone_caves", "minecraft:deep_dark",
                    "minecraft:lush_caves", "terralith:cave/underground_jungle", "terralith:cave/fungal_caves",
                    "terralith:cave/andesite_caves", "terralith:cave/deep_caves", "terralith:cave/diorite_caves",
                    "terralith:cave/frostfire_caves", "terralith:cave/granite_caves", "terralith:cave/infested_caves",
                    "terralith:cave/mantle_caves", "terralith:cave/thermal_caves", "terralith:cave/tuff_caves")));
    public static final Set<ResourceKey<Biome>> ROSTER = GROUPS.keySet();
    public static final List<ResourceKey<Biome>> MOUNTAINS = List.of(NorthernPlainsRoster.FROZEN_PEAKS,
            NorthernPlainsRoster.FROZEN_CLIFFS, NorthernPlainsRoster.GLACIAL_CHASM);
    public static final List<ResourceKey<Biome>> HILLS = List.of(NorthernPlainsRoster.GROVE,
            NorthernPlainsRoster.ALPINE_HIGHLANDS, NorthernPlainsRoster.SHIELD, NorthernPlainsRoster.SNOWY_SHIELD);
    public static final List<ResourceKey<Biome>> FROZEN = List.of(NorthernPlainsRoster.FROZEN_RIVER,
            NorthernPlainsRoster.COLD_SHRUBLAND, NorthernPlainsRoster.WINTRY_LOWLANDS,
            NorthernPlainsRoster.SNOWY_PLAINS, NorthernPlainsRoster.ICE_SPIKES, NorthernPlainsRoster.ICE_MARSH,
            NorthernPlainsRoster.SNOWY_TAIGA, NorthernPlainsRoster.SIBERIAN_TAIGA, NorthernPlainsRoster.WINTRY_FOREST,
            NorthernPlainsRoster.GROVE, NorthernPlainsRoster.FROZEN_PEAKS, NorthernPlainsRoster.FROZEN_CLIFFS,
            NorthernPlainsRoster.GLACIAL_CHASM);
    private static final Map<ResourceLocation, ResourceKey<Biome>> SOURCES = getSources();
    private static final Set<ResourceKey<Biome>> WATER_AND_CAVES = Set.of(Replicas.RIVER,
            NorthernPlainsRoster.FROZEN_RIVER, Replicas.DRIPSTONE_CAVES);
    public static final List<ResourceKey<Biome>> LAND = ROSTER.stream()
            .filter(biome -> !WATER_AND_CAVES.contains(biome))
            .sorted(Comparator.comparing(ResourceKey::location)).toList();

    private NorthernPlainsLayout() {}

    public static List<Pair<Climate.ParameterPoint, Holder<Biome>>> getParameters(HolderGetter<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters = new ArrayList<>();
        for (Map.Entry<Climate.ParameterPoint, ResourceLocation> entry : Ports.getBiomeParameters()) {
            ResourceKey<Biome> biome = SOURCES.get(entry.getValue());
            if (biome == null) throw new IllegalStateException("no Northern Plains biome for " + entry.getValue());
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
