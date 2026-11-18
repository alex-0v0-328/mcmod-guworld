package net.alex.guzhenrenworld.waterways;

import java.util.stream.IntStream;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * 域水文网格规划（{@link WaterwaysPlan}）的构建器，完全由世界种子与地形决定。
 * <ul>
 *   <li>第一阶段为并行网格采样（{@link #sample}）：将每个网格中心按世界种子与行列散列在 X/Z 轴随机抖动至多
 *       {@link Waterways#JITTER} 格，通过 {@link GroundHeights} 并行扫描地面高度，并结合海平面与噪声群系（路径以
 *       {@code river}结尾视为河口）划分网格类型；512×512 网格约需 45 CPU 秒。</li>
 *   <li>第二阶段为纯内存水文拓扑求解：调用 {@link WaterwaysFlood} 在采样数组上依次完成洼地填平、最陡流向判定与汇流面积
 *       累加，耗时不足 1 秒。</li>
 * </ul>
 */

public final class WaterwaysPlanner {

    private WaterwaysPlanner() {}

    public static WaterwaysPlan plan(Waterways.Region region, NoiseBasedChunkGenerator generator, RandomState random,
            long seed) {
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        int seaLevel = settings.seaLevel();
        int size = region.cells();
        int count = size * size;
        int[] x = new int[count];
        int[] z = new int[count];
        for (int cell = 0; cell < count; cell++) {
            int column = cell % size;
            int row = cell / size;
            x[cell] = region.origin() + column * Waterways.CELL + Waterways.CELL / 2 + jitter(seed, column, row, 0);
            z[cell] = region.origin() + row * Waterways.CELL + Waterways.CELL / 2 + jitter(seed, column, row, 1);
        }
        float[] ground = new float[count];
        WaterwaysPlan.Kind[] kind = new WaterwaysPlan.Kind[count];
        sample(generator, random, size, x, z, ground, kind);
        double[] filled = new double[count];
        int[] order = new int[count];
        WaterwaysFlood.fill(kind, ground, filled, order, size);
        int[] receiver = WaterwaysFlood.getReceivers(kind, filled, x, z, size);
        int[] catchment = WaterwaysFlood.getCatchments(receiver, order);
        return new WaterwaysPlan(region, seaLevel, x, z, ground, filled, receiver, catchment, kind);
    }

    private static void sample(NoiseBasedChunkGenerator generator, RandomState random, int size, int[] x, int[] z,
            float[] ground, WaterwaysPlan.Kind[] kind) {
        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        int minY = settings.noiseSettings().minY();
        GroundHeights heights = new GroundHeights(random.router(), minY, minY + settings.noiseSettings().height());
        BiomeSource biomes = generator.getBiomeSource();
        Climate.Sampler sampler = random.sampler();
        IntStream.range(0, x.length).parallel().forEach(cell -> {
            int height = heights.sample(x[cell], z[cell]);
            ground[cell] = height;
            kind[cell] = classify(cell, size, height, settings.seaLevel(), biomes.getNoiseBiome(
                    QuartPos.fromBlock(x[cell]), QuartPos.fromBlock(height), QuartPos.fromBlock(z[cell]), sampler)
                    .is(key -> key.location().getPath().endsWith("river")));
        });
    }

    private static WaterwaysPlan.Kind classify(int cell, int size, int height, int seaLevel, boolean river) {
        if (height < seaLevel) return WaterwaysPlan.Kind.WATER;
        if (river) return WaterwaysPlan.Kind.OUTLET;
        int column = cell % size;
        int row = cell / size;
        boolean edge = column == 0 || row == 0 || column == size - 1 || row == size - 1;
        return edge ? WaterwaysPlan.Kind.EDGE : WaterwaysPlan.Kind.LAND;
    }

    private static int jitter(long seed, int column, int row, int axis) {
        long hash = seed ^ column * 0x9E3779B97F4A7C15L ^ row * 0xC2B2AE3D27D4EB4FL ^ axis * 0x165667B19E3779F9L;
        hash = (hash ^ hash >>> 30) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ hash >>> 27) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return Math.floorMod(hash, 2 * Waterways.JITTER + 1) - Waterways.JITTER;
    }
}
