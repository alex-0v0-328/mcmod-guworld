package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSea;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSeaLayout;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * 东海地形与群系的运行时检查。
 * <ul>
 *   <li>经 {@link RegionProbe} 读取，种子 12345，32×32 网格间距 256 格覆盖全区 8 km，每列只采一次。</li>
 *   <li>记高度分位、全区与自西向东各三分之一的水面占比、带陆地群系的水面柱（Tectonic 远海平岛周围的浅滩）、侵蚀、
 *       大陆度与温度十分位、各群系占比。</li>
 *   <li>最高实心列在 y {@link #LOWEST_PEAK} 到 {@link #HIGHEST_PEAK}：岛屿隆起成丘陵与少数山地，不到维度顶部。</li>
 *   <li>水面占全部列 {@link #WATER_MIN}% 到 {@link #WATER_MAX}%，西三分之一（陆地聚集）{@link #WEST_WATER_MIN}% 到
 *       {@link #WEST_WATER_MAX}%，东三分之一（开阔海）至少 {@link #EAST_WATER_MIN}%。</li>
 *   <li>陆地列至多 {@link #DRY_SEA_MAX}% 带水体群系而非 {@link EasternSeaLayout#LAND}：群系表与地形不符的海岸格。</li>
 *   <li>同批列喂 {@link WorldgenGolden} 哈希，与 golden 文件记录不再逐字节一致即失败。</li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EasternSeaGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int LOWEST_PEAK = 120;
    private static final int HIGHEST_PEAK = 260;
    private static final int WATER_MIN = 75;
    private static final int WATER_MAX = 88;
    private static final int WEST_WATER_MIN = 55;
    private static final int WEST_WATER_MAX = 70;
    private static final int EAST_WATER_MIN = 95;
    private static final int THIRD = RegionProbe.SAMPLES * RegionProbe.SPACING / 6;
    private static final int DRY_SEA_MAX = 2;

    private EasternSeaGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 2000)
    public static void easternSeaMeetsItsTargets(GameTestHelper helper) {
        RegionProbe probe = new RegionProbe(helper, EasternSea.REGION.levelStem(), Region.MIN_Y, Region.HEIGHT);
        int total = RegionProbe.TOTAL;
        int[] heights = new int[total];
        double[] erosion = new double[total];
        double[] continents = new double[total];
        double[] temperature = new double[total];
        Map<String, Integer> counts = new HashMap<>();
        WorldgenGolden golden = new WorldgenGolden("eastern_sea");
        int wet = 0;
        int[] wetThirds = new int[3];
        int[] thirds = new int[3];
        int drySea = 0;
        int wetLand = 0;
        NoiseColumn[] columns = probe.getGridColumns();
        for (int i = 0; i < total; i++) {
            int x = RegionProbe.gridX(i);
            int z = RegionProbe.gridZ(i);
            NoiseColumn column = columns[i];
            heights[i] = probe.getHeight(column, Heightmap.Types.OCEAN_FLOOR_WG);
            int surface = probe.getHeight(column, Heightmap.Types.WORLD_SURFACE_WG);
            boolean water = surface > heights[i];
            int third = x < -THIRD ? 0 : x > THIRD ? 2 : 1;
            thirds[third]++;
            if (water) {
                wet++;
                wetThirds[third]++;
            }
            Holder<Biome> biome = probe.getBiome(x, surface, z);
            golden.add(heights[i], surface, biome);
            erosion[i] = probe.getErosion(x, z);
            continents[i] = probe.getContinentalness(x, z);
            temperature[i] = probe.getTemperature(x, z);
            counts.merge(biome.unwrapKey().orElseThrow().location().getPath(), 1, Integer::sum);
            boolean land = EasternSeaLayout.LAND.stream().anyMatch(biome::is);
            if (!water && !land) drySea++;
            if (water && land) wetLand++;
        }
        golden.verify(helper);
        Arrays.sort(heights);
        int highest = heights[total - 1];
        Map<String, String> shares = new TreeMap<>();
        counts.forEach((name, count) -> shares.put(name, "%.1f%%".formatted(count * 100.0 / total)));
        LOGGER.info("eastern sea heights p10 {} p50 {} p90 {} p99 {} max {}, wet {}%", heights[total / 10],
                heights[total / 2], heights[total * 9 / 10], heights[total * 99 / 100], highest, wet * 100 / total);
        LOGGER.info("eastern sea water west {}% middle {}% east {}%", wetThirds[0] * 100 / thirds[0],
                wetThirds[1] * 100 / thirds[1], wetThirds[2] * 100 / thirds[2]);
        LOGGER.info("eastern sea dry columns with a water biome: {} of {}; wet columns with a land biome: {} of {}",
                drySea, total - wet, wetLand, wet);
        LOGGER.info("eastern sea erosion deciles {}, continentalness deciles {}, temperature deciles {}",
                getDeciles(erosion), getDeciles(continents), getDeciles(temperature));
        LOGGER.info("eastern sea biomes {}", shares);

        helper.assertTrue(highest >= LOWEST_PEAK && highest <= HIGHEST_PEAK,
                "the highest column stands at y " + highest);
        helper.assertTrue(wet * 100 >= total * WATER_MIN && wet * 100 <= total * WATER_MAX,
                wet + " of " + total + " columns are under water");
        helper.assertTrue(wetThirds[0] * 100 >= thirds[0] * WEST_WATER_MIN
                && wetThirds[0] * 100 <= thirds[0] * WEST_WATER_MAX,
                wetThirds[0] + " of " + thirds[0] + " western columns are under water");
        helper.assertTrue(wetThirds[2] * 100 >= thirds[2] * EAST_WATER_MIN,
                wetThirds[2] + " of " + thirds[2] + " eastern columns are under water");
        helper.assertTrue(drySea * 100 <= (total - wet) * DRY_SEA_MAX,
                drySea + " of " + (total - wet) + " dry columns carry a water biome");
        helper.succeed();
    }

    private static List<String> getDeciles(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return Arrays.stream(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 })
                .mapToObj(decile -> "%.3f".formatted(sorted[sorted.length * decile / 10])).toList();
    }
}
