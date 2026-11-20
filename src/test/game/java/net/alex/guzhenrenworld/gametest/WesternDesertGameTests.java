package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesert;
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
 * 西漠地形与群系的运行时检查。
 * <ul>
 *   <li>经 {@link RegionProbe} 读取，种子 12345，32×32 网格间距 256 格覆盖全区 8 km，每列只采一次。</li>
 *   <li>最高实心列在 y {@link #LOWEST_TOP} 到 {@link #HIGHEST_TOP}，约 y 200；水面至多 {@link #WET_PERCENT}%：
 *       河道在海平面 52 以下干涸，只有含水层灌满处有水；无海。</li>
 *   <li>三条地形带各在 15%、30%、55% 的十个百分点内：山地，y {@link #MOUNTAIN_Y} 及以上；丘陵、台地与峡谷，Terralith
 *       侵蚀起伏带上侵蚀低于 {@link #FLAT_EROSION} 的较低列；平沙与沙丘，其余。</li>
 *   <li>记高度分位、几档高度以下的列占比、按群系分的有水列、侵蚀、大陆度与温度十分位、各群系占比，供调参。</li>
 *   <li>同批列喂 {@link WorldgenGolden} 哈希，与 golden 文件记录不再逐字节一致即失败。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WesternDesertGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int LOWEST_TOP = 180;
    private static final int HIGHEST_TOP = 220;
    private static final int WET_PERCENT = 3;
    private static final int MOUNTAIN_Y = 130;
    private static final double FLAT_EROSION = 0.05;
    private static final int[] HEIGHT_MARKS = { 70, 80, 90, 100, 120, 140, 160, 180 };

    private WesternDesertGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 2000)
    public static void westernDesertMeetsItsTargets(GameTestHelper helper) {
        RegionProbe probe = new RegionProbe(helper, WesternDesert.REGION.levelStem(), Region.MIN_Y,
                Region.HEIGHT);
        int total = RegionProbe.TOTAL;
        int[] heights = new int[total];
        double[] erosion = new double[total];
        double[] continents = new double[total];
        double[] temperature = new double[total];
        Map<String, Integer> counts = new HashMap<>();
        WorldgenGolden golden = new WorldgenGolden("western_desert");
        Map<String, Integer> wetCounts = new TreeMap<>();
        int wet = 0;
        int mountains = 0;
        int hills = 0;
        NoiseColumn[] columns = probe.getGridColumns();
        for (int i = 0; i < total; i++) {
            int x = RegionProbe.gridX(i);
            int z = RegionProbe.gridZ(i);
            NoiseColumn column = columns[i];
            heights[i] = probe.getHeight(column, Heightmap.Types.OCEAN_FLOOR_WG);
            int surface = probe.getHeight(column, Heightmap.Types.WORLD_SURFACE_WG);
            Holder<Biome> holder = probe.getBiome(x, surface, z);
            golden.add(heights[i], surface, holder);
            String biome = getName(holder);
            erosion[i] = probe.getErosion(x, z);
            continents[i] = probe.getContinentalness(x, z);
            temperature[i] = probe.getTemperature(x, z);
            counts.merge(biome, 1, Integer::sum);
            if (surface > heights[i]) {
                wet++;
                wetCounts.merge(biome, 1, Integer::sum);
            }
            if (heights[i] >= MOUNTAIN_Y) mountains++;
            else if (erosion[i] < FLAT_EROSION) hills++;
        }
        int flats = total - mountains - hills;
        Map<Integer, String> under = new TreeMap<>();
        for (int mark : HEIGHT_MARKS) {
            under.put(mark, "%d%%".formatted(Arrays.stream(heights).filter(height -> height < mark).count() * 100
                    / total));
        }
        golden.verify(helper);
        Arrays.sort(heights);
        int highest = heights[total - 1];
        Map<String, String> shares = new TreeMap<>();
        counts.forEach((name, count) -> shares.put(name, "%.1f%%".formatted(count * 100.0 / total)));
        LOGGER.info("western desert heights p10 {} p50 {} p90 {} p99 {} max {}, under {}", heights[total / 10],
                heights[total / 2], heights[total * 9 / 10], heights[total * 99 / 100], highest, under);
        LOGGER.info("western desert wet {} of {} columns, by biome {}", wet, total, wetCounts);
        LOGGER.info("western desert erosion deciles {}, continentalness deciles {}, temperature deciles {}",
                getDeciles(erosion), getDeciles(continents), getDeciles(temperature));
        LOGGER.info("western desert bands: mountains {}%, hills {}%, flats {}%; biomes {}", mountains * 100 / total,
                hills * 100 / total, flats * 100 / total, shares);

        helper.assertTrue(highest >= LOWEST_TOP && highest <= HIGHEST_TOP, "the highest column stands at y " + highest);
        helper.assertTrue(wet * 100 <= total * WET_PERCENT, wet + " of " + total + " columns are under water");
        helper.assertTrue(mountains * 20 >= total && mountains * 4 <= total, "mountains take " + mountains);
        helper.assertTrue(hills * 5 >= total && hills * 5 <= total * 2, "hills take " + hills);
        helper.assertTrue(flats * 20 >= total * 9 && flats * 20 <= total * 13, "flats take " + flats);
        helper.succeed();
    }

    private static String getName(Holder<Biome> biome) {
        return biome.unwrapKey().orElseThrow().location().getPath();
    }

    private static List<String> getDeciles(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return Arrays.stream(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 })
                .mapToObj(decile -> "%.3f".formatted(sorted[sorted.length * decile / 10])).toList();
    }
}
