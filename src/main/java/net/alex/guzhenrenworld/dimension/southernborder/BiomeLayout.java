package net.alex.guzhenrenworld.dimension.southernborder;

import static net.alex.guzhenrenworld.biome.BiomeTable.Cell.of;
import static net.alex.guzhenrenworld.biome.Ports.CLOUD_FOREST;
import static net.alex.guzhenrenworld.biome.Ports.FORESTED_PLATEAU;
import static net.alex.guzhenrenworld.biome.Ports.HOT_SPRINGS;
import static net.alex.guzhenrenworld.biome.Ports.MISTY_MOUNTAINS;
import static net.alex.guzhenrenworld.biome.Ports.ORCHID_SWAMP;
import static net.alex.guzhenrenworld.biome.Ports.PEACH_GROVE;
import static net.alex.guzhenrenworld.biome.Ports.PLATEAU;
import static net.alex.guzhenrenworld.biome.Ports.RAINFOREST;
import static net.alex.guzhenrenworld.biome.Replicas.BAMBOO_JUNGLE;
import static net.alex.guzhenrenworld.biome.Replicas.BIRCH_FOREST;
import static net.alex.guzhenrenworld.biome.Replicas.DARK_FOREST;
import static net.alex.guzhenrenworld.biome.Replicas.FOREST;
import static net.alex.guzhenrenworld.biome.Replicas.MEADOW;
import static net.alex.guzhenrenworld.biome.Replicas.PLAINS;
import static net.alex.guzhenrenworld.biome.Replicas.STONY_PEAKS;
import static net.alex.guzhenrenworld.biome.Replicas.TAIGA;
import static net.alex.guzhenrenworld.biome.Replicas.WINDSWEPT_HILLS;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.BiomeTable;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;

/**
 * The Southern Border [南疆] biome table on the shared {@link BiomeTable}: Gu World's vanilla replicas, the biomes
 * ported from Terralith, and the region's four landform biomes, which {@link BiomeSettings} builds.
 *
 * <p>The relief axis is {@code Terrain}'s elevation, so the five {@link #ELEVATIONS} bands are, in
 * blocks, lowland below y 90 (valley floors, plains, floodplains), hills to 150 (the lower benches), mid slopes to
 * 220 (the upper benches), high slopes to 280 and peaks above; each band carries its own family:
 * <ul>
 *   <li>Lowland: plains and meadows, forests from birch to dark forest, bamboo, peach groves, and in the warm, wet
 *   south rainforest and orchid swamps.</li>
 *   <li>Hills: the plateau family on the benches, forests, cloud forest where it is wet, rainforest where it is hot;
 *   hot springs rare on the cool to mild, dry to medium plateaus.</li>
 *   <li>Mid slopes: plateaus, taiga, misty mountains and cloud forest; hot springs rare again.</li>
 *   <li>High slopes: windswept hills, taiga and misty mountains.</li>
 *   <li>Peaks: stony peaks.</li>
 * </ul>
 * The weirdness is a 512-block variant noise with its sign kept ({@code Terrain}), so neighboring
 * patches of one climate alternate between a cell's normal and variant biome, and the noise's far tops carry the
 * rare one.
 *
 * <p>The landform code stands in as continentalness: the ordinary table takes the band below 0.1, where the code is
 * -1, and each of the four {@link #LANDFORMS} takes the band of its code in {@code Landforms.CODES},
 * whatever the altitude and climate. Rivers take the weirdness band within {@link Rivers#RIVER_WEIRDNESS} of zero on
 * the two lowest bands, through the landforms too; the terrain sets the weirdness inside that band exactly where a
 * channel holds water. Lush caves sit under the wettest humidity band and dripstone caves under the driest, between
 * {@link #CAVE_DEPTH}, under ordinary ground only.
 *
 * <p>None of these biomes joins a vanilla biome tag, so no vanilla structure starts here, the strongholds included,
 * and nothing another mod adds to the overworld's biomes reaches them. {@link #LAND}, every surface biome but the
 * river, is where the spirit spring [元泉] may stand. The language table names the {@link #LANDFORMS} in their order.
 *
 * <p>⚠ The table is a first prototype for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see Terrain
 * @since 1.0.0
 */

public final class BiomeLayout {

