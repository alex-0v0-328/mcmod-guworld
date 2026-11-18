package net.alex.guzhenrenworld.waterways;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.jetbrains.annotations.NotNull;

/**
 * 水系规划与河岸过渡使用的刻河前地表高度采样器（定义为最终密度大于零的最高实心方块上方一格）。
 * <ul>
 *   <li>基于剥离了水系雕刻外壳的底层最终密度函数（{@link WaterwaysDensity#getUncarved}），通过 {@link GroundScan} 在不
 *       依赖区块插值的情况下直接扫描地表高度；同时将路由中的 {@code flat_cache} 与 {@code cache_2d} 节点替换为记住上一
 *       水平列求值结果的{@link ColumnMemo}，避免同一列垂直扫描时重复计算水平噪声。</li>
 *   <li>规划阶段调用无猜测值的 {@link #sample(int, int)} 从顶向下扫描；河岸过渡阶段传入预估高度（水面加下切深度）调用带
 *       {@code guess} 的重载，在噪声区块内雕刻时更直接复用当前区块已填充列缓存的地形密度函数副本。</li>
 *   <li>⚠ {@link ColumnMemo} 内部持有可变的上一列坐标与缓存值，因此每个工作线程必须通过 {@link #terrain}（
 *       {@link ThreadLocal}）各自持有独立的密度函数树副本，严禁跨线程共享。</li>
 * </ul>
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
