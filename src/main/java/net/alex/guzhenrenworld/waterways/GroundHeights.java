package net.alex.guzhenrenworld.waterways;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.jetbrains.annotations.NotNull;

/**
 * The ground height the waterways [水系] planner reads: one above the highest block whose final density is above
 * zero, found by {@link GroundScan}, uninterpolated, through a copy of the region's router in which every
 * {@code flat_cache} and {@code cache_2d} keeps the last column's value, with the waterways carving left out
 * ({@link WaterwaysDensity#getUncarved}): the plan and the banks read the terrain before it. A noise chunk per column
 * costs about 16 ms with Gu World's routers; this costs 0.15 to 0.18 ms and lands within 1 to 2 blocks of it at the
 * 95th percentile, 6 to 10 at worst on land (the P0 benchmark, wiki《Gu World 维护说明#地形性能工具》). A
 * {@code flat_cache} computes its input at y 0, as a noise chunk's does.
 *
 * <p>The banks of a stream ask with a guess, the stream's water surface plus the incision ({@link GroundScan}). A noise
 * chunk's carving scans through the chunk's own copy of the terrain instead, whose column caches already hold the
 * chunk's columns.
 *
 * <p>⚠ The column memos are mutable, so every thread scans through its own copy of the router.
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