    public static final ResourceKey<Biome> KARST_PEAK_FOREST = key("karst_peak_forest");
    public static final ResourceKey<Biome> DANXIA = key("danxia");
    public static final ResourceKey<Biome> SANDSTONE_PILLARS = key("sandstone_pillars");
    public static final ResourceKey<Biome> ZHANGSHIYAN_CLIFFS = key("zhangshiyan_cliffs");
    public static final List<ResourceKey<Biome>> LANDFORMS =
            List.of(KARST_PEAK_FOREST, DANXIA, SANDSTONE_PILLARS, ZHANGSHIYAN_CLIFFS);
    public static final List<ResourceKey<Biome>> LAND = getLand();
    private static final List<Climate.Parameter> ELEVATIONS = List.of(
            Climate.Parameter.span(-1.0F, -0.704F),
            Climate.Parameter.span(-0.704F, -0.259F),
            Climate.Parameter.span(-0.259F, 0.259F),
            Climate.Parameter.span(0.259F, 0.704F),
            Climate.Parameter.span(0.704F, 1.0F));
    private static final Climate.Parameter ORDINARY = Climate.Parameter.span(-1.0F, 0.1F);
    private static final Climate.Parameter LOWLANDS = Climate.Parameter.span(ELEVATIONS.get(0), ELEVATIONS.get(1));
    private static final Climate.Parameter CAVE_DEPTH = Climate.Parameter.span(0.2F, 0.9F);
    private static final List<List<BiomeTable.Cell>> LOWLAND = List.of(
            List.of(of(PLAINS, MEADOW), of(PLAINS, MEADOW), of(TAIGA, FOREST), of(TAIGA, FOREST),
                    of(TAIGA, DARK_FOREST)),
            List.of(of(PLAINS, MEADOW), of(PLAINS, MEADOW), of(FOREST, BIRCH_FOREST), of(FOREST, DARK_FOREST),
                    of(DARK_FOREST, ORCHID_SWAMP)),
            List.of(of(PLAINS, MEADOW), of(PLAINS, PEACH_GROVE), of(FOREST, PEACH_GROVE), of(BAMBOO_JUNGLE, FOREST),
                    of(ORCHID_SWAMP, DARK_FOREST)),
            List.of(of(PLAINS), of(PLAINS, PEACH_GROVE), of(FOREST, BAMBOO_JUNGLE), of(RAINFOREST, BAMBOO_JUNGLE),
                    of(ORCHID_SWAMP, RAINFOREST)),
            List.of(of(PLAINS), of(PLAINS, RAINFOREST), of(RAINFOREST, BAMBOO_JUNGLE),
                    of(RAINFOREST, BAMBOO_JUNGLE), of(RAINFOREST, ORCHID_SWAMP)));
    private static final List<List<BiomeTable.Cell>> HILLS = List.of(
            List.of(of(TAIGA, WINDSWEPT_HILLS), of(TAIGA, FOREST), of(TAIGA, FOREST), of(TAIGA, DARK_FOREST),
                    of(TAIGA, DARK_FOREST)),
            List.of(of(MEADOW, PLATEAU), new BiomeTable.Cell(PLATEAU, MEADOW, HOT_SPRINGS),
                    new BiomeTable.Cell(FORESTED_PLATEAU, BIRCH_FOREST, HOT_SPRINGS), of(FOREST, DARK_FOREST),
                    of(CLOUD_FOREST, DARK_FOREST)),
            List.of(of(MEADOW, PLATEAU), new BiomeTable.Cell(PLATEAU, PEACH_GROVE, HOT_SPRINGS),
                    new BiomeTable.Cell(FORESTED_PLATEAU, FOREST, HOT_SPRINGS), of(BAMBOO_JUNGLE, FOREST),
                    of(CLOUD_FOREST, MISTY_MOUNTAINS)),
            List.of(of(PLATEAU, MEADOW), of(PLATEAU, PEACH_GROVE), of(FOREST, FORESTED_PLATEAU),
                    of(BAMBOO_JUNGLE, RAINFOREST), of(RAINFOREST, CLOUD_FOREST)),
            List.of(of(PLATEAU, MEADOW), of(PLATEAU, FORESTED_PLATEAU), of(RAINFOREST, BAMBOO_JUNGLE),
                    of(RAINFOREST, BAMBOO_JUNGLE), of(RAINFOREST, CLOUD_FOREST)));
    private static final List<List<BiomeTable.Cell>> MID_SLOPES = List.of(
            List.of(of(TAIGA, WINDSWEPT_HILLS), of(TAIGA, WINDSWEPT_HILLS), of(TAIGA, WINDSWEPT_HILLS),
                    of(TAIGA, WINDSWEPT_HILLS), of(TAIGA, WINDSWEPT_HILLS)),
            List.of(of(WINDSWEPT_HILLS, PLATEAU), new BiomeTable.Cell(TAIGA, FORESTED_PLATEAU, HOT_SPRINGS),
                    of(TAIGA, BIRCH_FOREST), of(MISTY_MOUNTAINS, TAIGA), of(CLOUD_FOREST, MISTY_MOUNTAINS)),
            List.of(of(PLATEAU, WINDSWEPT_HILLS), new BiomeTable.Cell(FORESTED_PLATEAU, TAIGA, HOT_SPRINGS),
                    of(MISTY_MOUNTAINS, FORESTED_PLATEAU), of(CLOUD_FOREST, MISTY_MOUNTAINS),
                    of(CLOUD_FOREST, MISTY_MOUNTAINS)),
            List.of(of(PLATEAU, MEADOW), new BiomeTable.Cell(FORESTED_PLATEAU, PLATEAU, HOT_SPRINGS),
                    of(FOREST, MISTY_MOUNTAINS), of(CLOUD_FOREST, MISTY_MOUNTAINS), of(CLOUD_FOREST, RAINFOREST)),
            List.of(of(PLATEAU, MEADOW), of(FORESTED_PLATEAU, PLATEAU), of(RAINFOREST, MISTY_MOUNTAINS),
                    of(CLOUD_FOREST, RAINFOREST), of(CLOUD_FOREST, MISTY_MOUNTAINS)));
    private static final List<BiomeTable.Cell> HIGH_SLOPE_WARM = List.of(of(WINDSWEPT_HILLS, PLATEAU),
            of(WINDSWEPT_HILLS, MISTY_MOUNTAINS), of(MISTY_MOUNTAINS, TAIGA), of(MISTY_MOUNTAINS, CLOUD_FOREST),
            of(MISTY_MOUNTAINS));
    private static final List<List<BiomeTable.Cell>> HIGH_SLOPES = List.of(
            List.of(of(WINDSWEPT_HILLS, STONY_PEAKS), of(WINDSWEPT_HILLS, TAIGA), of(TAIGA, WINDSWEPT_HILLS),
                    of(TAIGA), of(TAIGA)),
            List.of(of(WINDSWEPT_HILLS), of(WINDSWEPT_HILLS, TAIGA), of(TAIGA, MISTY_MOUNTAINS),
                    of(MISTY_MOUNTAINS, TAIGA), of(MISTY_MOUNTAINS)),
            HIGH_SLOPE_WARM, HIGH_SLOPE_WARM, HIGH_SLOPE_WARM);
    private static final List<BiomeTable.Cell> PEAK = List.of(of(STONY_PEAKS), of(STONY_PEAKS), of(STONY_PEAKS),
            of(STONY_PEAKS), of(STONY_PEAKS));
    private static final List<List<BiomeTable.Cell>> PEAKS = List.of(PEAK, PEAK, PEAK, PEAK, PEAK);

