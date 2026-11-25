package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlains;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlainsLayout;
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
 * 北原地形与群系的运行时检查。
 * <ul>
 *   <li>经 {@link RegionProbe} 读取，种子 12345，32×32 网格间距 256 格覆盖全区 8 km，每列只采一次。</li>
 *   <li>最高实心列在 y 190 到 240，约 y 220；水面 5% 到 12%，只有河与湖。</li>
 *   <li>按地表群系：平地与河流 55% 到 75%，丘陵与高原 15% 到 35%，山地（含冰川裂谷）2% 到 20%，约 65 / 25 / 10。</li>
 *   <li>纬度梯度：北侧带（z -2048 及以下）至少 80% 的列属冰冻段（{@link NorthernPlainsLayout#FROZEN}），南侧带
 *       （z 2048 及以上）至多 20%。</li>
 *   <li>记高度分位、侵蚀、大陆度与温度十分位、各群系占比，供调参。</li>
 *   <li>同批列喂 {@link WorldgenGolden} 哈希，与 golden 文件记录不再逐字节一致即失败。</li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NorthernPlainsGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int STRIP_EDGE = 2048;

    private NorthernPlainsGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 2000)
    public static void northernPlainsMeetsItsTargets(GameTestHelper helper) {
        RegionProbe probe = new RegionProbe(helper, NorthernPlains.REGION.levelStem(), Region.MIN_Y,
                Region.HEIGHT);
        int total = RegionProbe.TOTAL;
        int[] heights = new int[total];
        double[] erosion = new double[total];
        double[] continents = new double[total];
        double[] temperature = new double[total];
        Map<String, Integer> counts = new HashMap<>();
        WorldgenGolden golden = new WorldgenGolden("northern_plains");
        int wet = 0;
        int mountains = 0;
        int hills = 0;
        int[] strip = new int[2];
        int[] frozen = new int[2];
        NoiseColumn[] columns = probe.getGridColumns();
        for (int i = 0; i < total; i++) {
            int x = RegionProbe.gridX(i);
            int z = RegionProbe.gridZ(i);
            NoiseColumn column = columns[i];
            heights[i] = probe.getHeight(column, Heightmap.Types.OCEAN_FLOOR_WG);
            int surface = probe.getHeight(column, Heightmap.Types.WORLD_SURFACE_WG);
            if (surface > heights[i]) wet++;
            Holder<Biome> biome = probe.getBiome(x, surface, z);
            golden.add(heights[i], surface, biome);
            erosion[i] = probe.getErosion(x, z);
            continents[i] = probe.getContinentalness(x, z);
            temperature[i] = probe.getTemperature(x, z);
            counts.merge(biome.unwrapKey().orElseThrow().location().getPath(), 1, Integer::sum);
            if (NorthernPlainsLayout.MOUNTAINS.stream().anyMatch(biome::is)) mountains++;
            if (NorthernPlainsLayout.HILLS.stream().anyMatch(biome::is)) hills++;
            int side = z <= -STRIP_EDGE ? 0 : z >= STRIP_EDGE ? 1 : -1;
            if (side < 0) continue;
            strip[side]++;
            if (NorthernPlainsLayout.FROZEN.stream().anyMatch(biome::is)) frozen[side]++;
        }
        golden.verify(helper);
        Arrays.sort(heights);
        int highest = heights[total - 1];
        int flats = total - mountains - hills;
        Map<String, String> shares = new TreeMap<>();
        counts.forEach((name, count) -> shares.put(name, "%.1f%%".formatted(count * 100.0 / total)));
        String summary = "heights p10 %d p50 %d p90 %d p99 %d max %d, wet %d%%; bands: mountains %d%%, hills %d%%, "
                .formatted(heights[total / 10], heights[total / 2], heights[total * 9 / 10],
                        heights[total * 99 / 100], highest, wet * 100 / total, mountains * 100 / total,
                        hills * 100 / total)
                + "flats %d%%; frozen north %d%%, south %d%%".formatted(flats * 100 / total,
                        frozen[0] * 100 / strip[0], frozen[1] * 100 / strip[1]);
        String deciles = "erosion deciles %s, continentalness deciles %s, temperature deciles %s".formatted(
                getDeciles(erosion), getDeciles(continents), getDeciles(temperature));
        LOGGER.info("northern plains {}", summary);
        LOGGER.info("northern plains {}", deciles);
        LOGGER.info("northern plains biomes {}", shares);

        helper.assertTrue(highest >= 190 && highest <= 240, "the highest column stands at y " + highest);
        helper.assertTrue(wet * 20 >= total && wet * 25 <= total * 3, wet + " of " + total + " columns are wet");
        helper.assertTrue(flats * 20 >= total * 11 && flats * 20 <= total * 15, "flats take " + flats);
        helper.assertTrue(hills * 20 >= total * 3 && hills * 20 <= total * 7, "hills take " + hills);
        helper.assertTrue(mountains * 50 >= total && mountains * 5 <= total, "mountains take " + mountains);
        helper.assertTrue(frozen[0] * 5 >= strip[0] * 4, frozen[0] + " of " + strip[0] + " northern columns freeze");
        helper.assertTrue(frozen[1] * 5 <= strip[1], frozen[1] + " of " + strip[1] + " southern columns freeze");
        helper.succeed();
    }

    private static List<String> getDeciles(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return Arrays.stream(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 })
                .mapToObj(decile -> "%.3f".formatted(sorted[sorted.length * decile / 10])).toList();
    }
}
