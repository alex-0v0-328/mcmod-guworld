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
 * 一个区域在一个世界（一个 {@link RandomState}）里的刻河。
 *
 * <ul>
 *   <li>持 {@link WaterwaysIndex}；{@link #getSurface} 为一列刻后地表，供 {@link WaterwaysDensity}
 *       削地形；{@link #getChannel}、{@link #getBed} 为该列河道与河床方块，供 {@link WaterwaysFeature} 放水。</li>
 *   <li>索引在独立线程只建一次，由 {@link #prefetch} 或首个来问的列触发；其间来问者都等（区块工作线程经 fork-join 托管
 *       阻塞），服务端线程也等，故运行时在维度加载时即预建。</li>
 *   <li>建图失败或无法启动：记错误，区域不刻河；{@link #create} 总从规划建，{@link WaterwaysRuntime} 先试存档。</li>
 *   <li>远离溪流的列只多读一次桶；近处按摆动噪声（截到 ±1）偏移 ≤ {@link Waterways#MEANDER} 格，岸带伸到才经
 *       {@link GroundHeights} 从水面加下切处读地面（噪声区块内用 {@link WaterwaysDensity} 传入的本区块地形副本）。</li>
 *   <li>地表存入每线程 {@link #COLUMNS} 槽直接映射表：地形对一列每个实心方块都问一次。</li>
 * </ul>
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
