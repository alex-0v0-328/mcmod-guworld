package net.alex.guzhenrenworld.waterways;

import java.util.stream.IntStream;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * 区域 {@link WaterwaysPlan} 的建造器：网格上两步，都只由世界种子决定。
 *
 * <ul>
 *   <li>取样：格中心按种子与格的散列沿各轴偏移至多 {@link Waterways#JITTER} 格；地面由 {@link GroundHeights} 并行读，
 *       类型由高度、海平面与群系定（ID 以 {@code river} 结尾为出口）；网格 512 × 512，约 45 CPU 秒。</li>
 *   <li>填洼、流向、汇流：{@link WaterwaysFlood}，在取样数组上跑，远不到 1 秒。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
