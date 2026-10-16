package net.alex.guzhenrenworld.dimension;

import net.minecraft.core.Holder;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * Shape helpers for the 2D height functions of the heightfield regions, built from vanilla density functions only
 * so that {@code runData} can encode them.
 *
 * <p>{@link #getSpline} takes flat {@code location, value, derivative} triples. {@link #getRamp} falls from 1 at
 * its start to 0 at its end almost linearly, flat at both ends, so a valley cut through it keeps an even slope
 * instead of the steep middle of a smoothstep. {@link #getSoftMin} is the lower of two heights with the crease
 * between them rounded over {@code rounding} blocks, so a valley cut into the ground meets it in a curve, not a
 * sharp rim: the second height plus a spline of the gap, whose two quadratic pieces a cubic segment reproduces
 * exactly, so the first height is read once. A point query caches nothing, so every extra read of a function
 * recomputes its whole chain; pass the costly height first. {@link #getCoordinate} wraps an unregistered function
 * only: a registered one goes in as its holder, since a coordinate around a bare holder reference cannot be encoded.
 *
 * @author Alex
 * @version 1.0.0
 * @see SouthernBorderTerrain
 * @since 1.0.0
 */

final class DensityShapes {

    private DensityShapes() {}

    static DensityFunction getSpline(DensityFunctions.Spline.Coordinate coordinate, float... points) {
        CubicSpline.Builder<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> builder =
                CubicSpline.builder(coordinate);
        for (int index = 0; index < points.length; index += 3) {
            builder.addPoint(points[index], points[index + 1], points[index + 2]);
        }
        return DensityFunctions.spline(builder.build());
    }

    static DensityFunction getRamp(DensityFunctions.Spline.Coordinate coordinate, float start, float end) {
        float span = end - start;
        float slope = -1.1F / span;
        return getSpline(coordinate, start, 1.0F, 0.0F, start + 0.15F * span, 0.93F, slope,
                end - 0.15F * span, 0.07F, slope, end, 0.0F, 0.0F);
    }

    static DensityFunction getSoftMin(DensityFunction first, DensityFunction second, float rounding) {
        DensityFunction gap = DensityFunctions.add(first, getScaled(-1.0, second));
        return DensityFunctions.add(second, getSpline(getCoordinate(gap),
                -rounding, -rounding, 1.0F, 0.0F, -0.25F * rounding, 0.5F, rounding, 0.0F, 0.0F));
    }

    static DensityFunction getBlend(double base, double scale, DensityFunction value) {
        return DensityFunctions.add(DensityFunctions.constant(base), getScaled(scale, value));
    }

    static DensityFunction getScaled(double scale, DensityFunction value) {
        return DensityFunctions.mul(DensityFunctions.constant(scale), value);
    }

    static DensityFunctions.Spline.Coordinate getCoordinate(DensityFunction function) {
        return new DensityFunctions.Spline.Coordinate(Holder.direct(function));
    }

    static DensityFunctions.Spline.Coordinate getCoordinate(Holder<DensityFunction> registered) {
        return new DensityFunctions.Spline.Coordinate(registered);
    }
}
