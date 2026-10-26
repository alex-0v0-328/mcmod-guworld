package net.alex.guzhenrenworld.dimension.westerndesert;

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
 * Where the Western Desert [西漠] places its biomes: Terralith's overworld multi-noise table as shipped
 * ({@link Ports#getBiomeParameters}), with every source biome mapped onto the region's own ({@link #GROUPS}, a
 * roster biome to the source biomes it stands in for). The terrain under it is the region's copy of Tectonic's
 * ({@link WesternDesertTerrain}), which remaps its own erosion and ridges into the bands Terralith's table expects, so
 * each parameter cell keeps the terrain archetype Terralith gave it and gets the region's biome of its kind.
 *
 * <p>The roster, by terrain band:
 * <ul>
 *   <li>Flats: desert (Tectonic's dunes), ancient sands, lush desert, the two oases, hot shrubland, brushland and
 *   gravel desert, and the river.</li>
 *   <li>Hills, mesas and canyons: badlands, wooded badlands, eroded badlands, white mesa, Bryce canyon, desert canyon,
 *   sandstone valley and arid highlands.</li>
 *   <li>Mountains: the badlands' banded terracotta on the warm peaks, the sandstone valley's sandstone on the cold
 *   ones, and desert spires on Terralith's spire cells.</li>
 *   <li>Underground: lush caves and dripstone caves.</li>
 * </ul>
 * A source biome goes to the roster biome of the same landform and the nearest vegetation: forests and jungles to the
 * shrublands, lush desert and wooded badlands, swamps to the oases, warm mountains to the badlands. The whole region is
 * hot ({@link WesternDesertTerrain} lifts the temperature into the warm and hot bands, which the table fills with its
 * deserts, badlands, canyons and dry shrubland), so the cooler cells go to three biomes only: the temperate plains and
 * the cold flats and marshes to the gravel desert, which keeps Terralith's cold climate (the desert's nights), the
 * cold forests, hills and slopes to the arid highlands, the cold peaks and chasms to the sandstone valley. Ocean,
 * beach and island sources are mapped for completeness only:
 * {@link WesternDesertTerrain}'s continentalness never reaches their cells. {@link #ROSTER} is every biome the region
 * can place, the spire cells' desert spires included, and {@link #LAND} every surface biome of it but the river,
 * where the spirit spring [元泉] may stand.
 *
 * <p>Terrain follows erosion, not the biome: every cell of Terralith's spire band, erosion {@link #SPIRE_EROSION} and
 * up, grows Tectonic's spire terrain whatever biome sits there, so a land cell there becomes desert spires
 * ({@link #isSpireCell}) unless it holds one of {@link #KEPT_ON_SPIRES}: the river, the caves, the oases and the two
 * canyon biomes keep their own.
 *
 * <p>⚠ {@link #getParameters} throws on a source biome {@link #GROUPS} does not map, so a re-port that adds one fails
 * {@code runData} instead of dropping cells.
 *
 * @author Alex
 * @version 1.0.0
 * @see WesternDesertTerrain
 * @since 1.0.0
 */

public final class WesternDesertLayout {

    private static final Map<ResourceKey<Biome>, List<String>> GROUPS = Map.ofEntries(
            Map.entry(Replicas.RIVER, List.of("minecraft:river", "minecraft:frozen_river", "terralith:warm_river")),
            Map.entry(WesternDesertRoster.DESERT, List.of("minecraft:desert", "minecraft:beach",
                    "minecraft:mushroom_fields", "minecraft:ocean", "minecraft:deep_ocean", "minecraft:cold_ocean",
                    "minecraft:deep_cold_ocean", "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean",
                    "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:warm_ocean",
                    "terralith:deep_warm_ocean", "terralith:alpha_islands", "terralith:alpha_islands_winter",
                    "terralith:mirage_isles", "terralith:skylands_autumn", "terralith:skylands_spring",
                    "terralith:skylands_summer", "terralith:skylands_winter")),
            Map.entry(WesternDesertRoster.ANCIENT_SANDS, List.of("terralith:ancient_sands")),
            Map.entry(WesternDesertRoster.LUSH_DESERT, List.of("terralith:lush_desert", "minecraft:jungle",
                    "minecraft:dark_forest", "terralith:tropical_jungle", "terralith:amethyst_rainforest")),
            Map.entry(WesternDesertRoster.DESERT_OASIS, List.of("terralith:desert_oasis", "minecraft:swamp",
                    "terralith:orchid_swamp")),
            Map.entry(WesternDesertRoster.RED_OASIS, List.of("terralith:red_oasis", "minecraft:mangrove_swamp")),
            Map.entry(WesternDesertRoster.HOT_SHRUBLAND, List.of("terralith:hot_shrubland", "minecraft:forest",
                    "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                    "minecraft:cherry_grove", "terralith:sakura_grove", "terralith:lavender_forest",
                    "terralith:yellowstone")),
            Map.entry(WesternDesertRoster.BRUSHLAND, List.of("terralith:brushland", "minecraft:savanna")),
            Map.entry(WesternDesertRoster.GRAVEL_DESERT, List.of("terralith:gravel_desert", "minecraft:plains",
                    "minecraft:sunflower_plains", "minecraft:meadow", "terralith:shrubland", "minecraft:snowy_plains",
                    "minecraft:ice_spikes", "minecraft:snowy_beach", "minecraft:stony_shore", "terralith:steppe",
                    "terralith:cold_shrubland", "terralith:rocky_shrubland", "terralith:wintry_lowlands",
                    "terralith:ice_marsh", "terralith:gravel_beach")),
            Map.entry(WesternDesertRoster.ARID_HIGHLANDS, List.of("terralith:arid_highlands",
                    "minecraft:savanna_plateau", "minecraft:windswept_hills", "minecraft:windswept_forest",
                    "minecraft:windswept_gravelly_hills", "minecraft:windswept_savanna", "minecraft:taiga",
                    "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                    "minecraft:grove", "minecraft:snowy_slopes", "terralith:highlands",
                    "terralith:temperate_highlands", "terralith:blooming_plateau", "terralith:shield",
                    "terralith:shield_clearing", "terralith:snowy_shield", "terralith:savanna_slopes",
                    "terralith:snowy_badlands", "terralith:siberian_taiga", "terralith:siberian_grove",
                    "terralith:wintry_forest", "terralith:snowy_maple_forest", "terralith:moonlight_grove",
                    "terralith:birch_taiga", "terralith:alpine_highlands", "terralith:forested_highlands",
                    "terralith:snowy_cherry_grove", "terralith:alpine_grove")),
            Map.entry(WesternDesertRoster.SANDSTONE_VALLEY, List.of("terralith:sandstone_valley",
                    "minecraft:frozen_peaks", "minecraft:jagged_peaks", "terralith:glacial_chasm",
                    "terralith:yosemite_cliffs", "terralith:yosemite_lowlands", "terralith:moonlight_valley",
                    "terralith:lavender_valley", "terralith:lush_valley", "terralith:blooming_valley",
                    "terralith:sakura_valley", "terralith:valley_clearing", "terralith:frozen_cliffs",
                    "terralith:white_cliffs", "terralith:emerald_peaks", "terralith:scarlet_mountains",
                    "terralith:rocky_mountains", "terralith:cloud_forest", "terralith:haze_mountain")),
            Map.entry(WesternDesertRoster.DESERT_CANYON, List.of("terralith:desert_canyon",
                    "terralith:amethyst_canyon")),
            Map.entry(WesternDesertRoster.BADLANDS, List.of("minecraft:badlands", "minecraft:stony_peaks",
                    "terralith:painted_mountains", "terralith:granite_cliffs", "terralith:volcanic_peaks",
                    "terralith:volcanic_crater", "terralith:caldera", "terralith:basalt_cliffs",
                    "terralith:ashen_savanna", "terralith:warped_mesa", "terralith:savanna_badlands")),
            Map.entry(WesternDesertRoster.ERODED_BADLANDS, List.of("minecraft:eroded_badlands")),
            Map.entry(WesternDesertRoster.WOODED_BADLANDS, List.of("minecraft:wooded_badlands",
                    "minecraft:sparse_jungle", "minecraft:bamboo_jungle", "terralith:rocky_jungle",
                    "terralith:jungle_mountains")),
            Map.entry(WesternDesertRoster.WHITE_MESA, List.of("terralith:white_mesa")),
            Map.entry(WesternDesertRoster.BRYCE_CANYON, List.of("terralith:bryce_canyon")),
            Map.entry(WesternDesertRoster.DESERT_SPIRES, List.of("terralith:desert_spires", "terralith:stony_spires",
                    "terralith:windswept_spires", "terralith:fractured_savanna")),
            Map.entry(Replicas.LUSH_CAVES, List.of("minecraft:lush_caves", "terralith:cave/underground_jungle",
                    "terralith:cave/fungal_caves")),
            Map.entry(Replicas.DRIPSTONE_CAVES, List.of("minecraft:dripstone_caves", "minecraft:deep_dark",
                    "terralith:cave/andesite_caves", "terralith:cave/deep_caves", "terralith:cave/diorite_caves",
                    "terralith:cave/frostfire_caves", "terralith:cave/granite_caves", "terralith:cave/infested_caves",
                    "terralith:cave/mantle_caves", "terralith:cave/thermal_caves", "terralith:cave/tuff_caves")));
    public static final Set<ResourceKey<Biome>> ROSTER = GROUPS.keySet();
    private static final Map<ResourceLocation, ResourceKey<Biome>> SOURCES = getSources();
    private static final float SPIRE_EROSION = 0.565F;
    private static final Set<ResourceKey<Biome>> WATER_AND_CAVES = Set.of(Replicas.RIVER, Replicas.LUSH_CAVES,
            Replicas.DRIPSTONE_CAVES);
    private static final Set<ResourceKey<Biome>> KEPT_ON_SPIRES = Set.of(Replicas.RIVER, Replicas.LUSH_CAVES,
            Replicas.DRIPSTONE_CAVES, WesternDesertRoster.DESERT_OASIS, WesternDesertRoster.RED_OASIS,
            WesternDesertRoster.SANDSTONE_VALLEY, WesternDesertRoster.DESERT_CANYON);
    public static final List<ResourceKey<Biome>> LAND = ROSTER.stream()
            .filter(biome -> !WATER_AND_CAVES.contains(biome))
            .sorted(Comparator.comparing(ResourceKey::location)).toList();

    private WesternDesertLayout() {}

    public static List<Pair<Climate.ParameterPoint, Holder<Biome>>> getParameters(HolderGetter<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters = new ArrayList<>();
        for (Map.Entry<Climate.ParameterPoint, ResourceLocation> entry : Ports.getBiomeParameters()) {
            ResourceKey<Biome> biome = SOURCES.get(entry.getValue());
            if (biome == null) throw new IllegalStateException("no Western Desert biome for " + entry.getValue());
            if (isSpireCell(entry.getKey()) && !KEPT_ON_SPIRES.contains(biome)) {
                biome = WesternDesertRoster.DESERT_SPIRES;
            }
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
