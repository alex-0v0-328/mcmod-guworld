package net.alex.guzhenrenworld.waterways;

import java.util.Comparator;
import java.util.stream.IntStream;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * 区域水系的水面剖面：每段 {@link Waterways#KNOTS} + 1 个等距节点的水位。
 *
 * <ul>
 *   <li>节点间线性，往下游不升；每个节点比河道上及两侧读到的地面至少低 {@link Waterways#INCISION} 格，岸能兜住水。</li>
 *   <li>每节点读三处地面：按摆动反推的中线，及两侧边缘外 1 格；海平面下按海平面读，溪流在海平面入海、不追海床下切；
 *       低于规划地面 {@link Waterways#MAX_DIP} 格以上的读数（网格漏掉的峡谷、崖边）跳过：水或溢出，但不切深谷。</li>
 *   <li>按填后高度降序（上游先）：到达水位取地面减下切与各汇入段末端的最低者；本段逐节点取前一节点与本节点读数减下切的
 *       较小者。</li>
 *   <li>再把每段末节点改为流向格那段的首节点（流向格不成河时取其最终到达水位），汇于一格的各段同高。</li>
 *   <li>水随地面降、遇隆起保持水位而刻穿：不随规划填后高度，穿规划浅湖的溪流刻穿湖沿；入海段止于海平面减下切，
 *       入出口或汇点的止于其地面减下切。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WaterwaysProfile {

    private final float[][] levels;

    private WaterwaysProfile(float[][] levels) {
        this.levels = levels;
    }

    public static WaterwaysProfile of(WaterwaysPlan plan, GroundHeights ground, NormalNoise meander) {
        int count = plan.count();
        double[][] readings = new double[count][];
        IntStream.range(0, count).parallel().forEach(cell -> readings[cell] = WaterwaysIndex.isSegment(plan, cell)
                ? getReadings(plan, cell, ground, meander) : null);
        double[] arrival = new double[count];
        for (int cell = 0; cell < count; cell++) {
            arrival[cell] = (plan.kind(cell) == WaterwaysPlan.Kind.WATER ? plan.seaLevel() : plan.ground(cell))
                    - Waterways.INCISION;
        }
        int[] order = IntStream.range(0, count).filter(cell -> WaterwaysIndex.isSegment(plan, cell)).boxed()
                .sorted(Comparator.comparingDouble(plan::filled).reversed()).mapToInt(Integer::intValue).toArray();
        float[][] levels = new float[count][];
        for (int cell : order) {
            float[] level = new float[Waterways.KNOTS + 1];
            double current = arrival[cell];
            for (int knot = 0; knot <= Waterways.KNOTS; knot++) {
                current = Math.min(current, readings[cell][knot]);
                level[knot] = (float) current;
            }
            levels[cell] = level;
            arrival[plan.receiver(cell)] = Math.min(arrival[plan.receiver(cell)], current);
        }
        for (int cell : order) {
            int receiver = plan.receiver(cell);
            levels[cell][Waterways.KNOTS] = levels[receiver] != null ? levels[receiver][0] : (float) arrival[receiver];
        }
        return new WaterwaysProfile(levels);
    }

    public float level(int cell, int knot) {
        return levels[cell][knot];
    }

    private static double[] getReadings(WaterwaysPlan plan, int cell, GroundHeights ground, NormalNoise meander) {
        int receiver = plan.receiver(cell);
        double x0 = plan.x(cell);
        double z0 = plan.z(cell);
        double dx = plan.x(receiver) - x0;
        double dz = plan.z(receiver) - z0;
        double length = Math.hypot(dx, dz);
        double normalX = length == 0 ? 0 : -dz / length;
        double normalZ = length == 0 ? 0 : dx / length;
        double width0 = plan.width(cell, Waterways.MIN_CATCHMENT);
        double width1 = WaterwaysIndex.getEndWidth(plan, cell);
        int sea = plan.seaLevel();
        double[] readings = new double[Waterways.KNOTS + 1];
        for (int knot = 0; knot <= Waterways.KNOTS; knot++) {
            double along = knot / (double) Waterways.KNOTS;
            double lineX = x0 + along * dx;
            double lineZ = z0 + along * dz;
            double from = Math.max(plan.ground(cell), sea);
            double planned = from + along * (Math.max(plan.ground(receiver), sea) - from);
            double centerX = lineX - Waterways.getMeanderX(meander, lineX, lineZ);
            double centerZ = lineZ - Waterways.getMeanderZ(meander, lineX, lineZ);
            double reach = (width0 + along * (width1 - width0)) / 2 + 1;
            double lowest = Double.POSITIVE_INFINITY;
            for (int side = -1; side <= 1; side++) {
                int height = Math.max(sea, ground.sample((int) Math.floor(centerX + side * reach * normalX),
                        (int) Math.floor(centerZ + side * reach * normalZ), (int) Math.round(planned)));
                if (height >= planned - Waterways.MAX_DIP) lowest = Math.min(lowest, height);
            }
            readings[knot] = lowest - Waterways.INCISION;
        }
        return readings;
    }
}
