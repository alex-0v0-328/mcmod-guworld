package net.alex.guzhenrenworld.waterways;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The carving of one region in one world (one {@link RandomState}): its {@link WaterwaysIndex}, {@link #getSurface},
 * the carved surface of a column, which {@link WaterwaysDensity} cuts the terrain down to, and {@link #getChannel}
 * with {@link #getBed}, the channel at a column and the block of its bed, which {@link WaterwaysFeature} fills.
 *
 * <p>The index is built once, on a thread of its own ({@link #BUILDER}), from {@link #prefetch} or the first column
 * that asks; every thread that asks meanwhile waits for it, chunk workers through the fork-join pool's managed
 * blocking. A server thread that needs a region chunk before the build ends waits too, so the runtime prefetches as
 * soon as the level loads. A build that fails, or cannot start, logs the error and leaves the region uncarved.
 * {@link #create} builds from the plan every time; {@link WaterwaysRuntime} hands in a builder that tries the world's
 * saved index first.
 *
 * <p>Most columns are far from any stream and cost one bucket read. A column near one warps its point by {@link
 * Waterways#MEANDER} blocks of the meander noise (clamped to one), reads the ground through {@link GroundHeights},
 * starting at the stream's water surface plus the incision, only when a bank reaches it, and keeps its surface in a
 * per-thread direct-mapped table of {@link #COLUMNS} slots, since the terrain asks once for every solid block of the
 * column.
 * Inside a noise chunk the ground is read through the chunk's own copy of the terrain, handed in by {@link
 * WaterwaysDensity}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WaterwaysCarver {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COLUMNS_BITS = 5;
    private static final int COLUMNS = 1 << (2 * COLUMNS_BITS);
    private static final int COLUMN_MASK = (1 << COLUMNS_BITS) - 1;
    private static final ExecutorService BUILDER = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "guworld-waterways");
        thread.setDaemon(true);
        return thread;
    });

    private final Waterways.Region region;
    private final Function<GroundHeights, WaterwaysIndex> builder;
    private final GroundHeights ground;
    private final NormalNoise meander;
    private final AtomicReference<CompletableFuture<WaterwaysIndex>> build = new AtomicReference<>();
    private final ThreadLocal<Columns> columns = ThreadLocal.withInitial(Columns::new);
    private volatile WaterwaysIndex ready;

    WaterwaysCarver(Waterways.Region region, NoiseBasedChunkGenerator generator, RandomState random,
            Function<GroundHeights, WaterwaysIndex> builder) {
        NoiseSettings noise = generator.generatorSettings().value().noiseSettings();
        this.region = region;
        this.builder = builder;
        this.ground = new GroundHeights(random.router(), noise.minY(), noise.minY() + noise.height());
        this.meander = random.getOrCreateNoise(Waterways.MEANDER_NOISE);
    }

    public static WaterwaysCarver create(Waterways.Region region, NoiseBasedChunkGenerator generator,
            RandomState random, long seed) {
        return new WaterwaysCarver(region, generator, random, ground -> WaterwaysIndex.of(
                WaterwaysPlanner.plan(region, generator, random, seed), ground,
                random.getOrCreateNoise(Waterways.MEANDER_NOISE)));
    }

    public Waterways.Region region() {
        return region;
    }

    public void prefetch() {
        getBuild();
    }

    public WaterwaysIndex getIndex() {
        WaterwaysIndex built = ready;
        if (built != null) return built;
        built = getBuild().join();
        ready = built;
        return built;
    }

    public double getSurface(int x, int z) {
        return getSurface(x, z, null);
    }

    public @Nullable WaterwaysIndex.Channel getChannel(int x, int z) {
        WaterwaysIndex index = getIndex();
        int bucket = index.getBucket(x, z);
        if (bucket < 0) return null;
        return index.getChannel(bucket, x + Waterways.getMeanderX(meander, x, z),
                z + Waterways.getMeanderZ(meander, x, z));
    }

    public BlockState getBed(int x, int z) {
        return region.bed().getBlock(meander, x, z);
    }

    double getSurface(int x, int z, @Nullable DensityFunction terrain) {
        WaterwaysIndex index = getIndex();
        int bucket = index.getBucket(x, z);
        if (bucket < 0) return Double.POSITIVE_INFINITY;
        Columns cache = columns.get();
        int slot = (x & COLUMN_MASK) << COLUMNS_BITS | (z & COLUMN_MASK);
        if (cache.x[slot] == x && cache.z[slot] == z) return cache.surface[slot];
        double surface = index.getSurface(bucket, x + Waterways.getMeanderX(meander, x, z),
                z + Waterways.getMeanderZ(meander, x, z), guess -> terrain == null
                ? ground.sample(x, z, (int) Math.round(guess))
                : ground.sample(terrain, x, z, (int) Math.round(guess)));
        cache.x[slot] = x;
        cache.z[slot] = z;
        cache.surface[slot] = surface;
        return surface;
    }

    private CompletableFuture<WaterwaysIndex> getBuild() {
        CompletableFuture<WaterwaysIndex> started = build.get();
        if (started != null) return started;
        CompletableFuture<WaterwaysIndex> fresh = new CompletableFuture<>();
        if (!build.compareAndSet(null, fresh)) return build.get();
        try {
            BUILDER.execute(() -> fresh.complete(getBuilt()));
        } catch (RuntimeException | Error exception) {
            LOGGER.error("Waterways of {} could not start; the region stays uncarved", region.name(), exception);
            fresh.complete(WaterwaysIndex.empty(region));
        }
        return fresh;
    }

    private WaterwaysIndex getBuilt() {
        long started = System.nanoTime();
        try {
            WaterwaysIndex index = builder.apply(ground);
            LOGGER.info("Waterways of {}: {} segments in {} s", region.name(), index.segments(),
                    "%.1f".formatted((System.nanoTime() - started) / 1e9));
            return index;
        } catch (RuntimeException | Error exception) {
            LOGGER.error("Waterways of {} failed; the region stays uncarved", region.name(), exception);
            return WaterwaysIndex.empty(region);
        }
    }

    private static final class Columns {

        private final int[] x = new int[COLUMNS];
        private final int[] z = new int[COLUMNS];
        private final double[] surface = new double[COLUMNS];

        private Columns() {
            Arrays.fill(x, Integer.MIN_VALUE);
        }
    }
}
