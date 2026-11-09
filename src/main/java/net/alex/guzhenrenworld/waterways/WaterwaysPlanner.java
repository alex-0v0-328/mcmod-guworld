package net.alex.guzhenrenworld.waterways;

import java.util.stream.IntStream;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Builds a region's {@link WaterwaysPlan} [水系规划] in two steps over its grid, both deterministic in the world seed.
 *
 * <ol>
 *   <li>Samples: each cell's point is its center moved by up to {@link Waterways#JITTER} blocks along each axis, a
 *   hash of the seed and the cell; its ground comes from {@link GroundHeights}, in parallel, and its kind from that
 *   height, the sea level and the biome there (any biome whose id ends in {@code river} is an outlet).</li>
 *   <li>Flood, flow and catchment: {@link WaterwaysFlood}, on the arrays the samples filled.</li>
 * </ol>
 *
 * <p>A region's grid is 512 cells square; sampling it costs about 45 CPU seconds, the other passes well under one.
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
