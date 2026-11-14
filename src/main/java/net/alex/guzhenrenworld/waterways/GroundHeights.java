package net.alex.guzhenrenworld.waterways;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.jetbrains.annotations.NotNull;

/**
 * 水系规划读的地面高度：最终密度大于零的最高方块之上一格。
 *
 * <ul>
 *   <li>{@link GroundScan} 不插值扫出；路由副本里 {@code flat_cache}、{@code cache_2d} 记住上一列的值
 *       （{@code flat_cache} 同噪声区块在 y 0 算输入），且不含刻河（{@link WaterwaysDensity#getUncarved}）：
 *       规划与岸带读刻河前地形。</li>
 *   <li>每列建噪声区块在 Gu World 路由上约 16 ms；本类 0.15–0.18 ms，95 分位误差 1–2 格，陆地最坏 6–10 格（P0 基准，
 *       wiki《Gu World 维护说明#地形性能工具》）。</li>
 *   <li>岸带带猜测值（水面加下切）来问；噪声区块刻河时改扫本区块的地形副本，列缓存现成。</li>
 *   <li>⚠ 列记忆可变：每线程用自己的路由副本。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class GroundHeights {

    private static final KeyDispatchDataCodec<? extends DensityFunction> FLAT_CACHE =
            DensityFunctions.flatCache(DensityFunctions.zero()).codec();
    private static final KeyDispatchDataCodec<? extends DensityFunction> CACHE_2D =
            DensityFunctions.cache2d(DensityFunctions.zero()).codec();

    private final ThreadLocal<DensityFunction> terrain;
    private final GroundScan scan;

    public GroundHeights(NoiseRouter router, int minY, int topY) {
        DensityFunction finalDensity = WaterwaysDensity.getUncarved(router);
        this.terrain = ThreadLocal.withInitial(() -> finalDensity.mapAll(GroundHeights::memoize));
        this.scan = new GroundScan(minY, topY);
    }

    public int sample(int x, int z) {
        return scan.sample(GroundHeights::isSolid, terrain.get(), x, z);
    }

    public int sample(int x, int z, int guess) {
        return sample(terrain.get(), x, z, guess);
    }

    public int sample(DensityFunction density, int x, int z, int guess) {
        return scan.sample(GroundHeights::isSolid, density, x, z, guess);
    }

    private static boolean isSolid(DensityFunction density, int x, int y, int z) {
        return density.compute(new DensityFunction.SinglePointContext(x, y, z)) > 0;
    }

    private static DensityFunction memoize(DensityFunction function) {
        if (!(function instanceof DensityFunctions.MarkerOrMarked marker)) return function;
        if (marker.codec() == FLAT_CACHE) return new ColumnMemo(marker.wrapped(), true);
        if (marker.codec() == CACHE_2D) return new ColumnMemo(marker.wrapped(), false);
        return function;
    }

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
