package net.alex.guzhenrenworld.waterways;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.jetbrains.annotations.NotNull;

/**
 * The ground height the waterways [水系] planner reads: one above the highest block whose final density is above
 * zero, found by scanning a column down from the top {@link #STEP} blocks at a time and then block by block,
 * uninterpolated, through a copy of the region's router in which every {@code flat_cache} and {@code cache_2d} keeps
 * the last column's value, with the waterways carving left out ({@link WaterwaysDensity#getUncarved}): the plan and
 * the banks read the terrain before it. A noise chunk per column costs about 16 ms with Gu World's routers; this
 * costs 0.15 to 0.18 ms and lands within 1 to 2 blocks of it at the 95th percentile, 6 to 10 at worst on land (the P0
 * benchmark, wiki《Gu World 维护说明#地形性能工具》). A {@code flat_cache} computes its input at y 0, as a noise
 * chunk's does.
 *
 * <p>The banks of a stream ask with a guess, the stream's water surface plus the incision: the scan starts there,
 * climbs {@link #CLIMB} blocks at a time while solid and refines, or walks down up to {@link #DESCENT} blocks before
 * falling back to the coarse steps. That takes a handful of samples instead of some forty, and finds the ground
 * nearest the stream: under an overhang, the floor, not the overhang's top. A noise chunk's carving scans through the
 * chunk's own copy of the terrain instead, whose column caches already hold the chunk's columns.
 *
 * <p>⚠ The column memos are mutable, so every thread scans through its own copy of the router.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class GroundHeights {

    private static final int STEP = 8;
    private static final int CLIMB = 4;
    private static final int DESCENT = 8;
    private static final KeyDispatchDataCodec<? extends DensityFunction> FLAT_CACHE =
            DensityFunctions.flatCache(DensityFunctions.zero()).codec();
    private static final KeyDispatchDataCodec<? extends DensityFunction> CACHE_2D =
            DensityFunctions.cache2d(DensityFunctions.zero()).codec();

    private final ThreadLocal<DensityFunction> terrain;
    private final int minY;
    private final int topY;

    public GroundHeights(NoiseRouter router, int minY, int topY) {
        DensityFunction finalDensity = WaterwaysDensity.getUncarved(router);
        this.terrain = ThreadLocal.withInitial(() -> finalDensity.mapAll(GroundHeights::memoize));
        this.minY = minY;
        this.topY = topY;
    }

    public int sample(int x, int z) {
        return scanDown(terrain.get(), x, topY - STEP, z);
    }

    public int sample(int x, int z, int guess) {
        return sample(terrain.get(), x, z, guess);
    }

    public int sample(DensityFunction density, int x, int z, int guess) {
        int y = Math.clamp(guess, minY, topY - 1);
        if (isSolid(density, x, y, z)) {
            int air = y + CLIMB;
            while (air < topY && isSolid(density, x, air, z)) {
                y = air;
                air += CLIMB;
            }
            for (int fine = Math.min(air, topY) - 1; fine > y; fine--) {
                if (isSolid(density, x, fine, z)) return fine + 1;
            }
            return y + 1;
        }
        int floor = Math.max(minY, y - DESCENT);
        for (int fine = y - 1; fine >= floor; fine--) {
            if (isSolid(density, x, fine, z)) return fine + 1;
        }
        return floor > minY ? scanDown(density, x, floor - STEP, z) : minY;
    }

    private int scanDown(DensityFunction density, int x, int start, int z) {
        for (int y = start; y >= minY; y -= STEP) {
            if (!isSolid(density, x, y, z)) continue;
            for (int fine = y + STEP - 1; fine > y; fine--) {
                if (isSolid(density, x, fine, z)) return fine + 1;
            }
            return y + 1;
        }
        return minY;
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
