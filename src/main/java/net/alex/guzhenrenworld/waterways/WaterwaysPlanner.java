package net.alex.guzhenrenworld.waterways;

import java.util.Arrays;
import java.util.stream.IntStream;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Builds a region's {@link WaterwaysPlan} [水系规划] in four passes over its grid, all deterministic in the world seed.
 *
 * <ol>
 *   <li>Samples: each cell's point is its center moved by up to {@link Waterways#JITTER} blocks along each axis, a
 *   hash of the seed and the cell; its ground comes from {@link GroundHeights}, in parallel, and its kind from that
 *   height, the sea level and the biome there (any biome whose id ends in {@code river} is an outlet).</li>
 *   <li>Flood: Priority-Flood with an epsilon (Barnes, Lehman and Mulla 2014) from every water, outlet and edge cell,
 *   so each cell's filled height is its ground, or {@link #EPSILON} above the lowest way out of its depression. Any
 *   cell lowest among its eight neighbors yet filled more than {@link Waterways#LAKE_DEPTH} blocks becomes a sink,
 *   seeding the next flood, until a flood adds none or after {@link #MAX_SINK_PASSES} reruns: what stays filled is
 *   a lake at most that deep, and a stream into a deeper basin ends at its floor instead of a lake or a gorge tens of
 *   blocks deep (without sinks over a quarter of the stream cells crossed one, up to 51 blocks deep).</li>
 *   <li>Flow: each land cell drains into the neighbor of the eight with the steepest fall of filled height per block
 *   between the jittered points; its flood parent always falls, so every land cell has one.</li>
 *   <li>Catchment: one per cell, summed down the receivers in the reverse of the flood's pop order, which never
 *   rises in filled height, so a cell is done before its receiver.</li>
 * </ol>
 *
 * <p>A region's grid is 512 cells square; sampling it costs about 45 CPU seconds, the other passes well under one.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WaterwaysPlanner {

    private static final double EPSILON = 1e-4;
    private static final int MAX_SINK_PASSES = 8;
    private static final int[] DX = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] DZ = {0, 1, 1, 1, 0, -1, -1, -1};

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
        for (int pass = 0; ; pass++) {
            flood(kind, ground, filled, order, size);
            if (pass == MAX_SINK_PASSES || !addSinks(kind, ground, filled, size)) break;
        }
        int[] receiver = getReceivers(kind, filled, x, z, size);
        int[] catchment = new int[count];
        Arrays.fill(catchment, 1);
        for (int index = count - 1; index >= 0; index--) {
            int cell = order[index];
            if (receiver[cell] >= 0) catchment[receiver[cell]] += catchment[cell];
        }
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

    private static int[] getReceivers(WaterwaysPlan.Kind[] kind, double[] filled, int[] x, int[] z, int size) {
        int[] receiver = new int[kind.length];
        Arrays.fill(receiver, -1);
        IntStream.range(0, kind.length).parallel().forEach(cell -> {
            if (kind[cell] != WaterwaysPlan.Kind.LAND) return;
            double steepest = 0;
            for (int direction = 0; direction < 8; direction++) {
                int next = neighbor(cell, direction, size);
                if (next < 0 || filled[next] >= filled[cell]) continue;
                double fall = (filled[cell] - filled[next]) / Math.hypot(x[next] - x[cell], z[next] - z[cell]);
                if (fall <= steepest) continue;
                steepest = fall;
                receiver[cell] = next;
            }
        });
        return receiver;
    }

    private static void flood(WaterwaysPlan.Kind[] kind, float[] ground, double[] filled, int[] order, int size) {
        int count = kind.length;
        boolean[] closed = new boolean[count];
        int popped = 0;
        Heap open = new Heap(count);
        for (int cell = 0; cell < count; cell++) {
            if (kind[cell] == WaterwaysPlan.Kind.LAND) continue;
            filled[cell] = ground[cell];
            closed[cell] = true;
            open.push(filled[cell], cell);
        }
        while (!open.isEmpty()) {
            int cell = open.pop();
            order[popped++] = cell;
            for (int direction = 0; direction < 8; direction++) {
                int next = neighbor(cell, direction, size);
                if (next < 0 || closed[next]) continue;
                filled[next] = Math.max(ground[next], filled[cell] + EPSILON);
                closed[next] = true;
                open.push(filled[next], next);
            }
        }
    }

    private static boolean addSinks(WaterwaysPlan.Kind[] kind, float[] ground, double[] filled, int size) {
        boolean added = false;
        for (int cell = 0; cell < kind.length; cell++) {
            if (kind[cell] != WaterwaysPlan.Kind.LAND || filled[cell] - ground[cell] <= Waterways.LAKE_DEPTH) continue;
            boolean lowest = true;
            for (int direction = 0; direction < 8 && lowest; direction++) {
                int next = neighbor(cell, direction, size);
                lowest = next < 0 || ground[next] >= ground[cell];
            }
            if (!lowest) continue;
            kind[cell] = WaterwaysPlan.Kind.SINK;
            added = true;
        }
        return added;
    }

    private static WaterwaysPlan.Kind classify(int cell, int size, int height, int seaLevel, boolean river) {
        if (height < seaLevel) return WaterwaysPlan.Kind.WATER;
        if (river) return WaterwaysPlan.Kind.OUTLET;
        int column = cell % size;
        int row = cell / size;
        boolean edge = column == 0 || row == 0 || column == size - 1 || row == size - 1;
        return edge ? WaterwaysPlan.Kind.EDGE : WaterwaysPlan.Kind.LAND;
    }

    private static int neighbor(int cell, int direction, int size) {
        int column = cell % size + DX[direction];
        int row = cell / size + DZ[direction];
        return column < 0 || row < 0 || column >= size || row >= size ? -1 : row * size + column;
    }

    private static int jitter(long seed, int column, int row, int axis) {
        long hash = seed ^ column * 0x9E3779B97F4A7C15L ^ row * 0xC2B2AE3D27D4EB4FL ^ axis * 0x165667B19E3779F9L;
        hash = (hash ^ hash >>> 30) * 0xBF58476D1CE4E5B9L;
        hash = (hash ^ hash >>> 27) * 0x94D049BB133111EBL;
        hash ^= hash >>> 31;
        return Math.floorMod(hash, 2 * Waterways.JITTER + 1) - Waterways.JITTER;
    }

    private static final class Heap {

        private final double[] keys;
        private final int[] values;
        private int size;

        private Heap(int capacity) {
            keys = new double[capacity];
            values = new int[capacity];
        }

        private boolean isEmpty() {
            return size == 0;
        }

        private void push(double key, int value) {
            int index = size++;
            while (index > 0) {
                int parent = (index - 1) >>> 1;
                if (keys[parent] <= key) break;
                keys[index] = keys[parent];
                values[index] = values[parent];
                index = parent;
            }
            keys[index] = key;
            values[index] = value;
        }

        private int pop() {
            int top = values[0];
            double key = keys[--size];
            int value = values[size];
            int index = 0;
            while (true) {
                int child = 2 * index + 1;
                if (child >= size) break;
                if (child + 1 < size && keys[child + 1] < keys[child]) child++;
                if (keys[child] >= key) break;
                keys[index] = keys[child];
                values[index] = values[child];
                index = child;
            }
            keys[index] = key;
            values[index] = value;
            return top;
        }
    }
}
