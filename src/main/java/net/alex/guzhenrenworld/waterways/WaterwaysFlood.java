package net.alex.guzhenrenworld.waterways;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * The drainage passes of a waterways [水系] plan [水系规划], on plain arrays over a square grid of cells (cell {@code
 * row * size + column}, eight neighbors), so they need no world:
 *
 * <ol>
 *   <li>{@link #fill}: Priority-Flood with an epsilon (Barnes, Lehman and Mulla 2014) from every water, outlet and
 *   edge cell, so each cell's filled height is its ground, or {@link #EPSILON} above the lowest way out of its
 *   depression. Any cell lowest among its eight neighbors yet filled more than {@link Waterways#LAKE_DEPTH} blocks
 *   becomes a sink, seeding the next flood, until a flood adds none or after {@link #MAX_SINK_PASSES} reruns: what
 *   stays filled is a lake at most that deep, and a stream into a deeper basin ends at its floor instead of a lake or
 *   a gorge tens of blocks deep (without sinks over a quarter of the stream cells crossed one, up to 51 blocks
 *   deep). It also records the order the flood popped the cells in.</li>
 *   <li>{@link #getReceivers}: each land cell drains into the neighbor of the eight with the steepest fall of filled
 *   height per block between the jittered points; its flood parent always falls, so every land cell has one.</li>
 *   <li>{@link #getCatchments}: one per cell, summed down the receivers in the reverse of the flood's pop order, which
 *   never rises in filled height, so a cell is done before its receiver.</li>
 * </ol>
 *
 * @author Alex
 * @version 1.0.0
 * @see WaterwaysPlanner
 * @since 1.0.0
 */

final class WaterwaysFlood {

    static final double EPSILON = 1e-4;
    private static final int MAX_SINK_PASSES = 8;
    private static final int[] DX = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] DZ = {0, 1, 1, 1, 0, -1, -1, -1};

    private WaterwaysFlood() {}

    static void fill(WaterwaysPlan.Kind[] kind, float[] ground, double[] filled, int[] order, int size) {
        for (int pass = 0; ; pass++) {
            flood(kind, ground, filled, order, size);
            if (pass == MAX_SINK_PASSES || !addSinks(kind, ground, filled, size)) break;
        }
    }

    static int[] getCatchments(int[] receiver, int[] order) {
        int[] catchment = new int[receiver.length];
        Arrays.fill(catchment, 1);
        for (int index = order.length - 1; index >= 0; index--) {
            int cell = order[index];
            if (receiver[cell] >= 0) catchment[receiver[cell]] += catchment[cell];
        }
        return catchment;
    }

    static int[] getReceivers(WaterwaysPlan.Kind[] kind, double[] filled, int[] x, int[] z, int size) {
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

    private static int neighbor(int cell, int direction, int size) {
        int column = cell % size + DX[direction];
        int row = cell / size + DZ[direction];
        return column < 0 || row < 0 || column >= size || row >= size ? -1 : row * size + column;
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
