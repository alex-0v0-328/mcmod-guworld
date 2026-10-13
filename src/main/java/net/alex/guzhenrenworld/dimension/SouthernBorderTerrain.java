package net.alex.guzhenrenworld.dimension;

import static net.alex.guzhenrenworld.dimension.DensityShapes.getBlend;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getCoordinate;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getRamp;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getScaled;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getSpline;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The Southern Border [南疆] terrain: mountain ranges above all, with plains between them and rivers running
 * down the valleys. Holds its noises, its surface height in blocks, and the climate fields its biome table reads,
 * every id under {@code guworld:southern_border/}.
 *
 * <p>The height is one 2D value per column, a sum in blocks:
 * <ul>
 *   <li>Floor: {@link #FLOOR_Y} ± {@link #FLOOR_SWING} over a kilometer, plus ±{@link #ROLLING_SWING} over 256
 *   blocks, the ground the plains and the valley floors share.</li>
 *   <li>Rise ({@link #getRise}): a ridged sum. The main ridge {@code 1 - |a + b|}, over two noises one and two
 *   kilometers across, peaks along their zero lines, so ranges run as long winding crests with straight flanks; three
 *   finer {@code -|noise|} terms (512, 256 and 128 blocks, weights 0.3, 0.12, 0.05) add spurs, strongest on the
 *   crests and faint on the valley floors. Every mountain noise is read at {@link #MOUNTAIN_SCALE}, which
 *   stretches the ranges and keeps their flanks walkable. The mountain share ({@link #MOUNTAINS}, 0..1, a
 *   four-kilometer noise) shifts the sum down by up to 1 where it is low, so the plains keep only the odd hill;
 *   the river valleys ({@link #getValleys}) shift it down further. A spline turns the sum into blocks, flat at 0,
 *   up to {@link #PEAK_RISE}.</li>
 *   <li>Benches: where a 512-block mask noise is high, a terrace spline flattens the rise around +56..59 and
 *   +152..155 blocks and steepens it just above, so those mountainsides carry shelves some tens of blocks wide
 *   and low summits end in flat tops, the ground the clan villages [山寨] will stand on. A kilometer-scale noise
 *   shifts both shelves by up to ±{@link #BENCH_SWING} blocks, so they never line up like contour rings.</li>
 *   <li>Detail: ±(0.6 + 2.5 × rise / 100, capped at 3.1) blocks of 64-block bumps.</li>
 * </ul>
 *
 * <p>Rivers follow the zero lines of noises, so they meander and never stop on a plain:
 * <ul>
 *   <li>Great rivers ({@link #GREAT_RIVER}): a four-kilometer noise with finer octaves, a few per eight
 *   kilometers. Within ±{@link #BASIN_WIDTH} of the line the mountains sink by up to {@link #BASIN_CUT}, within
 *   ±{@link #GREAT_VALLEY_WIDTH} by {@link #GREAT_VALLEY_CUT} more, so every great river crosses the ranges down a
 *   broad valley; the channel is ±{@link #GREAT_RIVER_WIDTH}, some 30..60 blocks wide.</li>
 *   <li>Streams ({@link #STREAM}): two two-kilometer noises, one on each side of the great rivers, blended across
 *   the great channel, so a stream that meets a great river joins it like a tributary instead of crossing it.
 *   Their valleys sink the mountains by {@link #STREAM_VALLEY_CUT} within ±{@link #STREAM_VALLEY_WIDTH}, which
 *   carries them through low passes; under high ranges they end as dry valleys, their headwaters.</li>
 *   <li>Floodplains ({@link #getFloodplains}): beside both channels ground below y 72 is flattened to
 *   {@link #FLOODPLAIN_Y}, fading out by y 90, so the floodplain never cuts a slot into a hillside.</li>
 *   <li>Channels ({@link #registerChannels}): {@link #CHANNEL} is the distance to the nearer channel in channel
 *   half-widths, 0 at the line and 1 at the bank. Where the floodplain lies below y 66 the bed sinks to
 *   {@link #GREAT_RIVER_DEPTH} or {@link #STREAM_DEPTH} below sea level and the sea level fills it; by y 74
 *   the channel fades out, so a stream climbing into the mountains leaves its water at a shore.</li>
 * </ul>
 *
 * <p>{@link #SHORE} is the channel distance where the channel holds water and at least {@link #DRY_SHORE}
 * elsewhere. The router keeps cave entrances out of the riverbeds with it, and the biome table reads it, scaled
 * by {@code SouthernBorderBiomes.RIVER_WEIRDNESS}, as the weirdness, so the river biome covers exactly the wet
 * channels. {@link #ELEVATION} is the height mapped to -1 at y 50 and 1 at y 400, clamped; it is the erosion
 * parameter of the biome table, so biome bands follow altitude and a peak biome only ever lands on a peak. The
 * mountain share stands in as continentalness. The full vanilla cave set stays under the floor, below
 * {@link #CAVE_CEILING_Y}; above it the mountains keep only cave tunnels ({@link HeightfieldRouter}).
 *
 * <p>Every function read in several places is a registered density function: only a shared holder shares its
 * cache, and only inside chunk generation; a point query outside it recomputes the whole chain.
 * {@link DensityShapes} holds the spline and blend helpers.
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
    private static final ResourceKey<NormalNoise.NoiseParameters> ROLLING_NOISE = noiseKey("rolling");
    private static final ResourceKey<NormalNoise.NoiseParameters> RIDGE_COARSE_NOISE = noiseKey("ridge_coarse");
    private static final ResourceKey<NormalNoise.NoiseParameters> RIDGE_FINE_NOISE = noiseKey("ridge_fine");
    private static final ResourceKey<NormalNoise.NoiseParameters> SPUR_A_NOISE = noiseKey("spur_a");
    private static final ResourceKey<NormalNoise.NoiseParameters> SPUR_B_NOISE = noiseKey("spur_b");
    private static final ResourceKey<NormalNoise.NoiseParameters> SPUR_C_NOISE = noiseKey("spur_c");
    private static final ResourceKey<NormalNoise.NoiseParameters> BENCH_NOISE = noiseKey("bench");
    private static final ResourceKey<NormalNoise.NoiseParameters> BENCH_MASK_NOISE = noiseKey("bench_mask");
    private static final ResourceKey<NormalNoise.NoiseParameters> DETAIL_NOISE = noiseKey("detail");
    private static final ResourceKey<NormalNoise.NoiseParameters> GREAT_RIVER_NOISE = noiseKey("great_river");
    private static final ResourceKey<NormalNoise.NoiseParameters> STREAM_A_NOISE = noiseKey("stream_a");
    private static final ResourceKey<NormalNoise.NoiseParameters> STREAM_B_NOISE = noiseKey("stream_b");
    private static final ResourceKey<NormalNoise.NoiseParameters> TEMPERATURE_NOISE = noiseKey("temperature");
    private static final ResourceKey<NormalNoise.NoiseParameters> VEGETATION_NOISE = noiseKey("vegetation");
    private static final ResourceKey<DensityFunction> GREAT_RIVER = functionKey("great_river");
    private static final ResourceKey<DensityFunction> STREAM = functionKey("stream");
    private static final ResourceKey<DensityFunction> MOUNTAINS = functionKey("mountains");
    private static final ResourceKey<DensityFunction> RISE = functionKey("rise");
    private static final ResourceKey<DensityFunction> GROUND = functionKey("ground");
    private static final ResourceKey<DensityFunction> LAND = functionKey("land");
    private static final ResourceKey<DensityFunction> CHANNEL = functionKey("channel");
    private static final ResourceKey<DensityFunction> HEIGHT = functionKey("height");
    private static final ResourceKey<DensityFunction> SHORE = functionKey("shore");
    private static final ResourceKey<DensityFunction> ELEVATION = functionKey("elevation");
    private static final double FLOOR_Y = 70.0;
    private static final double FLOOR_SWING = 6.0;
    private static final double ROLLING_SWING = 2.5;
    private static final double MOUNTAIN_SCALE = 0.8;
    private static final float MOUNTAINS_START = -1.0F;
    private static final float MOUNTAINS_FULL = -0.35F;
    private static final float PEAK_RISE = 310.0F;
    private static final double BENCH_SWING = 16.0;
    private static final float[] TERRACE = {
            -40.0F, -40.0F, 1.0F, 45.0F, 45.0F, 1.0F, 60.0F, 56.0F, 0.15F, 80.0F, 59.0F, 0.15F, 100.0F, 100.0F, 1.3F,
            140.0F, 140.0F, 1.0F, 160.0F, 152.0F, 0.15F, 180.0F, 155.0F, 0.15F, 205.0F, 205.0F, 1.3F,
            420.0F, 420.0F, 1.0F };
    private static final float GREAT_RIVER_WIDTH = 0.01F;
    private static final float GREAT_VALLEY_WIDTH = 0.15F;
    private static final double GREAT_VALLEY_CUT = 0.5;
    private static final float BASIN_WIDTH = 0.4F;
    private static final double BASIN_CUT = 0.3;
    private static final float STREAM_WIDTH = 0.004F;
    private static final float STREAM_VALLEY_WIDTH = 0.08F;
    private static final double STREAM_VALLEY_CUT = 0.2;
    private static final float GREAT_FLOODPLAIN = 0.05F;
    private static final float STREAM_FLOODPLAIN = 0.02F;
    private static final double FLOODPLAIN_Y = 64.5;
    private static final float GREAT_RIVER_DEPTH = 9.0F;
    private static final float STREAM_DEPTH = 4.0F;
    private static final double DRY_SHORE = 8.0;
    private static final double CAVE_CEILING_Y = 62.0;

    private SouthernBorderTerrain() {}

    private record Rivers(DensityFunction great, Holder<DensityFunction> stream) {}

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        context.register(CONTINENT_NOISE, new NormalNoise.NoiseParameters(-12, 1.0, 1.0, 0.5));
        context.register(BASIN_NOISE, new NormalNoise.NoiseParameters(-10, 1.0, 0.5));
        context.register(ROLLING_NOISE, new NormalNoise.NoiseParameters(-8, 1.0));
        context.register(RIDGE_COARSE_NOISE, new NormalNoise.NoiseParameters(-11, 1.0));
        context.register(RIDGE_FINE_NOISE, new NormalNoise.NoiseParameters(-10, 1.0));
        context.register(SPUR_A_NOISE, new NormalNoise.NoiseParameters(-9, 1.0));
        context.register(SPUR_B_NOISE, new NormalNoise.NoiseParameters(-8, 1.0));
        context.register(SPUR_C_NOISE, new NormalNoise.NoiseParameters(-7, 1.0));
        context.register(BENCH_NOISE, new NormalNoise.NoiseParameters(-10, 1.0));
        context.register(BENCH_MASK_NOISE, new NormalNoise.NoiseParameters(-9, 1.0));
        context.register(DETAIL_NOISE, new NormalNoise.NoiseParameters(-6, 1.0));
        context.register(GREAT_RIVER_NOISE, new NormalNoise.NoiseParameters(-12, 1.0, 1.5, 1.0, 0.5));
        context.register(STREAM_A_NOISE, new NormalNoise.NoiseParameters(-11, 1.0, 0.7, 0.35));
        context.register(STREAM_B_NOISE, new NormalNoise.NoiseParameters(-11, 1.0, 0.7, 0.35));
        context.register(TEMPERATURE_NOISE, new NormalNoise.NoiseParameters(-11, 1.0, 0.5));
        context.register(VEGETATION_NOISE, new NormalNoise.NoiseParameters(-10, 1.0, 0.5));
    }

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
        Holder<DensityFunction> greatRiver =
                context.register(GREAT_RIVER, DensityFunctions.flatCache(getNoise(noises, GREAT_RIVER_NOISE)));
        DensityFunction side = getSpline(getCoordinate(greatRiver),
                -GREAT_RIVER_WIDTH, 0.0F, 0.0F, GREAT_RIVER_WIDTH, 1.0F, 0.0F);
        Holder<DensityFunction> stream = context.register(STREAM, DensityFunctions.flatCache(DensityFunctions.lerp(
                side, getNoise(noises, STREAM_B_NOISE).abs(), getNoise(noises, STREAM_A_NOISE).abs())));
        Holder<DensityFunction> mountains = context.register(MOUNTAINS, DensityFunctions.flatCache(getSpline(
                getCoordinate(getNoise(noises, CONTINENT_NOISE)), MOUNTAINS_START, 0.0F, 0.0F, MOUNTAINS_FULL, 1.0F,
                0.0F)));
        Rivers rivers = new Rivers(new DensityFunctions.HolderHolder(greatRiver).abs(), stream);
        Holder<DensityFunction> rise =
                context.register(RISE, DensityFunctions.flatCache(getRise(noises, mountains, rivers)));
        Holder<DensityFunction> ground = context.register(GROUND, DensityFunctions.flatCache(getGround(noises, rise)));
        Holder<DensityFunction> land =
                context.register(LAND, DensityFunctions.flatCache(getFloodplains(ground, rivers)));
        registerChannels(context, land, rivers);
    }

    public static NoiseRouter createRouter(HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction shore = getFunction(functions, SHORE);
        return HeightfieldRouter.create(functions, noises, new HeightfieldRouter.Region(
                getFunction(functions, HEIGHT),
                CAVE_CEILING_Y,
                shore,
                DensityFunctions.flatCache(getNoise(noises, TEMPERATURE_NOISE)),
                DensityFunctions.flatCache(getNoise(noises, VEGETATION_NOISE)),
                getFunction(functions, MOUNTAINS),
                getFunction(functions, ELEVATION),
                getScaled(SouthernBorderBiomes.RIVER_WEIRDNESS, shore).clamp(0.0, 1.0)));
    }

    private static DensityFunction getRise(HolderGetter<NormalNoise.NoiseParameters> noises,
            Holder<DensityFunction> mountains, Rivers rivers) {
        DensityFunction ridge = getBlend(1.0, -1.0, DensityFunctions.add(getMountainNoise(noises, RIDGE_COARSE_NOISE),
                getMountainNoise(noises, RIDGE_FINE_NOISE)).abs()).clamp(0.0, 1.0);
        DensityFunction spurs = DensityFunctions.add(DensityFunctions.add(
                        getScaled(-0.3, getMountainNoise(noises, SPUR_A_NOISE).abs()),
                        getScaled(-0.12, getMountainNoise(noises, SPUR_B_NOISE).abs())),
                getScaled(-0.05, getMountainNoise(noises, SPUR_C_NOISE).abs()));
        DensityFunction ridged = DensityFunctions.add(ridge, DensityFunctions.mul(spurs, getBlend(0.3, 0.7, ridge)));
        DensityFunction relief = DensityFunctions.add(
                DensityFunctions.add(ridged, getBlend(-1.0, 1.0, new DensityFunctions.HolderHolder(mountains))),
                getScaled(-1.0, getValleys(rivers)));
        return getSpline(getCoordinate(relief),
                0.0F, 0.0F, 0.0F,
                0.3F, 0.22F * PEAK_RISE, 1.25F * PEAK_RISE,
                0.75F, 0.8F * PEAK_RISE, 1.2F * PEAK_RISE,
                1.0F, PEAK_RISE, 0.35F * PEAK_RISE);
    }

    private static DensityFunction getValleys(Rivers rivers) {
        DensityFunctions.Spline.Coordinate great = getCoordinate(rivers.great());
        return DensityFunctions.add(DensityFunctions.add(
                        getScaled(GREAT_VALLEY_CUT, getRamp(great, GREAT_RIVER_WIDTH, GREAT_VALLEY_WIDTH)),
                        getScaled(BASIN_CUT, getRamp(great, GREAT_RIVER_WIDTH, BASIN_WIDTH))),
                getScaled(STREAM_VALLEY_CUT, getRamp(getCoordinate(rivers.stream()),
                        STREAM_WIDTH, STREAM_VALLEY_WIDTH)));
    }

    private static DensityFunction getGround(HolderGetter<NormalNoise.NoiseParameters> noises,
            Holder<DensityFunction> rise) {
        DensityFunction risen = new DensityFunctions.HolderHolder(rise);
        DensityFunction shift = getScaled(BENCH_SWING, getNoise(noises, BENCH_NOISE));
        DensityFunction terraced = DensityFunctions.add(
                getSpline(getCoordinate(DensityFunctions.add(risen, shift)), TERRACE), getScaled(-1.0, shift));
        DensityFunction mask = getSpline(getCoordinate(getNoise(noises, BENCH_MASK_NOISE)),
                -0.1F, 0.0F, 0.0F, 0.25F, 1.0F, 0.0F);
        DensityFunction benched = DensityFunctions.max(DensityFunctions.lerp(mask, risen, terraced),
                DensityFunctions.zero());
        DensityFunction floor = DensityFunctions.add(
                getBlend(FLOOR_Y, FLOOR_SWING, getNoise(noises, BASIN_NOISE)),
                getScaled(ROLLING_SWING, getNoise(noises, ROLLING_NOISE)));
        DensityFunction detail = DensityFunctions.mul(getNoise(noises, DETAIL_NOISE),
                getBlend(0.6, 2.5, getScaled(0.01, risen).clamp(0.0, 1.0)));
        return DensityFunctions.add(DensityFunctions.add(floor, benched), detail);
    }

    private static DensityFunction getFloodplains(Holder<DensityFunction> ground, Rivers rivers) {
        DensityFunction plain = DensityFunctions.min(
                getSpline(getCoordinate(rivers.great()), GREAT_RIVER_WIDTH, 0.0F, 0.0F, GREAT_FLOODPLAIN, 1.0F, 0.0F),
                getSpline(getCoordinate(rivers.stream()),
                        STREAM_WIDTH, 0.0F, 0.0F, STREAM_FLOODPLAIN, 1.0F, 0.0F));
        DensityFunction lowland = getSpline(getCoordinate(ground),
                72.0F, 1.0F, 0.0F, 90.0F, 0.0F, 0.0F);
        DensityFunction keep = getBlend(1.0, -1.0, DensityFunctions.mul(getBlend(1.0, -1.0, plain), lowland));
        return DensityFunctions.lerp(keep, FLOODPLAIN_Y, new DensityFunctions.HolderHolder(ground));
    }

    private static void registerChannels(BootstrapContext<DensityFunction> context, Holder<DensityFunction> land,
            Rivers rivers) {
        DensityFunction greatSpan = getScaled(1.0 / GREAT_RIVER_WIDTH, rivers.great());
        DensityFunction streamSpan = getScaled(1.0 / STREAM_WIDTH, new DensityFunctions.HolderHolder(rivers.stream()));
        Holder<DensityFunction> channel =
                context.register(CHANNEL, DensityFunctions.flatCache(DensityFunctions.min(greatSpan, streamSpan)));
        DensityFunction wet = getSpline(getCoordinate(land),
                SouthernBorder.SEA_LEVEL + 3.0F, 1.0F, 0.0F, SouthernBorder.SEA_LEVEL + 11.0F, 0.0F, 0.0F);
        DensityFunction depth = getSpline(getCoordinate(DensityFunctions.add(greatSpan, getScaled(-1.0, streamSpan))),
                -0.5F, GREAT_RIVER_DEPTH, 0.0F, 0.5F, STREAM_DEPTH, 0.0F);
        DensityFunction inside = getSpline(getCoordinate(channel),
                0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F);
        DensityFunction bed = getBlend(SouthernBorder.SEA_LEVEL, -1.0, depth);
        DensityFunction carved =
                DensityFunctions.lerp(DensityFunctions.mul(inside, wet), new DensityFunctions.HolderHolder(land), bed);
        Holder<DensityFunction> height =
                context.register(HEIGHT, DensityFunctions.flatCache(DensityFunctions.cache2d(carved)));
        context.register(SHORE, DensityFunctions.flatCache(DensityFunctions.add(
                new DensityFunctions.HolderHolder(channel), getScaled(DRY_SHORE, getBlend(1.0, -1.0, wet)))));
        context.register(ELEVATION, DensityFunctions.flatCache(
                getBlend(-225.0 / 175.0, 1.0 / 175.0, new DensityFunctions.HolderHolder(height)).clamp(-1.0, 1.0)));
    }

    private static DensityFunction getNoise(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key) {
        return DensityFunctions.noise(noises.getOrThrow(key), 1.0, 0.0);
    }

    private static DensityFunction getMountainNoise(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key) {
        return DensityFunctions.noise(noises.getOrThrow(key), MOUNTAIN_SCALE, 0.0);
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
