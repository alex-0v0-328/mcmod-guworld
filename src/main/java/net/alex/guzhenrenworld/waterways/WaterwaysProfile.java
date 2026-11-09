package net.alex.guzhenrenworld.waterways;

import java.util.Comparator;
import java.util.stream.IntStream;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The water surface of a region's waterways [水系]: the level at each of {@link Waterways#KNOTS} + 1 evenly spaced
 * knots along every stream segment of a {@link WaterwaysPlan}, linear between them, never rising downstream and at
 * least {@link Waterways#INCISION} blocks under the ground read at each knot, on the channel and beside it, so the
 * banks hold the water.
 *
 * <p>At each knot the ground is read at three points: on the centerline, moved back by the meander to where the
 * carving puts the channel, and one block past each channel edge. Ground under the sea level reads as the sea level,
 * the sea's surface standing in for its floor, so a stream meets the sea at the sea level instead of cutting a trench
 * to the seabed. A reading more than {@link Waterways#MAX_DIP} blocks under the planned ground there, a canyon or
 * cliff the plan's grid missed, is skipped: the water may spill over it, instead of the whole stream below cutting a
 * gorge down to its floor.
 *
 * <p>Cells go upstream first, in falling filled height. A cell's arrival level is the lowest of its ground less the
 * incision and the ends of the segments flowing into it; its own segment carries that level down the knots, each
 * knot at the lower of the knot before and its reading less the incision. Every segment's last knot then becomes
 * its receiver's first knot, or the receiver's final arrival level where the receiver carries no stream, so the
 * segments meeting at a cell meet at one level. Water follows falling ground knot by knot and holds its level where
 * the ground rises, so it cuts through a rise: nothing follows the plan's filled height, and a stream through a
 * shallow lake on the plan cuts through the lake's rim. A segment into the sea arrives the incision under the sea
 * level, one into an outlet or a sink that far under its ground.
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