    private BiomeLayout() {}

    public static Climate.ParameterList<Holder<Biome>> getParameters(HolderGetter<Biome> biomes) {
        BiomeTable table = new BiomeTable(ORDINARY, Rivers.RIVER_WEIRDNESS)
                .addBand(ELEVATIONS.get(0), LOWLAND)
                .addBand(ELEVATIONS.get(1), HILLS)
                .addBand(ELEVATIONS.get(2), MID_SLOPES)
                .addBand(ELEVATIONS.get(3), HIGH_SLOPES)
                .addBand(ELEVATIONS.get(4), PEAKS);
        for (int index = 0; index < LANDFORMS.size(); index++) {
            float code = Landforms.CODES.get(index);
            table.addFixed(LANDFORMS.get(index), Climate.Parameter.span(code - 0.1F, code + 0.1F));
        }
        return table.addRiver(Replicas.RIVER, LOWLANDS)
                .addCave(Replicas.LUSH_CAVES, BiomeTable.HUMIDITIES.getLast(), CAVE_DEPTH)
                .addCave(Replicas.DRIPSTONE_CAVES, BiomeTable.HUMIDITIES.getFirst(), CAVE_DEPTH)
                .build(biomes);
    }

    private static List<ResourceKey<Biome>> getLand() {
        List<ResourceKey<Biome>> land = new ArrayList<>(List.of(PLAINS, FOREST, BIRCH_FOREST, DARK_FOREST,
                BAMBOO_JUNGLE, MEADOW, TAIGA, WINDSWEPT_HILLS, STONY_PEAKS));
        land.addAll(Ports.BIOMES);
        land.addAll(LANDFORMS);
        return List.copyOf(land);
    }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, GuWorld.id(name));
    }
}
