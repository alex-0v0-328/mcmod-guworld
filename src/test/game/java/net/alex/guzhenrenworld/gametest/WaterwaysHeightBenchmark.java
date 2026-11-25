package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSea;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlains;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesert;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import static net.alex.guzhenrenworld.dimension.region.Region.HEIGHT;
import static net.alex.guzhenrenworld.dimension.region.Region.MIN_Y;

/**
 * 水系规划高度来源的可选 P0 基准。
 * <ul>
 *   <li>仅当 GameTest 服务端带 {@code -Dguworld.waterwaysBenchmark=true} 时单独成批；四域经 {@link RegionProbe}
 *       （种子 12345）在区域测试 32×32 网格（间距 256 格）比较廉价地面高度估计与整列取样 {@code OCEAN_FLOOR_WG}。</li>
 *   <li>深度零点估计：y 256 读一次路由深度，解 Tectonic 深度 {@code 1 - y / 128 + offset} 的零点；
 *       路由的深度上限 0.8 到 y 358 才生效。</li>
 *   <li>扫描估计：不含 jaggedness 的初始密度自顶向下扫，8 格一步再逐格，分别以 0 与预估地表的 0.390625 为阈值；
 *       最终密度同法以 0 为阈值、不插值；各 {@code flat_cache}、{@code cache_2d} 按列记忆，免每列建噪声区块。</li>
 *   <li>每种估计记绝对误差（中位、95 分位、最大）、偏差、最小二乘拟合及拟合后 95 分位、海平面以上陆地的同样数值、
 *       三轮计时中每样本最佳微秒数；只在读取出错时失败。</li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
public final class WaterwaysHeightBenchmark {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SWITCH = "guworld.waterwaysBenchmark";
    private static final int SAMPLES = 32;
    private static final int SPACING = 256;
    private static final int ORIGIN = (1 - SAMPLES) * SPACING / 2;
    private static final int ROUNDS = 3;
    private static final int DEPTH_Y = 256;
    private static final double DEPTH_CAP = 0.8;
    private static final double PRELIMINARY_SURFACE = 0.390625;
    private static final int SCAN_STEP = 8;
    private static final KeyDispatchDataCodec<? extends DensityFunction> FLAT_CACHE =
            DensityFunctions.flatCache(DensityFunctions.zero()).codec();
    private static final KeyDispatchDataCodec<? extends DensityFunction> CACHE_2D =
            DensityFunctions.cache2d(DensityFunctions.zero()).codec();
    private static final List<Region> REGIONS = List.of(
            new Region(SouthernBorder.REGION.name(), SouthernBorder.REGION.levelStem(), MIN_Y, HEIGHT),
            new Region(WesternDesert.REGION.name(), WesternDesert.REGION.levelStem(), MIN_Y, HEIGHT),
            new Region(EasternSea.REGION.name(), EasternSea.REGION.levelStem(), MIN_Y, HEIGHT),
            new Region(NorthernPlains.REGION.name(), NorthernPlains.REGION.levelStem(), MIN_Y, HEIGHT));

    private WaterwaysHeightBenchmark() {}

    @GameTestGenerator
    public static List<TestFunction> generateBenchmark() {
        if (!Boolean.getBoolean(SWITCH)) return List.of();
        return List.of(new TestFunction("waterwaysBenchmark", "waterwaysheightbenchmark",
                GuWorld.MOD_ID + ":empty9x9x9", 100, 0, true, WaterwaysHeightBenchmark::measure));
    }

    private static void measure(GameTestHelper helper) {
        for (Region region : REGIONS) {
            measure(helper, region);
        }
        helper.succeed();
    }

    private static void measure(GameTestHelper helper, Region region) {
        RegionProbe probe = new RegionProbe(helper, region.stem(), region.minY(), region.height());
        int count = SAMPLES * SAMPLES;
        int[] truth = new int[count];
        long started = System.nanoTime();
        for (int i = 0; i < count; i++) {
            truth[i] = probe.getHeight(probe.getColumn(x(i), z(i)), Heightmap.Types.OCEAN_FLOOR_WG);
        }
        double truthMillis = (System.nanoTime() - started) / 1e6 / count;
        boolean[] land = new boolean[count];
        int seaLevel = probe.getSeaLevel();
        for (int i = 0; i < count; i++) {
            land[i] = truth[i] > seaLevel;
        }

        NoiseRouter router = probe.getRouter();
        DensityFunction depth = router.depth();
        int[] capped = new int[1];
        IntToDoubleFunction depthEstimate = i -> {
            double value = depth.compute(new DensityFunction.SinglePointContext(x(i), DEPTH_Y, z(i)));
            if (value >= DEPTH_CAP - 1e-9) capped[0]++;
            return 128 * (value + 2);
        };
        DensityFunction initial = router.initialDensityWithoutJaggedness()
                .mapAll(WaterwaysHeightBenchmark::memoize);
        int topY = region.minY() + region.height();
        IntToDoubleFunction zeroScan = i -> scan(initial, x(i), z(i), 0, region.minY(), topY);
        IntToDoubleFunction surfaceScan = i -> scan(initial, x(i), z(i), PRELIMINARY_SURFACE, region.minY(), topY);
        DensityFunction terrain = router.finalDensity().mapAll(WaterwaysHeightBenchmark::memoize);
        IntToDoubleFunction finalScan = i -> scan(terrain, x(i), z(i), 0, region.minY(), topY);

        List<String> lines = new ArrayList<>();
        lines.add(report("depth zero", depthEstimate, truth, land));
        lines.add(report("scan > 0", zeroScan, truth, land));
        lines.add(report("scan > 0.39", surfaceScan, truth, land));
        lines.add(report("final > 0", finalScan, truth, land));
        LOGGER.info("waterways height {}: {} columns, {} on land, truth {} ms a column, depth capped {} of {}\n  {}",
                region.name(), count, countOf(land), "%.1f".formatted(truthMillis), capped[0] / (ROUNDS + 2),
                count, String.join("\n  ", lines));
    }

    private static String report(String name, IntToDoubleFunction estimate, int[] truth, boolean[] land) {
        int count = truth.length;
        double[] values = new double[count];
        for (int i = 0; i < count; i++) {
            values[i] = estimate.applyAsDouble(i);
        }
        double best = Double.MAX_VALUE;
        for (int round = 0; round < ROUNDS + 1; round++) {
            long started = System.nanoTime();
            double sink = 0;
            for (int i = 0; i < count; i++) {
                sink += estimate.applyAsDouble(i);
            }
            double micros = (System.nanoTime() - started) / 1e3 / count;
            if (round > 0 && !Double.isNaN(sink)) best = Math.min(best, micros);
        }
        boolean[] all = new boolean[count];
        Arrays.fill(all, true);
        return "%-11s %6.1f us | all %s | land %s".formatted(name, best, errors(values, truth, all),
                errors(values, truth, land));
    }

    private static String errors(double[] values, int[] truth, boolean[] mask) {
        int n = countOf(mask);
        if (n < 2) return "n/a";
        double[] estimate = new double[n];
        double[] actual = new double[n];
        int k = 0;
        for (int i = 0; i < truth.length; i++) {
            if (!mask[i]) continue;
            estimate[k] = values[i];
            actual[k++] = truth[i];
        }
        double meanEstimate = Arrays.stream(estimate).average().orElseThrow();
        double meanActual = Arrays.stream(actual).average().orElseThrow();
        double covariance = 0;
        double variance = 0;
        double[] raw = new double[n];
        for (int i = 0; i < n; i++) {
            covariance += (estimate[i] - meanEstimate) * (actual[i] - meanActual);
            variance += (estimate[i] - meanEstimate) * (estimate[i] - meanEstimate);
            raw[i] = Math.abs(estimate[i] - actual[i]);
        }
        double slope = variance == 0 ? 0 : covariance / variance;
        double intercept = meanActual - slope * meanEstimate;
        double[] fitted = new double[n];
        for (int i = 0; i < n; i++) {
            fitted[i] = Math.abs(slope * estimate[i] + intercept - actual[i]);
        }
        Arrays.sort(raw);
        Arrays.sort(fitted);
        return "n %d median %.1f p95 %.1f max %.1f bias %+.1f fit %.3fx%+.1f p95 %.1f".formatted(n, raw[n / 2],
                percentile95(raw), raw[n - 1], meanEstimate - meanActual, slope, intercept, percentile95(fitted));
    }

    private static double scan(DensityFunction initial, int x, int z, double threshold, int minY, int topY) {
        for (int y = topY - SCAN_STEP; y >= minY; y -= SCAN_STEP) {
            if (initial.compute(new DensityFunction.SinglePointContext(x, y, z)) <= threshold) continue;
            for (int fine = y + SCAN_STEP - 1; fine > y; fine--) {
                if (initial.compute(new DensityFunction.SinglePointContext(x, fine, z)) > threshold) return fine + 1;
            }
            return y + 1;
        }
        return minY;
    }

    private static DensityFunction memoize(DensityFunction function) {
        if (!(function instanceof DensityFunctions.MarkerOrMarked marker)) return function;
        if (marker.codec() == FLAT_CACHE) return new ColumnMemo(marker.wrapped(), true);
        if (marker.codec() == CACHE_2D) return new ColumnMemo(marker.wrapped(), false);
        return function;
    }

    private static double percentile95(double[] sorted) {
        return sorted[Math.max(0, (int) Math.ceil(0.95 * sorted.length) - 1)];
    }

    private static int countOf(boolean[] mask) {
        int n = 0;
        for (boolean value : mask) {
            if (value) n++;
        }
        return n;
    }

    private static int x(int i) {
        return ORIGIN + i % SAMPLES * SPACING;
    }

    private static int z(int i) {
        return ORIGIN + i / SAMPLES * SPACING;
    }

    private record Region(String name, ResourceKey<LevelStem> stem, int minY, int height) {}

    private static final class ColumnMemo implements DensityFunction {

        private final DensityFunction wrapped;
        private final boolean flat;
        private int lastX = Integer.MIN_VALUE;
        private int lastZ = Integer.MIN_VALUE;
        private double last;

        private ColumnMemo(DensityFunction wrapped, boolean flat) {
            this.wrapped = wrapped;
            this.flat = flat;
        }

        @Override
        public double compute(@NotNull FunctionContext context) {
            int x = context.blockX();
            int z = context.blockZ();
            if (x != lastX || z != lastZ) {
                last = wrapped.compute(flat ? new SinglePointContext(x, 0, z) : context);
                lastX = x;
                lastZ = z;
            }
            return last;
        }

        @Override
        public void fillArray(double @NotNull [] array, @NotNull ContextProvider provider) {
            provider.fillAllDirectly(array, this);
        }

        @Override
        public @NotNull DensityFunction mapAll(@NotNull Visitor visitor) {
            return visitor.apply(new ColumnMemo(wrapped.mapAll(visitor), flat));
        }

        @Override
        public double minValue() {
            return wrapped.minValue();
        }

        @Override
        public double maxValue() {
            return wrapped.maxValue();
        }

        @Override
        public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return wrapped.codec();
        }
    }
}
