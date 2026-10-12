package net.alex.guzhenrenworld.dimension;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The Southern Border [南疆] terrain: mountains above all, with plains between them and great rivers cutting
 * through them. Holds its noises, its surface height in blocks, and the climate fields its biome table reads,
 * every id under {@code guworld:southern_border/}.
 *
 * <p>The height is a sum in blocks, one 2D value per column ({@link #getLand}, then {@link #carveRivers}):
 * <ul>
 *   <li>Plain floor: {@link #BASIN_FLOOR_Y} ± {@link #BASIN_SWING}, from a noise with features about a
 *   kilometer across, so the flat land rises and falls in slopes kilometers long.</li>
 *   <li>Mountain share ({@link #MOUNTAINS}, 0..1): a spline over a noise with features about two kilometers
 *   across, flat at 0 for continent values up to -0.3 and flat at 1 from -0.02, so the ranges take about half
 *   the land and the plains a sixth, with smooth foothills between. Near a great river the share fades to 0,
 *   so the river runs down a wide valley between ranges.</li>
 *   <li>Rise: up to {@link #PEAK_RISE} blocks over the plain floor, times the mountain share. A ridge value
 *   {@code 1 - |noise|} peaks along a noise's zero lines and draws the ranges (about a kilometer); two finer
 *   crease values {@code min(1, 2|noise|)} dip along theirs and cut gullies into the flanks. A spline turns the
 *   product into blocks: flat below 0.3 (the valley floors between ridges), steepest in the middle, and still
 *   rising to a point at the top, so only the shelves below are flat.</li>
 *   <li>Benches ({@link #getRise}): a terrace spline flattens the rise around +74..77 and +192..195 blocks and
 *   steepens it just above, so mountainsides carry shelves some tens of blocks wide and low summits end in
 *   flat tops, the ground the clan villages [山寨] will stand on. A kilometer-scale noise shifts both
 *   shelf heights by up to ±{@link #BENCH_SWING} blocks, so they never line up like contour rings.</li>
 *   <li>Detail: ±(0.5 + 4 × mountain share) blocks of 64-block bumps, so plains stay almost flat.</li>
 *   <li>Rivers ({@link #RIVER}): along the zero lines of a kilometer-scale noise the height drops to
 *   {@link #RIVER_BED_Y}, sea level fills the channel, and the banks rise back over |noise| 0.008..0.06. A
 *   mountain share of 0.4 and up fades them out, so they run on the plains and stop at the foot of the
 *   ranges instead of scoring ditches through the hills.</li>
 *   <li>Great rivers ({@link #GREAT_RIVER}): along the zero lines of a four-kilometer-scale noise, a few per
 *   eight kilometers, the height drops to {@link #GREAT_RIVER_BED_Y} over |noise| 0.002..0.006, a channel
 *   some 30..60 blocks wide, after the mountain share has already faded over |noise| 0.01..0.08, a valley
 *   floor about 80 blocks wide with the ranges back at full height some 300 blocks out on each side.</li>
 * </ul>
 *
 * <p>{@link #ELEVATION} is the height mapped to -1 at y 50 and 1 at y 400, clamped; it is the erosion parameter
 * of the biome table ({@code SouthernBorderBiomes}), so biome bands follow altitude instead of a separate
 * noise and a peak biome only ever lands on a peak. The mountain share stands in as continentalness; the
 * weirdness is the distance to the nearer river centerline, the great river's scaled by
 * {@link #GREAT_RIVER_WEIRDNESS_SCALE} so both channels fall in the river biome's band. The height, the
 * mountain share, both rivers and the elevation are registered density functions: the router reads them in
 * several places, and only a shared holder shares their caches. A spline whose coordinate is a registered
 * function takes its holder as is ({@link #carveRivers}): a coordinate wrapping a bare holder reference cannot be
 * encoded, and {@code runData} fails.
 *
 * <p>⚠ Every number here is a prototype for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see HeightfieldRouter
 * @since 1.0.0
 */

public final class SouthernBorderTerrain {

    private static final ResourceKey<NormalNoise.NoiseParameters> CONTINENT_NOISE = noiseKey("continent");
    private static final ResourceKey<NormalNoise.NoiseParameters> BASIN_NOISE = noiseKey("basin");
    private static final ResourceKey<NormalNoise.NoiseParameters> RIDGE_A_NOISE = noiseKey("ridge_a");
    private static final ResourceKey<NormalNoise.NoiseParameters> RIDGE_B_NOISE = noiseKey("ridge_b");
    private static final ResourceKey<NormalNoise.NoiseParameters> RIDGE_C_NOISE = noiseKey("ridge_c");
    private static final ResourceKey<NormalNoise.NoiseParameters> BENCH_NOISE = noiseKey("bench");
    private static final ResourceKey<NormalNoise.NoiseParameters> DETAIL_NOISE = noiseKey("detail");
    private static final ResourceKey<NormalNoise.NoiseParameters> RIVER_NOISE = noiseKey("river");
    private static final ResourceKey<NormalNoise.NoiseParameters> GREAT_RIVER_NOISE = noiseKey("great_river");
    private static final ResourceKey<NormalNoise.NoiseParameters> TEMPERATURE_NOISE = noiseKey("temperature");
    private static final ResourceKey<NormalNoise.NoiseParameters> VEGETATION_NOISE = noiseKey("vegetation");
    private static final ResourceKey<DensityFunction> RIVER = functionKey("river");
    private static final ResourceKey<DensityFunction> GREAT_RIVER = functionKey("great_river");
    private static final ResourceKey<DensityFunction> MOUNTAINS = functionKey("mountains");
    private static final ResourceKey<DensityFunction> HEIGHT = functionKey("height");
    private static final ResourceKey<DensityFunction> ELEVATION = functionKey("elevation");
    private static final double BASIN_FLOOR_Y = 76.0;
    private static final double BASIN_SWING = 14.0;
    private static final float PEAK_RISE = 320.0F;
    private static final double BENCH_SWING = 24.0;
    private static final double RIVER_BED_Y = 56.0;
    private static final double GREAT_RIVER_BED_Y = 48.0;
    private static final double GREAT_RIVER_WEIRDNESS_SCALE = 2.0;

    private SouthernBorderTerrain() {}

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        context.register(CONTINENT_NOISE, new NormalNoise.NoiseParameters(-11, 1.0, 1.0, 0.5));
        context.register(BASIN_NOISE, new NormalNoise.NoiseParameters(-10, 1.0, 0.5));
        context.register(RIDGE_A_NOISE, new NormalNoise.NoiseParameters(-10, 1.0));
        context.register(RIDGE_B_NOISE, new NormalNoise.NoiseParameters(-8, 1.0));
        context.register(RIDGE_C_NOISE, new NormalNoise.NoiseParameters(-7, 1.0));
        context.register(BENCH_NOISE, new NormalNoise.NoiseParameters(-10, 1.0));
        context.register(DETAIL_NOISE, new NormalNoise.NoiseParameters(-6, 1.0));
        context.register(RIVER_NOISE, new NormalNoise.NoiseParameters(-10, 1.0));
        context.register(GREAT_RIVER_NOISE, new NormalNoise.NoiseParameters(-12, 1.0));
        context.register(TEMPERATURE_NOISE, new NormalNoise.NoiseParameters(-11, 1.0, 0.5));
        context.register(VEGETATION_NOISE, new NormalNoise.NoiseParameters(-10, 1.0, 0.5));
    }

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
        Holder<DensityFunction> river =
                context.register(RIVER, DensityFunctions.flatCache(getNoise(noises, RIVER_NOISE)));
        Holder<DensityFunction> greatRiver =
                context.register(GREAT_RIVER, DensityFunctions.flatCache(getNoise(noises, GREAT_RIVER_NOISE)));
        DensityFunction share = getSpline(getCoordinate(getNoise(noises, CONTINENT_NOISE)),
                -0.3F, 0.0F, 0.0F, -0.02F, 1.0F, 0.0F);
        DensityFunction valley = getSpline(getCoordinate(new DensityFunctions.HolderHolder(greatRiver).abs()),
                0.01F, 0.0F, 0.0F, 0.08F, 1.0F, 0.0F);
        Holder<DensityFunction> mountains =
                context.register(MOUNTAINS, DensityFunctions.flatCache(DensityFunctions.mul(share, valley)));
        DensityFunction height = carveRivers(getLand(noises, new DensityFunctions.HolderHolder(mountains)),
                mountains, new DensityFunctions.HolderHolder(river), new DensityFunctions.HolderHolder(greatRiver));
        Holder<DensityFunction> heightHolder =
                context.register(HEIGHT, DensityFunctions.flatCache(DensityFunctions.cache2d(height)));
        DensityFunction elevation = DensityFunctions.add(DensityFunctions.mul(
                new DensityFunctions.HolderHolder(heightHolder), DensityFunctions.constant(1.0 / 175.0)),
                DensityFunctions.constant(-225.0 / 175.0));
        context.register(ELEVATION, DensityFunctions.flatCache(elevation.clamp(-1.0, 1.0)));
    }

    public static NoiseRouter createRouter(HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction riverDistance = DensityFunctions.min(getFunction(functions, RIVER).abs(),
                DensityFunctions.mul(DensityFunctions.constant(GREAT_RIVER_WEIRDNESS_SCALE),
                        getFunction(functions, GREAT_RIVER).abs()));
        return HeightfieldRouter.create(functions, noises, new HeightfieldRouter.Region(
                getFunction(functions, HEIGHT),
                DensityFunctions.flatCache(getNoise(noises, TEMPERATURE_NOISE)),
                DensityFunctions.flatCache(getNoise(noises, VEGETATION_NOISE)),
                getFunction(functions, MOUNTAINS),
                getFunction(functions, ELEVATION),
                riverDistance));
    }

    private static DensityFunction getLand(HolderGetter<NormalNoise.NoiseParameters> noises,
            DensityFunction mountains) {
        DensityFunction floor = DensityFunctions.add(DensityFunctions.constant(BASIN_FLOOR_Y),
                DensityFunctions.mul(DensityFunctions.constant(BASIN_SWING), getNoise(noises, BASIN_NOISE)));
        DensityFunction detail = DensityFunctions.mul(getNoise(noises, DETAIL_NOISE), getBlend(0.5, 4.0, mountains));
        return DensityFunctions.add(DensityFunctions.add(floor, getRise(noises, mountains)), detail);
    }

    private static DensityFunction getRise(HolderGetter<NormalNoise.NoiseParameters> noises,
            DensityFunction mountains) {
        DensityFunction ridged = DensityFunctions.mul(getRidge(noises, RIDGE_A_NOISE), DensityFunctions.mul(
                getBlend(0.6, 0.4, getCrease(noises, RIDGE_B_NOISE)),
                getBlend(0.85, 0.15, getCrease(noises, RIDGE_C_NOISE))));
        DensityFunction peaks = getSpline(getCoordinate(ridged),
                0.3F, 0.0F, 0.0F,
                0.5F, PEAK_RISE / 3.0F, 700.0F,
                0.72F, PEAK_RISE * 0.82F, 600.0F,
                0.9F, PEAK_RISE * 0.96F, 250.0F,
                1.0F, PEAK_RISE, 50.0F);
        DensityFunction shift = DensityFunctions.mul(DensityFunctions.constant(BENCH_SWING),
                getNoise(noises, BENCH_NOISE));
        DensityFunction terraced = getSpline(getCoordinate(DensityFunctions.add(
                        DensityFunctions.mul(mountains, peaks), shift)),
                -40.0F, -40.0F, 1.0F,
                60.0F, 60.0F, 1.0F,
                85.0F, 74.0F, 0.1F,
                100.0F, 77.0F, 0.1F,
                130.0F, 130.0F, 1.6F,
                180.0F, 180.0F, 1.0F,
                205.0F, 192.0F, 0.1F,
                220.0F, 195.0F, 0.1F,
                250.0F, 250.0F, 1.6F,
                420.0F, 420.0F, 1.0F);
        return DensityFunctions.add(terraced, DensityFunctions.mul(DensityFunctions.constant(-1.0), shift));
    }

    private static DensityFunction carveRivers(DensityFunction land, Holder<DensityFunction> mountains,
            DensityFunction river, DensityFunction greatRiver) {
        DensityFunction banks = getSpline(getCoordinate(river.abs()), 0.008F, 0.0F, 0.0F, 0.06F, 1.0F, 0.0F);
        DensityFunction headwaters = getSpline(new DensityFunctions.Spline.Coordinate(mountains),
                0.0F, 0.0F, 0.0F, 0.4F, 1.0F, 0.0F);
        DensityFunction withRivers = DensityFunctions.lerp(DensityFunctions.max(banks, headwaters),
                DensityFunctions.constant(RIVER_BED_Y), land);
        DensityFunction greatBanks = getSpline(getCoordinate(greatRiver.abs()),
                0.002F, 0.0F, 0.0F, 0.006F, 1.0F, 0.0F);
        return DensityFunctions.lerp(greatBanks, DensityFunctions.constant(GREAT_RIVER_BED_Y), withRivers);
    }

    private static DensityFunction getRidge(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key) {
        return DensityFunctions.add(DensityFunctions.constant(1.0),
                DensityFunctions.mul(DensityFunctions.constant(-1.0), getNoise(noises, key).abs())).clamp(0.0, 1.0);
    }

    private static DensityFunction getCrease(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key) {
        return DensityFunctions.mul(DensityFunctions.constant(2.0), getNoise(noises, key).abs()).clamp(0.0, 1.0);
    }

    private static DensityFunction getBlend(double base, double scale, DensityFunction value) {
        return DensityFunctions.add(DensityFunctions.constant(base),
                DensityFunctions.mul(DensityFunctions.constant(scale), value));
    }

    private static DensityFunction getSpline(DensityFunctions.Spline.Coordinate coordinate, float... points) {
        CubicSpline.Builder<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> builder =
                CubicSpline.builder(coordinate);
        for (int index = 0; index < points.length; index += 3) {
            builder.addPoint(points[index], points[index + 1], points[index + 2]);
        }
        return DensityFunctions.spline(builder.build());
    }

    private static DensityFunction getNoise(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key) {
        return DensityFunctions.noise(noises.getOrThrow(key), 1.0, 0.0);
    }

    private static DensityFunctions.Spline.Coordinate getCoordinate(DensityFunction function) {
        return new DensityFunctions.Spline.Coordinate(Holder.direct(function));
    }

    private static DensityFunction getFunction(HolderGetter<DensityFunction> functions,
            ResourceKey<DensityFunction> key) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(key));
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
        return ResourceKey.create(Registries.NOISE, GuWorld.id("southern_border/" + name));
    }

    private static ResourceKey<DensityFunction> functionKey(String name) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, GuWorld.id("southern_border/" + name));
    }
}
