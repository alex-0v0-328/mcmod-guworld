package net.alex.guzhenrenworld.waterways;

import com.google.common.base.Suppliers;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 单个域维度实例（绑定一个 {@link RandomState}）的水系河槽雕刻器。
 * <ul>
 *   <li>内部延迟持有 {@link WaterwaysIndex}：{@link #getSurface} 计算单列刻河后的目标地表高度供
 *       {@link WaterwaysDensity} 削低地形，{@link #getChannel} 与 {@link #getBed} 查询单列的河道水位、河底与河床方块供
 *       {@link WaterwaysFeature} 铺床注水。</li>
 *   <li>水系索引仅构建一次，可由维度加载时的 {@link #prefetch} 提前触发，或由首个查询的列按需触发；构建任务（
 *       {@link WaterwaysBuild}）排进全局单线程低优先级队列（{@link WaterwaysQueue#SHARED}），一次只建一个维度，规划与
 *       剖面的并行计算另走专用线程池（{@link WaterwaysPool}）；构建完成前所有调用方在 {@link CompletableFuture#join()}
 *       上等待（排队中也一样），若构建异常则记录错误日志并回退为空索引（该域保持不刻河）。玩家进入某维度时
 *       {@link WaterwaysRuntime#prioritize} 调 {@link #promote}，把仍在排队的建图提到队首。维度卸载或停服时
 *       {@link WaterwaysRuntime} 调 {@link #cancel}：排队中的任务直接出队并回退为空索引，运行中的规划阶段地面扫描随即
 *       抛出 {@link CancellationException} 停下，回退为空索引且不写存档，免得单人退回标题后旧世界的建图继续占满线程、重
 *       进时再起第二份。</li>
 *   <li>禁区内的列不等索引：离禁区中心（{@link Waterways#getKeepOutCentre}，首次用到时取一次）不到
 *       {@link Waterways.Region#keepOut()} 减 {@link Waterways#KEEP_OUT_REACH} 格的列一定不刻河、不放水，直接返回。主模
 *       组在主线程上同步生成荡魂山起点区块与正南 160 格的入口区块，等福地建图（几十秒）会被看门狗判卡死。</li>
 *   <li>远离开河网格桶的水平列只需一次桶数组判空即可快速跳过；落入有效桶的列先叠加 {@link Waterways#MEANDER} 蜿蜒偏移，
 *       仅当落入岸带过渡区时才通过 {@link GroundHeights} 从水面加下切深度处采样原地面高度（在噪声区块内直接复用传入的本
 *       区块地形副本）。</li>
 *   <li>由于噪声生成会对同一水平列的每个实心高度格重复调用 {@link #getSurface}，每个线程通过 {@link #columns} 维护一个
 *       {@link #COLUMNS} 槽位的直接映射列缓存，使同一水平列只求值一次。</li>
 * </ul>
 */

public final class WaterwaysCarver {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COLUMNS_BITS = 5;
    private static final int COLUMNS = 1 << (2 * COLUMNS_BITS);
    private static final int COLUMN_MASK = (1 << COLUMNS_BITS) - 1;

    private final Waterways.Region region;
    private final GroundHeights ground;
    private final NormalNoise meander;
    private final double keptOut;
    private final Supplier<BlockPos> centre;
    private final WaterwaysBuild<WaterwaysIndex> build;
    private final ThreadLocal<Columns> columns = ThreadLocal.withInitial(Columns::new);
    private volatile WaterwaysIndex ready;

    WaterwaysCarver(Waterways.Region region, NoiseBasedChunkGenerator generator, RandomState random,
            BiFunction<GroundHeights, BooleanSupplier, WaterwaysIndex> builder) {
        NoiseSettings noise = generator.generatorSettings().value().noiseSettings();
        this.region = region;
        this.ground = new GroundHeights(random.router(), noise.minY(), noise.minY() + noise.height());
        this.build = new WaterwaysBuild<>(WaterwaysQueue.SHARED, region.name(), cancelled -> {
            long started = System.nanoTime();
            WaterwaysIndex index = builder.apply(ground, cancelled);
            LOGGER.info("Waterways of {}: {} segments in {} s", region.name(), index.segments(),
                    "%.1f".formatted((System.nanoTime() - started) / 1e9));
            return index;
        }, () -> WaterwaysIndex.empty(region));
        this.meander = random.getOrCreateNoise(Waterways.MEANDER_NOISE);
        double reach = region.keepOut() - Waterways.KEEP_OUT_REACH;
        this.keptOut = region.keepOut() > 0 && reach > 0 ? reach * reach : -1.0;
        this.centre = Suppliers.memoize(() -> Waterways.getKeepOutCentre(generator, random));
    }

    public static WaterwaysCarver create(Waterways.Region region, NoiseBasedChunkGenerator generator,
            RandomState random, long seed) {
        return new WaterwaysCarver(region, generator, random, (ground, cancelled) -> WaterwaysIndex.of(
                WaterwaysPlanner.plan(region, generator, random, seed, cancelled), ground,
                random.getOrCreateNoise(Waterways.MEANDER_NOISE)));
    }

    public Waterways.Region region() {
        return region;
    }

    public void prefetch() {
        build.start();
    }

    void promote() {
        build.promote();
    }

    void cancel() {
        build.cancel();
    }

    public WaterwaysIndex getIndex() {
        WaterwaysIndex built = ready;
        if (built != null) return built;
        built = build.start().join();
        ready = built;
        return built;
    }

    public double getSurface(int x, int z) {
        return getSurface(x, z, null);
    }

    public @Nullable WaterwaysIndex.Channel getChannel(int x, int z) {
        if (isKeptOut(x, z)) return null;
        WaterwaysIndex index = getIndex();
        int bucket = index.getBucket(x, z);
        if (bucket < 0) return null;
        return index.getChannel(bucket, x + Waterways.getMeanderX(meander, x, z),
                z + Waterways.getMeanderZ(meander, x, z));
    }

    private boolean isKeptOut(int x, int z) {
        if (keptOut < 0) return false;
        BlockPos at = centre.get();
        double dx = x - at.getX();
        double dz = z - at.getZ();
        return dx * dx + dz * dz < keptOut;
    }

    public BlockState getBed(int x, int z) {
        return region.bed().getBlock(meander, x, z);
    }

    double getSurface(int x, int z, @Nullable DensityFunction terrain) {
        if (isKeptOut(x, z)) return Double.POSITIVE_INFINITY;
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

    private static final class Columns {

        private final int[] x = new int[COLUMNS];
        private final int[] z = new int[COLUMNS];
        private final double[] surface = new double[COLUMNS];

        private Columns() {
            Arrays.fill(x, Integer.MIN_VALUE);
        }
    }
}
