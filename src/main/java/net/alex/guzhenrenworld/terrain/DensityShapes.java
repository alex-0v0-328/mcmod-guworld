package net.alex.guzhenrenworld.terrain;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The shared vocabulary of the heightfield regions' density functions: shapes and arithmetic built from vanilla
 * density functions only, so that {@code runData} can encode them, and the reads of noises and registered functions.
 *
 * <p>{@link #getSpline} takes flat {@code location, value, derivative} triples. {@link #getSmoothstep} is its
 * commonest case, two points with flat ends: 0 at its start and 1 at its end, or the two values it is given, eased
 * in between. {@link #getRamp} falls from 1 at its start to 0 at its end almost linearly, flat at both ends, so a
 * valley cut through it keeps an even slope instead of the steep middle of a smoothstep. {@link #getSoftMin} is the
 * lower of two heights with the crease between them rounded over {@code rounding} blocks, so a valley cut into the
 * ground meets it in a curve, not a sharp rim: the second height plus a spline of the gap, whose two quadratic pieces
 * a cubic segment reproduces exactly, so the first height is read once. A point query caches nothing, so every extra
 * read of a function recomputes its whole chain; pass the costly height first.
 *
 * <p>{@link #getChoice} is vanilla's range choice with no lower bound: the first function where the input lies below
 * the threshold, the second elsewhere. {@link #getNoise} reads a noise flat in y, the way every 2D field of a region
 * reads one; {@link #getFunction} refers to a registered function by its key. {@link #getCoordinate} wraps an
 * unregistered function only: a registered one goes in as its holder, since a coordinate around a bare holder
 * reference cannot be encoded.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class DensityShapes {

    private DensityShapes() {}

    public static DensityFunction getSpline(DensityFunctions.Spline.Coordinate coordinate, float... points) {
        CubicSpline.Builder<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> builder =
                CubicSpline.builder(coordinate);
        for (int index = 0; index < points.length; index += 3) {
            builder.addPoint(points[index], points[index + 1], points[index + 2]);
        }
        return DensityFunctions.spline(builder.build());
    }

    public static DensityFunction getSmoothstep(DensityFunctions.Spline.Coordinate coordinate, float start, float end) {
        return getSmoothstep(coordinate, start, 0.0F, end, 1.0F);
    }

    public static DensityFunction getSmoothstep(
            DensityFunctions.Spline.Coordinate coordinate, float start, float startValue, float end, float endValue) {
        return getSpline(coordinate, start, startValue, 0.0F, end, endValue, 0.0F);
    }

    public static DensityFunction getRamp(DensityFunctions.Spline.Coordinate coordinate, float start, float end) {
        float span = end - start;
        float slope = -1.1F / span;
        return getSpline(coordinate, start, 1.0F, 0.0F, start + 0.15F * span, 0.93F, slope,
                end - 0.15F * span, 0.07F, slope, end, 0.0F, 0.0F);
    }

    public static DensityFunction getSoftMin(DensityFunction first, DensityFunction second, float rounding) {
        return DensityFunctions.add(second, getSpline(getCoordinate(getDifference(first, second)),
                -rounding, -rounding, 1.0F, 0.0F, -0.25F * rounding, 0.5F, rounding, 0.0F, 0.0F));
    }

    public static DensityFunction getDifference(DensityFunction minuend, DensityFunction subtrahend) {
        return DensityFunctions.add(minuend, getScaled(-1.0, subtrahend));
    }

    public static DensityFunction getBlend(double base, double scale, DensityFunction value) {
        return DensityFunctions.add(DensityFunctions.constant(base), getScaled(scale, value));
    }

    public static DensityFunction getScaled(double scale, DensityFunction value) {
        return DensityFunctions.mul(DensityFunctions.constant(scale), value);
    }

    public static DensityFunction getChoice(DensityFunction input, double threshold, DensityFunction below,
            DensityFunction above) {
        return DensityFunctions.rangeChoice(input, -1000000.0, threshold, below, above);
    }

    public static DensityFunction getNoise(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key) {
        return getNoise(noises, key, 1.0);
    }

    public static DensityFunction getNoise(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key, double horizontalScale) {
        return DensityFunctions.noise(noises.getOrThrow(key), horizontalScale, 0.0);
    }

    public static DensityFunction getFunction(
            HolderGetter<DensityFunction> functions, ResourceKey<DensityFunction> key) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(key));
    }

    public static DensityFunctions.Spline.Coordinate getCoordinate(DensityFunction function) {
        return new DensityFunctions.Spline.Coordinate(Holder.direct(function));
    }

    public static DensityFunctions.Spline.Coordinate getCoordinate(Holder<DensityFunction> registered) {
        return new DensityFunctions.Spline.Coordinate(registered);
    }
}
