package net.alex.guzhenrenworld.dimension.southernborder;

import static net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder.functionKey;
import static net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder.noiseKey;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getBlend;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getCoordinate;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getDifference;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getFunction;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getNoise;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getRamp;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getScaled;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getSmoothstep;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getSoftMin;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getSpline;

import java.util.Arrays;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The rivers of the Southern Border [南疆]: where they run, the valleys they cut, the floodplains beside them and the
 * channels the sea level fills, every id under {@code guworld:southern_border/}.
 *
 * <p>Rivers follow the zero lines of noises, so they meander and never stop on a plain. Channels and valleys are
 * measured in blocks from that line ({@link #GREAT_DISTANCE}, {@link #STREAM_DISTANCE}): the noise over its own
 * slope, which comes from two samples {@link #SLOPE_STEP} blocks east and south, the sum of both differences times
 * {@link #SLOPE_NORM} standing in for its length, and a spline through {@link #SLOPES} for the division. A
 * threshold on the bare noise instead makes the width twice the threshold over the slope, so a steady slope draws a
 * canal and a flat stretch of noise swells the river into a round lake. Near a saddle of the noise, though, two
 * stretches of zero line pass close without meeting and the distance alone would break the river there, so a great
 * river also takes the bare threshold {@link #GREAT_BRIDGE}, which joins them through a small pool. Three things keep
 * the rivers natural:
 * <ul>
 *   <li>Every river noise is read {@link #WARP_BLOCKS} blocks off its true position by two 256-block warp noises
 *   (streams half that), so courses wander in bends a few hundred blocks long.</li>
 *   <li>The great river noise carries octaves down to 256 blocks, so its course bends at several scales.</li>
 *   <li>A 512-block width noise scales every half-width by 0.75..1.5 ({@link #getNarrowing}), so a river narrows into
 *   runs and opens into broad reaches, and its floodplain and valley with it; {@link #NARROWING} holds its
 *   reciprocal, which scales the distances.</li>
 * </ul>
 *
 * <p>Water stands only below the sea level, {@link #SEA_LEVEL}, the height the dimension's noise settings fill water
 * to, so a river holds water along its whole course only if its bed is set to a fixed height, never lowered by some
 * amount from the ground around, which rises and falls along the course and would break the river into a string of
 * pools. Vanilla, Tectonic and Terralith set their river floors to one height and end the rivers once, where a smooth
 * large-scale field, erosion, crosses a threshold. The streams here do the same, and the great rivers set floodplain
 * and bed to fixed heights inside valleys deep enough to hold them.
 * <ul>
 *   <li>Great rivers ({@link #GREAT_RIVER}): a four-kilometer noise, a few per eight kilometers. Within
 *   ±{@link #BASIN_WIDTH} of the line the mountains sink by up to {@link #BASIN_CUT}, within
 *   ±{@link #GREAT_VALLEY_WIDTH} by {@link #GREAT_VALLEY_CUT} more ({@link Network#getGreatValleys}), so every great
 *   river crosses the ranges down a broad valley. Within {@link #GREAT_FLOODPLAIN} blocks of the channel low ground
 *   is flattened to a floodplain {@link #FLOODPLAIN_Y} ± {@link #FLOODPLAIN_SWING} by a 128-block noise
 *   ({@link Network#getFloodplain}); the flattening holds below about y 72 and fades out by y 90, both lifted or
 *   lowered up to {@link #FLOODPLAIN_EDGE_SWING} blocks by the same noise, so the floodplain edge wanders instead of
 *   following one contour. The channel ({@link Network#getCarved}), ±{@link #GREAT_HALF_WIDTH} blocks before
 *   widening, 24..48 blocks wide, sinks to {@link #GREAT_RIVER_DEPTH} below sea level where the floodplain lies below
 *   y 66 and fades out by y 74.</li>
 *   <li>Streams ({@link #STREAM}): two two-kilometer noises, one on each side of the great rivers, blended across the
 *   great channel, so a stream that meets a great river joins it like a tributary instead of crossing it. {@link
 *   Network#getStreamCut} sinks the mountains by {@link #STREAM_VALLEY_CUT} within ±{@link #STREAM_VALLEY_WIDTH} of the
 *   line. The valley ({@link Network#getStreamValley}) is a profile over the distance: {@link #CHANNEL_PROFILE}, the
 *   bed {@link #STREAM_DEPTH} below sea level, the water line at ±{@link #STREAM_HALF_WIDTH} blocks before widening
 *   (5..10 blocks wide), a low bank and a floodplain a few blocks above the water, then the walls {@link #getWalled}
 *   adds: those of {@link #STREAM_PROFILE} steepen to one block per block, those of {@link #GORGE_PROFILE}, taken by
 *   the landform zone weight, rise tens of blocks per block, so a stream cuts a gorge through cliff country instead of
 *   planing its tops away; the weight is 0 outside, so foot and top share a profile there. The ground takes the lower
 *   of itself and the profile, rounded over {@link #VALLEY_ROUNDING} blocks.</li>
 *   <li>Headwaters: {@code Terrain}'s range field, its ridged relief without spurs, changes only over
 *   kilometers. Below {@link #REACH_FULL} the valley is whole; toward {@link #REACH_END} the profile rises by up to
 *   {@link #STREAM_LIFT} blocks, a share of that field alone, so the water ends once, where the bed passes sea level,
 *   and a dry valley climbs on until its floor passes the ground. A lift read off the ground would follow every hill
 *   and leave pools along the last stretch.</li>
 * </ul>
 * The terrain floods and carves the foot and the top of its cliffs alike, with the wetness of the foot, so a river
 * runs past a cliff as it runs over open ground.
 *
 * <p>{@link #CHANNEL} is the distance to the nearer channel in widened half-widths, 0 at the line and 1 at the bank;
 * {@link #SHORE} is that distance where the surface stands below sea level and at least {@link #DRY_SHORE} where it
 * does not. The router keeps cave entrances out of the riverbeds with it, and the biome table reads it, scaled by
 * {@code BiomeLayout.RIVER_WEIRDNESS}, as the weirdness, so the river biome covers exactly the wet
 * channels.
 *
 * <p>⚠ Every number here is a prototype for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see Terrain
 * @since 1.0.0
 */

final class Rivers {

    private static final ResourceKey<NormalNoise.NoiseParameters> GREAT_RIVER_NOISE = noiseKey("great_river");
    private static final ResourceKey<NormalNoise.NoiseParameters> STREAM_A_NOISE = noiseKey("stream_a");
    private static final ResourceKey<NormalNoise.NoiseParameters> STREAM_B_NOISE = noiseKey("stream_b");
    private static final ResourceKey<NormalNoise.NoiseParameters> WARP_X_NOISE = noiseKey("river_warp_x");
    private static final ResourceKey<NormalNoise.NoiseParameters> WARP_Z_NOISE = noiseKey("river_warp_z");
    private static final ResourceKey<NormalNoise.NoiseParameters> WIDTH_NOISE = noiseKey("river_width");
    private static final ResourceKey<NormalNoise.NoiseParameters> FLOODPLAIN_NOISE = noiseKey("floodplain");
    private static final ResourceKey<DensityFunction> GREAT_RIVER = functionKey("great_river");
    private static final ResourceKey<DensityFunction> STREAM = functionKey("stream");
    private static final ResourceKey<DensityFunction> GREAT_DISTANCE = functionKey("great_distance");
    private static final ResourceKey<DensityFunction> STREAM_DISTANCE = functionKey("stream_distance");
    private static final ResourceKey<DensityFunction> NARROWING = functionKey("narrowing");
    private static final ResourceKey<DensityFunction> CHANNEL = functionKey("channel");
    private static final ResourceKey<DensityFunction> SHORE = functionKey("shore");
    private static final float GREAT_RIVER_WIDTH = 0.01F;
    private static final float GREAT_VALLEY_WIDTH = 0.15F;
    private static final double GREAT_VALLEY_CUT = 0.5;
    private static final float BASIN_WIDTH = 0.4F;
    private static final double BASIN_CUT = 0.3;
    private static final float STREAM_WIDTH = 0.004F;
    private static final float STREAM_VALLEY_WIDTH = 0.08F;
    private static final double STREAM_VALLEY_CUT = 0.2;
    private static final float GREAT_HALF_WIDTH = 16.0F;
    private static final float GREAT_BRIDGE = 0.006F;
    private static final float STREAM_HALF_WIDTH = 3.5F;
    private static final float GREAT_FLOODPLAIN = 70.0F;
    static final int SEA_LEVEL = 63;
    static final double FLOODPLAIN_Y = 65.0;
    private static final double FLOODPLAIN_SWING = 2.0;
    private static final double FLOODPLAIN_EDGE_SWING = 6.0;
    private static final double WARP_BLOCKS = 24.0;
    private static final double SLOPE_STEP = 4.0;
    private static final double SLOPE_NORM = 0.8;
    private static final float[] SLOPES = { 2.0E-5F, 5.0E-5F, 1.0E-4F, 2.0E-4F, 5.0E-4F, 1.0E-3F, 2.0E-3F, 5.0E-3F,
            1.0E-2F };
    private static final float GREAT_RIVER_DEPTH = 9.0F;
    private static final float STREAM_DEPTH = 4.0F;
    private static final float[] CHANNEL_PROFILE = {
            0.0F, SEA_LEVEL - STREAM_DEPTH, 0.0F, 1.5F, 59.7F, 0.9F, 3.5F, 63.0F, 1.5F, 5.0F, 64.3F, 0.4F,
            14.0F, 66.0F, 0.2F };
    private static final float[] STREAM_PROFILE = getWalled(26.0F, 73.0F, 0.8F, 60.0F, 106.0F, 1.0F,
            300.0F, 346.0F, 1.0F);
    private static final float[] GORGE_PROFILE = getWalled(18.0F, 100.0F, 12.0F, 22.0F, 340.0F, 30.0F);
    private static final float VALLEY_ROUNDING = 6.0F;
    private static final double STREAM_LIFT = 300.0;
    private static final float REACH_FULL = 0.45F;
    private static final float REACH_END = 0.75F;
    private static final double DRY_SHORE = 8.0;

    private Rivers() {}

    record Network(DensityFunction great, Holder<DensityFunction> stream, Holder<DensityFunction> greatDistance,
            Holder<DensityFunction> streamDistance, Holder<DensityFunction> narrowing, Holder<DensityFunction> channel,
            DensityFunction floodplain) {

        DensityFunction getGreatValleys() {
            return DensityFunctions.add(getScaled(GREAT_VALLEY_CUT, getGreatValley()),
                    getScaled(BASIN_CUT, getRamp(getCoordinate(great), GREAT_RIVER_WIDTH, BASIN_WIDTH)));
        }

        DensityFunction getStreamCut() {
            return getScaled(STREAM_VALLEY_CUT, getRamp(getCoordinate(stream), STREAM_WIDTH, STREAM_VALLEY_WIDTH));
        }

        DensityFunction getGreatValley() {
            return getRamp(getCoordinate(great), GREAT_RIVER_WIDTH, GREAT_VALLEY_WIDTH);
        }

        DensityFunction getFloodplain(Holder<DensityFunction> ground) {
            DensityFunction plain = getSmoothstep(getCoordinate(getNarrowed(greatDistance, narrowing)),
                    GREAT_HALF_WIDTH, GREAT_FLOODPLAIN);
            DensityFunction shifted = DensityFunctions.add(new DensityFunctions.HolderHolder(ground),
                    getScaled(-FLOODPLAIN_EDGE_SWING / FLOODPLAIN_SWING, getBlend(-FLOODPLAIN_Y, 1.0, floodplain)));
            DensityFunction lowland = getSmoothstep(getCoordinate(shifted), 72.0F, 1.0F, 90.0F, 0.0F);
            DensityFunction keep = getBlend(1.0, -1.0, DensityFunctions.mul(getBlend(1.0, -1.0, plain), lowland));
            return DensityFunctions.lerp(keep, floodplain, new DensityFunctions.HolderHolder(ground));
        }

        DensityFunction getStreamValley(Holder<DensityFunction> land, Holder<DensityFunction> range,
                Holder<DensityFunction> zones) {
            DensityFunctions.Spline.Coordinate distance = getCoordinate(getNarrowed(streamDistance, narrowing));
            DensityFunction profile = DensityFunctions.lerp(new DensityFunctions.HolderHolder(zones),
                    getSpline(distance, STREAM_PROFILE), getSpline(distance, GORGE_PROFILE));
            DensityFunction valley = DensityFunctions.add(profile,
                    getScaled(STREAM_LIFT, getSmoothstep(getCoordinate(range), REACH_FULL, REACH_END)));
            return getSoftMin(new DensityFunctions.HolderHolder(land), valley, VALLEY_ROUNDING);
        }

        DensityFunction getWet(Holder<DensityFunction> land) {
            return getSmoothstep(getCoordinate(land),
                    SEA_LEVEL + 3.0F, 1.0F, SEA_LEVEL + 11.0F, 0.0F);
        }

        DensityFunction getCarved(Holder<DensityFunction> land, DensityFunction wet) {
            DensityFunction inside = getSmoothstep(
                    getCoordinate(getHalfWidths(greatDistance, narrowing, GREAT_HALF_WIDTH)), 0.0F, 1.0F, 1.0F, 0.0F);
            return DensityFunctions.lerp(DensityFunctions.mul(inside, wet), new DensityFunctions.HolderHolder(land),
                    DensityFunctions.constant(SEA_LEVEL - GREAT_RIVER_DEPTH));
        }
    }

    static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        context.register(GREAT_RIVER_NOISE, new NormalNoise.NoiseParameters(-12, 1.0, 1.5, 1.0, 0.6, 0.35));
        context.register(STREAM_A_NOISE, new NormalNoise.NoiseParameters(-11, 1.0, 0.7, 0.35));
        context.register(STREAM_B_NOISE, new NormalNoise.NoiseParameters(-11, 1.0, 0.7, 0.35));
        context.register(WARP_X_NOISE, new NormalNoise.NoiseParameters(-8, 1.0, 0.5));
        context.register(WARP_Z_NOISE, new NormalNoise.NoiseParameters(-8, 1.0, 0.5));
        context.register(WIDTH_NOISE, new NormalNoise.NoiseParameters(-9, 1.0));
        context.register(FLOODPLAIN_NOISE, new NormalNoise.NoiseParameters(-7, 1.0));
    }

    static Network register(BootstrapContext<DensityFunction> context) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
        Holder<DensityFunction> greatRiver = context.register(GREAT_RIVER,
                DensityFunctions.flatCache(getWarped(noises, GREAT_RIVER_NOISE, WARP_BLOCKS, 0.0, 0.0)));
        DensityFunction side = getSmoothstep(getCoordinate(greatRiver), -GREAT_RIVER_WIDTH, GREAT_RIVER_WIDTH);
        Holder<DensityFunction> stream = context.register(STREAM, DensityFunctions.flatCache(DensityFunctions.lerp(
                side, getWarped(noises, STREAM_B_NOISE, WARP_BLOCKS / 2.0, 0.0, 0.0).abs(),
                getWarped(noises, STREAM_A_NOISE, WARP_BLOCKS / 2.0, 0.0, 0.0).abs())));
        Holder<DensityFunction> greatDistance = context.register(GREAT_DISTANCE, DensityFunctions.flatCache(
                DensityFunctions.min(getDistance(noises, GREAT_RIVER_NOISE, WARP_BLOCKS), getScaled(
                        GREAT_HALF_WIDTH / GREAT_BRIDGE, new DensityFunctions.HolderHolder(greatRiver).abs()))));
        Holder<DensityFunction> streamDistance = context.register(STREAM_DISTANCE, DensityFunctions.flatCache(
                DensityFunctions.lerp(side, getDistance(noises, STREAM_B_NOISE, WARP_BLOCKS / 2.0),
                        getDistance(noises, STREAM_A_NOISE, WARP_BLOCKS / 2.0))));
        Holder<DensityFunction> narrowing =
                context.register(NARROWING, DensityFunctions.flatCache(getNarrowing(noises)));
        Holder<DensityFunction> channel = context.register(CHANNEL, DensityFunctions.flatCache(DensityFunctions.min(
                getHalfWidths(greatDistance, narrowing, GREAT_HALF_WIDTH),
                getHalfWidths(streamDistance, narrowing, STREAM_HALF_WIDTH))));
        DensityFunction floodplain = getBlend(FLOODPLAIN_Y, FLOODPLAIN_SWING, getNoise(noises, FLOODPLAIN_NOISE));
        return new Network(new DensityFunctions.HolderHolder(greatRiver).abs(), stream, greatDistance, streamDistance,
                narrowing, channel, floodplain);
    }

    static void registerShore(BootstrapContext<DensityFunction> context, Network rivers,
            Holder<DensityFunction> height) {
        DensityFunction dry = getSmoothstep(getCoordinate(height),
                SEA_LEVEL - 0.5F, SEA_LEVEL + 1.0F);
        context.register(SHORE, DensityFunctions.flatCache(DensityFunctions.add(
                new DensityFunctions.HolderHolder(rivers.channel()), getScaled(DRY_SHORE, dry))));
    }

    static DensityFunction getShore(HolderGetter<DensityFunction> functions) {
        return getFunction(functions, SHORE);
    }

    private static DensityFunction getNarrowed(Holder<DensityFunction> distance, Holder<DensityFunction> narrowing) {
        return DensityFunctions.mul(new DensityFunctions.HolderHolder(distance),
                new DensityFunctions.HolderHolder(narrowing));
    }

    private static DensityFunction getHalfWidths(Holder<DensityFunction> distance, Holder<DensityFunction> narrowing,
            float halfWidth) {
        return getScaled(1.0 / halfWidth, getNarrowed(distance, narrowing));
    }

    private static float[] getWalled(float... walls) {
        float[] points = Arrays.copyOf(CHANNEL_PROFILE, CHANNEL_PROFILE.length + walls.length);
        System.arraycopy(walls, 0, points, CHANNEL_PROFILE.length, walls.length);
        return points;
    }

    private static DensityFunction getDistance(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key, double warp) {
        DensityFunction here = getWarped(noises, key, warp, 0.0, 0.0);
        DensityFunction east = getDifference(here, getWarped(noises, key, warp, SLOPE_STEP, 0.0));
        DensityFunction south = getDifference(here, getWarped(noises, key, warp, 0.0, SLOPE_STEP));
        DensityFunction slope = getScaled(SLOPE_NORM / SLOPE_STEP, DensityFunctions.add(east.abs(), south.abs()));
        float[] points = new float[SLOPES.length * 3];
        for (int index = 0; index < SLOPES.length; index++) {
            boolean end = index == 0 || index == SLOPES.length - 1;
            points[index * 3] = SLOPES[index];
            points[index * 3 + 1] = 1.0F / SLOPES[index];
            points[index * 3 + 2] = end ? 0.0F : -1.0F / (SLOPES[index] * SLOPES[index]);
        }
        return DensityFunctions.mul(here.abs(), getSpline(getCoordinate(slope), points));
    }

    private static DensityFunction getWarped(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key, double blocks, double eastward, double southward) {
        return DensityFunctions.shiftedNoise2d(getBlend(eastward, blocks, getNoise(noises, WARP_X_NOISE)),
                getBlend(southward, blocks, getNoise(noises, WARP_Z_NOISE)), 1.0, noises.getOrThrow(key));
    }

    private static DensityFunction getNarrowing(HolderGetter<NormalNoise.NoiseParameters> noises) {
        return getSpline(getCoordinate(getNoise(noises, WIDTH_NOISE)),
                -0.5F, 1.0F / 0.75F, 0.0F, 0.0F, 1.0F, -0.65F, 0.5F, 1.0F / 1.5F, 0.0F);
    }
}
