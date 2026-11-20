package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.southernborder.BiomeSettings;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * 南疆地形与群系的运行时检查。
 * <ul>
 *   <li>经 {@link RegionProbe} 读模组维度 JSON（{@code data/guworld/dimension}；GameTest 服务端不加载模组维度），
 *       种子 12345，32×32 网格间距 256 格覆盖全区 8 km，每列只采一次。</li>
 *   <li>最高实心列在 y 270 到 310，Tectonic 地形在 y 320 顶部以下已渐隐为空气；水面不到一成，只有河与湖，无海。</li>
 *   <li>按地表群系：山地（含地貌）最大，35% 到 60%；丘陵与高原、平地与河流各 15% 到 35%。</li>
 *   <li>记高度分位、侵蚀与大陆度十分位、各群系占比，供调参。</li>
 *   <li>同批列喂 {@link WorldgenGolden} 哈希，与 golden 文件记录不再逐字节一致即失败。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SouthernBorderGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<ResourceKey<Biome>> MOUNTAINS = List.of(Ports.MISTY_MOUNTAINS, Ports.CLOUD_FOREST,
            Ports.ROCKY_PEAKS, Ports.EMERALD_PEAKS, Ports.JUNGLE_MOUNTAINS, Ports.PAINTED_MOUNTAINS,
            Ports.GRANITE_CLIFFS, Ports.ALPINE_MEADOW, Ports.ZHANGSHIYAN_CLIFFS, Ports.KARST_PEAK_FOREST);
    private static final List<ResourceKey<Biome>> HILLS = List.of(BiomeSettings.BAMBOO_SEA, Ports.RAINFOREST,
            Ports.PLATEAU, Ports.FORESTED_PLATEAU, Ports.PEACH_GROVE);

    private SouthernBorderGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 2000)
    public static void southernBorderMeetsItsTargets(GameTestHelper helper) {
        RegionProbe probe = new RegionProbe(helper, SouthernBorder.REGION.levelStem(), Region.MIN_Y,
                Region.HEIGHT);
        int total = RegionProbe.TOTAL;
        int[] heights = new int[total];
        double[] erosion = new double[total];
        double[] continents = new double[total];
        Map<String, Integer> counts = new HashMap<>();
        WorldgenGolden golden = new WorldgenGolden("southern_border");
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
            if (surface > heights[i]) wet++;
            Holder<Biome> biome = probe.getBiome(x, surface, z);
            golden.add(heights[i], surface, biome);
            erosion[i] = probe.getErosion(x, z);
            continents[i] = probe.getContinentalness(x, z);
            counts.merge(biome.unwrapKey().orElseThrow().location().getPath(), 1, Integer::sum);
            if (MOUNTAINS.stream().anyMatch(biome::is)) mountains++;
            if (HILLS.stream().anyMatch(biome::is)) hills++;
        }
        golden.verify(helper);
        Arrays.sort(heights);
        int highest = heights[total - 1];
        int flats = total - mountains - hills;
        Map<String, String> shares = new TreeMap<>();
        counts.forEach((name, count) -> shares.put(name, "%.1f%%".formatted(count * 100.0 / total)));
        LOGGER.info("southern border heights p10 {} p50 {} p90 {} p99 {} max {}, wet {}%", heights[total / 10],
                heights[total / 2], heights[total * 9 / 10], heights[total * 99 / 100], highest, wet * 100 / total);
        LOGGER.info("southern border erosion deciles {}, continentalness deciles {}", getDeciles(erosion),
                getDeciles(continents));
        LOGGER.info("southern border bands: mountains {}%, hills {}%, flats {}%; biomes {}", mountains * 100 / total,
                hills * 100 / total, flats * 100 / total, shares);

        helper.assertTrue(highest >= 270 && highest <= Region.TOP_Y - 10,
                "the highest column stands at y " + highest);
        helper.assertTrue(wet * 10 < total, wet + " of " + total + " columns are under water");
        helper.assertTrue(mountains * 20 >= total * 7 && mountains * 10 <= total * 6, "mountains take " + mountains);
        helper.assertTrue(mountains > hills && mountains > flats, "mountains are not the largest band");
        helper.assertTrue(hills * 20 >= total * 3 && hills * 20 <= total * 7, "hills take " + hills);
        helper.assertTrue(flats * 20 >= total * 3 && flats * 20 <= total * 7, "flats take " + flats);
        helper.succeed();
    }

    private static List<String> getDeciles(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return Arrays.stream(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 })
                .mapToObj(decile -> "%.3f".formatted(sorted[sorted.length * decile / 10])).toList();
    }
}
