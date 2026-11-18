package net.alex.guzhenrenworld.waterways;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * 水系规划中的洼地填平、流向判定与汇流面积累加算法，纯内存网格数组运算而不访问世界。
 * <ul>
 *   <li>{@link #fill} 执行带微小梯度增量 {@link #EPSILON} 的 Priority-Flood 算法（Barnes 等，2014）：以水体、河口与边界
 *       格为初始种子，在八邻域网格（下标 {@code row * size + column}）上优先扩展最低格，将封闭洼地抬升至最低溢流口高度
 *       并按出队顺序记录拓扑序。</li>
 *   <li>每轮填洼后扫描八邻域局部最低点：若填平深度超过浅湖上限 {@link Waterways#LAKE_DEPTH}，则将该谷底格标记为内流汇点
 *       （{@link WaterwaysPlan.Kind#SINK}）并作为新种子重跑填洼（至多迭代 {@link #MAX_SINK_PASSES} 轮），使流入深谷的溪
 *       流自然终止于谷底，避免填出数十格深的人工湖或切穿高耸山脊。</li>
 *   <li>{@link #getReceivers} 为每个陆地格在八邻域中选择填洼后单位距离落差（按抖动后采样坐标计距）最大的邻居作为下游流
 *       向格；由于 Priority-Flood 保证父节点高度严格更低，所有陆地格必然存在有效流向。</li>
 *   <li>{@link #getCatchments} 按填洼出队逆序（高度由高到低）沿流向累加汇流格数，保证每个上游格必然先于其下游接收格完成
 *       统计。</li>
 * </ul>
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
