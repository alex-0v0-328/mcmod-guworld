package net.alex.guzhenrenworld.waterways;

import java.util.Comparator;
import java.util.stream.IntStream;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * 水系河道线段的纵向水面高程剖面计算器，为每条溪流线段的 {@link Waterways#KNOTS} + 1 个等距节点求解水位。
 * <ul>
 *   <li>各节点间线性插值且沿流向单调不升；每个节点水位至少低于河道中线及两侧河岸地面 {@link Waterways#INCISION} 格，确
 *       保两侧河岸始终能兜住水体。</li>
 *   <li>每个节点并行采样三处地面（反推蜿蜒偏移后的实际河道中线，以及法线方向两侧河宽外扩 1 格处）：低于海平面的采样点按
 *       海平面计（使溪流在海平面平缓入海而不向海底下切）；比规划插值地面低 {@link Waterways#MAX_DIP} 格以上的异常低值（
 *       如粗网格漏采的窄裂谷或崖边）会被忽略，防止单处裂缝把整段下游水位拉入谷底。</li>
 *   <li>按填洼高度由高到低（自上游向下游）递推：每条线段的起始到达水位取本格地面减下切与所有上游汇入段末端水位的最小
 *       值，段内各节点依次取前一节点水位与当前节点采样上限的较小值，最后将段末节点强制对齐到下游接收段的起点水位，保证
 *       多支流交汇处水位严格齐平。</li>
 * </ul>
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
