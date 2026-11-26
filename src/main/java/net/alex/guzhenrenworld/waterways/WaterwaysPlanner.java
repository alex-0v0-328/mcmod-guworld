package net.alex.guzhenrenworld.waterways;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * 域水文网格规划（{@link WaterwaysPlan}）的构建器，完全由世界种子与地形决定。
 * <ul>
 *   <li>第一阶段为并行网格采样（{@link #sample}）：将每个网格中心按世界种子与行列散列在 X/Z 轴随机抖动，至多
 *       {@link Waterways.Region#jitter()} 格（16 格网格时为 {@link Waterways#JITTER}），通过 {@link GroundHeights} 在专
 *       用低优先级线程池（{@link WaterwaysPool}）上并行扫描地面高度，并结合海平面与噪声群系（路径以 {@code river} 结尾
 *       视为河口）划分网格类型，禁区与墙边（{@link Waterways.Region#keepOut()}、{@link Waterways.Region#walled()}）记为
 *       墙；512×512 网格约需 45 CPU 秒。
 *       每格开扫前查一次取消标志，维度卸载后抛 {@link CancellationException} 让后台建图尽快停下。</li>
 *   <li>第二阶段为纯内存水文拓扑求解：调用 {@link WaterwaysFlood} 在采样数组上依次完成洼地填平、最陡流向判定与汇流面积
 *       累加，耗时不足 1 秒。</li>
 * </ul>
 */

public final class WaterwaysPlanner {

    private WaterwaysPlanner() {}

    public static WaterwaysPlan plan(Waterways.Region region, NoiseBasedChunkGenerator generator, RandomState random,
            long seed) {
        return plan(region, generator, random, seed, () -> false);
    }

    public static WaterwaysPlan plan(Waterways.Region region, NoiseBasedChunkGenerator generator, RandomState random,
            long seed, BooleanSupplier cancelled) {
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        int seaLevel = settings.seaLevel();
        int size = region.cells();
        int count = size * size;
        int[] x = new int[count];
        int[] z = new int[count];
        for (int cell = 0; cell < count; cell++) {
            int column = cell % size;
            int row = cell / size;
            x[cell] = region.origin() + column * region.cell() + region.cell() / 2
                    + jitter(seed, column, row, 0, region.jitter());
            z[cell] = region.origin() + row * region.cell() + region.cell() / 2
                    + jitter(seed, column, row, 1, region.jitter());
        }
        float[] ground = new float[count];
        WaterwaysPlan.Kind[] kind = new WaterwaysPlan.Kind[count];
        sample(region, generator, random, size, x, z, ground, kind, cancelled);
        double[] filled = new double[count];
        int[] order = new int[count];
        WaterwaysFlood.fill(kind, ground, filled, order, size);
        int[] receiver = WaterwaysFlood.getReceivers(kind, filled, x, z, size);
        int[] catchment = WaterwaysFlood.getCatchments(receiver, order);
        return new WaterwaysPlan(region, seaLevel, x, z, ground, filled, receiver, catchment, kind);
    }

    private static void sample(Waterways.Region region, NoiseBasedChunkGenerator generator, RandomState random,
            int size, int[] x, int[] z, float[] ground, WaterwaysPlan.Kind[] kind, BooleanSupplier cancelled) {
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        int minY = settings.noiseSettings().minY();
        GroundHeights heights = new GroundHeights(random.router(), minY, minY + settings.noiseSettings().height());
        BiomeSource biomes = generator.getBiomeSource();
        Climate.Sampler sampler = random.sampler();
        BlockPos centre = region.keepOut() > 0 ? Waterways.getKeepOutCentre(generator, random) : BlockPos.ZERO;
        WaterwaysPool.forEach(x.length, cell -> {
            if (cancelled.getAsBoolean()) throw new CancellationException(region.name());
            int height = heights.sample(x[cell], z[cell]);
            ground[cell] = height;
            kind[cell] = isWall(region, centre, cell, size, x[cell], z[cell]) ? WaterwaysPlan.Kind.WALL
                    : classify(cell, size, height, settings.seaLevel(), biomes.getNoiseBiome(
                    QuartPos.fromBlock(x[cell]), QuartPos.fromBlock(height), QuartPos.fromBlock(z[cell]), sampler)
                    .is(key -> key.location().getPath().endsWith("river")));
        });
    }

    private static boolean isWall(Waterways.Region region, BlockPos centre, int cell, int size, int x, int z) {
        int column = cell % size;
        int row = cell / size;
        boolean edge = column == 0 || row == 0 || column == size - 1 || row == size - 1;
        return region.walled() && edge || Math.hypot(x - centre.getX(), z - centre.getZ()) < region.keepOut();
    }

    private static WaterwaysPlan.Kind classify(int cell, int size, int height, int seaLevel, boolean river) {
        if (height < seaLevel) return WaterwaysPlan.Kind.WATER;
        if (river) return WaterwaysPlan.Kind.OUTLET;
        int column = cell % size;
        int row = cell / size;
        boolean edge = column == 0 || row == 0 || column == size - 1 || row == size - 1;
        return edge ? WaterwaysPlan.Kind.EDGE : WaterwaysPlan.Kind.LAND;
    }

    private static int jitter(long seed, int column, int row, int axis, int reach) {
        long hash = seed ^ column * 0x9E3779B97F4A7C15L ^ row * 0xC2B2AE3D27D4EB4FL ^ axis * 0x165667B19E3779F9L;
        hash = (hash ^ hash >>> 30) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ hash >>> 27) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return Math.floorMod(hash, 2 * reach + 1) - reach;
    }
}
