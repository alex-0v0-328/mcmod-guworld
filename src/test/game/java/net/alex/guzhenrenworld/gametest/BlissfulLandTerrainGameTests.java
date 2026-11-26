package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandBiomes;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandFoxImmortal;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.slf4j.Logger;

/**
 * 狐仙福地的地形与群系取样（种子 {@link RegionProbe#SEED}，不挂水系雕刻器）。
 * <ul>
 *   <li>全疆域：{@link #SAMPLES} × {@link #SAMPLES} 网格、间距 {@link #SPACING} 格，以原点为中心铺满疆域；日志
 *       {@code blissful land terrain shape} 记高度分位、y {@link #HIGHLAND} 以上占比、水面列占比和地表群系占比。断言
 *       最高处在 {@link #PEAK_LOW} 到 {@link #PEAK_HIGH} 之间（照中州的尺度，山峰约 y 250），水面列 2%–30%，地表群系至
 *       少 {@link #MIN_BIOMES} 种、都在名单里（地表与 {@link #SKY} 高空都查）；福地全部群系的地物按装饰步排得出顺序（原
 *       版遇到顺序成环会抛错）。</li>
 *   <li>原点附近不再整形：山心外 {@link #NEAR} 格一圈 {@link #SPOKES} 列的地面高差至少 {@link #NEAR_RELIEF} 格（旧平地
 *       圈全在 y 64）。</li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulLandTerrainGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SAMPLES = 32;
    private static final int SPACING = 608;
    private static final int ORIGIN = -(SAMPLES - 1) * SPACING / 2;
    private static final int HIGHLAND = 120;
    private static final int PEAK_LOW = 200;
    private static final int PEAK_HIGH = 290;
    private static final int MIN_BIOMES = 12;
    private static final int NEAR = 376;
    private static final int SPOKES = 64;
    private static final int NEAR_RELIEF = 8;
    private static final int[] SKY = {250, 310};

    private BlissfulLandTerrainGameTests() {}

    @GameTest(template = "empty9x9x9", timeoutTicks = 2400)
    public static void terrainFollowsTheCentralLands(GameTestHelper helper) {
        RegionProbe probe = new RegionProbe(helper, BlissfulLandFoxImmortal.LEVEL_STEM, BlissfulLandFoxImmortal.MIN_Y,
                BlissfulLandFoxImmortal.HEIGHT);
        int total = SAMPLES * SAMPLES;
        long started = System.nanoTime();
        int[][] columns = IntStream.range(0, total).parallel().mapToObj(index -> {
            int x = ORIGIN + index % SAMPLES * SPACING;
            int z = ORIGIN + index / SAMPLES * SPACING;
            NoiseColumn column = probe.getColumn(x, z);
            return new int[]{x, z, probe.getHeight(column, Heightmap.Types.OCEAN_FLOOR_WG),
                    probe.getHeight(column, Heightmap.Types.WORLD_SURFACE_WG)};
        }).toArray(int[][]::new);
        long millis = (System.nanoTime() - started) / 1_000_000;
        int[] floors = Arrays.stream(columns).mapToInt(column -> column[2]).sorted().toArray();
        long wet = Arrays.stream(columns).filter(column -> column[3] > column[2]).count();
        long highland = Arrays.stream(floors).filter(floor -> floor > HIGHLAND).count();
        Map<String, Integer> biomes = new TreeMap<>();
        int strays = 0;
        for (int[] column : columns) {
            Holder<Biome> biome = probe.getBiome(column[0], column[3], column[1]);
            ResourceKey<Biome> key = biome.unwrapKey().orElseThrow();
            biomes.merge(key.location().getPath(), 1, Integer::sum);
            if (!BlissfulLandBiomes.ROSTER.contains(key)) strays++;
            for (int y : SKY) {
                if (!probe.getBiome(column[0], y, column[1]).is(BlissfulLandBiomes.ROSTER::contains)) strays++;
            }
        }
        StringBuilder shares = new StringBuilder();
        biomes.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> shares.append(' ').append(entry.getKey()).append(' ')
                        .append("%.1f".formatted(100.0 * entry.getValue() / total)).append('%'));
        LOGGER.info("blissful land terrain shape: {} columns in {} ms, floor p10 {} p50 {} p90 {} p99 {} max {}, "
                        + "above y {} {}%, wet {}%, {} surface biomes:{}", total, millis, floors[total / 10],
                floors[total / 2], floors[total * 9 / 10], floors[total * 99 / 100], floors[total - 1], HIGHLAND,
                "%.1f".formatted(100.0 * highland / total), "%.1f".formatted(100.0 * wet / total), biomes.size(),
                shares);
        int peak = floors[total - 1];
        helper.assertTrue(peak >= PEAK_LOW && peak <= PEAK_HIGH, "the highest sampled floor is y " + peak);
        helper.assertTrue(wet >= total / 50 && wet <= total * 3 / 10, wet + " of " + total + " columns hold water");
        helper.assertTrue(biomes.size() >= MIN_BIOMES, "only " + biomes.size() + " surface biomes: " + biomes);
        helper.assertTrue(strays == 0, strays + " samples sit in biomes off the roster: " + biomes);
        List<Holder<Biome>> possible = List.copyOf(probe.getGenerator().getBiomeSource().possibleBiomes());
        FeatureSorter.buildFeaturesPerStep(possible, biome -> biome.value().getGenerationSettings().features(), true);
        checkNatural(helper, probe);
        helper.succeed();
    }

    private static void checkNatural(GameTestHelper helper, RegionProbe probe) {
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int spoke = 0; spoke < SPOKES; spoke++) {
            double angle = 2 * Math.PI * spoke / SPOKES;
            NoiseColumn column = probe.getColumn((int) Math.round(NEAR * Math.cos(angle)),
                    (int) Math.round(NEAR * Math.sin(angle)));
            int floor = probe.getHeight(column, Heightmap.Types.OCEAN_FLOOR_WG);
            lowest = Math.min(lowest, floor);
            highest = Math.max(highest, floor);
        }
        helper.assertTrue(highest - lowest >= NEAR_RELIEF, "the ground " + NEAR + " blocks out spans only y " + lowest
                + " to " + highest + ": the flat ring is back");
    }
}
