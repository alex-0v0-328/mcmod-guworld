package net.alex.guzhenrenworld.dimension.southernborder;

import static net.alex.guzhenrenworld.terrain.DensityShapes.getBlend;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getChoice;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getCoordinate;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getDifference;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getFunction;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getNoise;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getScaled;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getSmoothstep;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getSpline;

import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.terrain.HeightfieldRouter;
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
 * The Southern Border [南疆] terrain: mountain ranges above all, with plains between them, rivers running down the
 * valleys and cliff country in a third of the region. Holds its noises, its surface heights in blocks, and the
 * climate fields its biome table reads, every id under {@code guworld:southern_border/}.
 *
 * <p>The ground is one 2D value per column, a sum in blocks:
 * <ul>
 *   <li>Floor: {@link #FLOOR_Y} ± {@link #FLOOR_SWING} over a kilometer, plus ±{@link #ROLLING_SWING} over 256
 *   blocks, the ground the plains and the valley floors share.</li>
 *   <li>Rise ({@link #getRise}): a ridged sum. The main ridge {@code 1 - |a + b|}, over two noises one and two
 *   kilometers across, peaks along their zero lines, so ranges run as long winding crests with straight flanks; three
 *   finer {@code -|noise|} terms (512, 256 and 128 blocks, weights 0.3, 0.12, 0.05) add spurs, strongest on the
 *   crests and faint on the valley floors. Every mountain noise is read at {@link #MOUNTAIN_SCALE}, which
 *   stretches the ranges and keeps their flanks walkable. The mountain share ({@link #MOUNTAINS}, 0..1, a
 *   four-kilometer noise) shifts the sum down by up to 1 where it is low, so the plains keep only the odd hill;
 *   the river valleys ({@link Rivers}) shift it down further. A spline turns the sum into blocks,
 *   flat at 0, up to {@link #PEAK_RISE}. {@link #RIDGE} holds the main ridge and {@link #RANGE} the sum without
 *   spurs and stream hollows, a field that changes only over kilometers, which also tells the streams where their
 *   headwaters lie.</li>
 *   <li>Benches: where a 512-block mask noise is high, a terrace spline flattens the rise around +42..45 and
 *   +115..118 blocks and steepens it just above, so those mountainsides carry shelves some tens of blocks wide
 *   and low summits end in flat tops, the ground the clan villages [山寨] will stand on. A kilometer-scale noise
 *   shifts both shelves by up to ±{@link #BENCH_SWING} blocks, so they never line up like contour rings.</li>
 *   <li>Mountain detail ({@link Mountains}): slope-following ridges on the flanks and vanilla's jagged
 *   noise on the peaks, added to the rise before the benches are cut, so shelves stay flat.</li>
 *   <li>Detail: ±(0.6 + 2.5 × rise / 100, capped at 3.1) blocks of 64-block bumps.</li>
 * </ul>
 * The sum bends smoothly from y {@link #GROUND_CAP_START} toward {@link #GROUND_CAP}, which open ground never passes,
 * so the router's roughness and the jagged peaks stay under the y 320 cap.
 *
 * <p>{@link Landforms} reshapes a third of this ground into cliff country and caps it at y 320. A
 * cliff is two heights and an edge: {@link #FOOT}, the ground at its foot, {@link #TOP}, the ground on top, and the
 * landform edge field, positive where the top holds; open ground has the two heights equal. The rivers flood and
 * carve both heights, and {@link #HEIGHT}, the actual surface, takes the top where the edge is positive and the
 * foot elsewhere.
 *
 * <p>{@link #ELEVATION} is the surface mapped to -1 at y 50 and 1 at y 320, clamped; it is the erosion parameter of the
 * biome table, so biome bands follow altitude and a peak biome only ever lands on a peak. The landform code stands in
 * as continentalness. The weirdness is a 512-block variant noise, stretched {@link #VARIANT_SPREAD} times so its far
 * tops, where the table's rare biomes sit, cover a few percent, with its sign kept and its size held at
 * {@link #LAND_WEIRDNESS} or more ({@link #getWeirdness}), except inside a channel that holds water, where it is the
 * river shore scaled into the river band, below {@code BiomeLayout.RIVER_WEIRDNESS}. The full vanilla cave set
 * stays under the floor, below {@link #CAVE_CEILING_Y}; above it the mountains keep only cave tunnels
 * ({@link HeightfieldRouter}), and the landform zones none, so their walls stay whole.
 *
 * <p>A function read in several places is registered, since only a shared holder shares its cache, and only in
 * chunk generation: a point query recomputes the whole chain. {@link #FOOT}, {@link #TOP} and {@link #HEIGHT} also
 * keep the last column they computed ({@link #getColumnCached}): the aquifers and the surface rules find the
 * preliminary surface of a column, often one outside the chunk where the flat cache holds nothing, by stepping down it
 * four blocks at a time, and without that cache every step would recompute the whole chain. {@link DensityShapes}
 * holds the shared helpers.
 *
 * <p>⚠ Every number here is a prototype for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see HeightfieldRouter
 * @since 1.0.0
 */

public final class Terrain {

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
    private static final ResourceKey<NormalNoise.NoiseParameters> TEMPERATURE_NOISE = noiseKey("temperature");
    private static final ResourceKey<NormalNoise.NoiseParameters> VEGETATION_NOISE = noiseKey("vegetation");
    private static final ResourceKey<NormalNoise.NoiseParameters> VARIANT_NOISE = noiseKey("variant");
    private static final ResourceKey<DensityFunction> MOUNTAINS = functionKey("mountains");
    private static final ResourceKey<DensityFunction> RIDGE = functionKey("ridge");
    private static final ResourceKey<DensityFunction> RANGE = functionKey("range");
    private static final ResourceKey<DensityFunction> RISE = functionKey("rise");
    private static final ResourceKey<DensityFunction> GROUND = functionKey("ground");
    private static final ResourceKey<DensityFunction> LAND = functionKey("land");
    private static final ResourceKey<DensityFunction> FOOT = functionKey("foot");
    private static final ResourceKey<DensityFunction> TOP = functionKey("top");
    private static final ResourceKey<DensityFunction> HEIGHT = functionKey("height");
    private static final ResourceKey<DensityFunction> ELEVATION = functionKey("elevation");
    private static final double FLOOR_Y = 70.0;
    private static final double FLOOR_SWING = 6.0;
    private static final double ROLLING_SWING = 2.5;
    private static final double MOUNTAIN_SCALE = 0.8;
    private static final float MOUNTAINS_START = -1.0F;
    private static final float MOUNTAINS_FULL = -0.35F;
    private static final float PEAK_RISE = 235.0F;
    private static final double BENCH_SWING = 12.0;
    private static final float[] TERRACE = {
            -30.0F, -30.0F, 1.0F, 34.0F, 34.0F, 1.0F, 45.0F, 42.0F, 0.15F, 61.0F, 45.0F, 0.15F, 76.0F, 76.0F, 1.3F,
            106.0F, 106.0F, 1.0F, 121.0F, 115.0F, 0.15F, 136.0F, 118.0F, 0.15F, 155.0F, 155.0F, 1.3F,
            320.0F, 320.0F, 1.0F };
    private static final double CAVE_CEILING_Y = 62.0;
    private static final double LAND_WEIRDNESS = 0.02;
    private static final double VARIANT_SPREAD = 1.5;
    private static final float GROUND_CAP_START = 294.0F;
    private static final float GROUND_CAP = 314.0F;

    private Terrain() {}

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        Rivers.registerNoises(context);
        Mountains.registerNoises(context);
        Landforms.registerNoises(context);
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
        context.register(TEMPERATURE_NOISE, new NormalNoise.NoiseParameters(-11, 1.0, 0.5));
        context.register(VEGETATION_NOISE, new NormalNoise.NoiseParameters(-10, 1.0, 0.5));
        context.register(VARIANT_NOISE, new NormalNoise.NoiseParameters(-9, 1.0, 1.0));
    }

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
        Rivers.Network rivers = Rivers.register(context);
        Holder<DensityFunction> mountains = context.register(MOUNTAINS, DensityFunctions.flatCache(
                getSmoothstep(getCoordinate(getNoise(noises, CONTINENT_NOISE)), MOUNTAINS_START, MOUNTAINS_FULL)));
        Holder<DensityFunction> ridge = context.register(RIDGE, DensityFunctions.flatCache(getRidge(noises)));
        Holder<DensityFunction> range = context.register(RANGE,
                DensityFunctions.flatCache(getRange(ridge, mountains, rivers.getGreatValleys())));
        Holder<DensityFunction> rise = context.register(RISE,
                DensityFunctions.flatCache(getRise(noises, ridge, range, rivers.getStreamCut())));
        Holder<DensityFunction> ground = context.register(GROUND, DensityFunctions.flatCache(getGround(noises, rise)));
        Landforms.Cliffs cliffs =
                Landforms.register(context, ground, rivers.getGreatValley(), rivers.channel());
        Mountains.registerRoughness(context, rise, cliffs.weight());
        Holder<DensityFunction> land =
                context.register(LAND, DensityFunctions.flatCache(rivers.getFloodplain(cliffs.foot())));
        DensityFunction wet = rivers.getWet(land);
        DensityFunction footValley = rivers.getStreamValley(land, range, cliffs.weight());
        DensityFunction topValley =
                rivers.getStreamValley(Holder.direct(rivers.getFloodplain(cliffs.top())), range, cliffs.weight());
        Holder<DensityFunction> foot =
                context.register(FOOT, getColumnCached(rivers.getCarved(Holder.direct(footValley), wet)));
        Holder<DensityFunction> top =
                context.register(TOP, getColumnCached(rivers.getCarved(Holder.direct(topValley), wet)));
        DensityFunction onTop = getSmoothstep(getCoordinate(cliffs.edge()), -0.05F, 0.05F);
        Holder<DensityFunction> height = context.register(HEIGHT, getColumnCached(DensityFunctions.lerp(onTop,
                new DensityFunctions.HolderHolder(foot), new DensityFunctions.HolderHolder(top))));
        Rivers.registerShore(context, rivers, height);
        context.register(ELEVATION, DensityFunctions.flatCache(
                getBlend(-185.0 / 135.0, 1.0 / 135.0, new DensityFunctions.HolderHolder(height)).clamp(-1.0, 1.0)));
    }

    public static NoiseRouter createRouter(HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction shore = Rivers.getShore(functions);
        return HeightfieldRouter.create(functions, noises, new HeightfieldRouter.Region(
                new HeightfieldRouter.Surface(getFunction(functions, HEIGHT), getFunction(functions, FOOT),
                        getFunction(functions, TOP), Landforms.getEdge(functions),
                        new HeightfieldRouter.Detail(Mountains.getRoughness(functions),
                                Landforms.getFaces(functions),
                                Landforms.getSolids(functions))),
                CAVE_CEILING_Y,
                shore,
                Landforms.getWeight(functions),
                DensityFunctions.flatCache(getNoise(noises, TEMPERATURE_NOISE)),
                DensityFunctions.flatCache(getNoise(noises, VEGETATION_NOISE)),
                Landforms.getLandform(functions),
                getFunction(functions, ELEVATION),
                getWeirdness(DensityFunctions.flatCache(getScaled(VARIANT_SPREAD, getNoise(noises, VARIANT_NOISE))),
                        shore)));
    }

    private static DensityFunction getWeirdness(DensityFunction variant, DensityFunction shore) {
        DensityFunction land = getChoice(variant, 0.0,
                DensityFunctions.min(variant, DensityFunctions.constant(-LAND_WEIRDNESS)),
                DensityFunctions.max(variant, DensityFunctions.constant(LAND_WEIRDNESS)));
        return getChoice(shore, 1.0, getScaled(0.99 * BiomeLayout.RIVER_WEIRDNESS, shore), land);
    }

    private static DensityFunction getRidge(HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction crests = DensityFunctions.add(getMountainNoise(noises, RIDGE_COARSE_NOISE),
                getMountainNoise(noises, RIDGE_FINE_NOISE)).abs();
        return getBlend(1.0, -1.0, crests).clamp(0.0, 1.0);
    }

    private static DensityFunction getRange(Holder<DensityFunction> ridge, Holder<DensityFunction> mountains,
            DensityFunction greatValleys) {
        DensityFunction share = getBlend(-1.0, 1.0, new DensityFunctions.HolderHolder(mountains));
        return getDifference(DensityFunctions.add(new DensityFunctions.HolderHolder(ridge), share), greatValleys);
    }

    private static DensityFunction getRise(HolderGetter<NormalNoise.NoiseParameters> noises,
            Holder<DensityFunction> ridge, Holder<DensityFunction> range, DensityFunction streamCut) {
        DensityFunction spurs = DensityFunctions.add(DensityFunctions.add(
                        getScaled(-0.3, getMountainNoise(noises, SPUR_A_NOISE).abs()),
                        getScaled(-0.12, getMountainNoise(noises, SPUR_B_NOISE).abs())),
                getScaled(-0.05, getMountainNoise(noises, SPUR_C_NOISE).abs()));
        DensityFunction relief = getDifference(DensityFunctions.add(new DensityFunctions.HolderHolder(range),
                DensityFunctions.mul(spurs, getBlend(0.3, 0.7, new DensityFunctions.HolderHolder(ridge)))), streamCut);
        return getSpline(getCoordinate(relief),
                0.0F, 0.0F, 0.0F,
                0.3F, 0.22F * PEAK_RISE, 1.25F * PEAK_RISE,
                0.75F, 0.8F * PEAK_RISE, 1.2F * PEAK_RISE,
                1.0F, PEAK_RISE, 0.35F * PEAK_RISE);
    }

    private static DensityFunction getGround(HolderGetter<NormalNoise.NoiseParameters> noises,
            Holder<DensityFunction> rise) {
        DensityFunction risen = DensityFunctions.add(new DensityFunctions.HolderHolder(rise),
                Mountains.getDetail(noises, rise));
        DensityFunction shift = getScaled(BENCH_SWING, getNoise(noises, BENCH_NOISE));
        DensityFunction terraced =
                getDifference(getSpline(getCoordinate(DensityFunctions.add(risen, shift)), TERRACE), shift);
        DensityFunction mask = getSmoothstep(getCoordinate(getNoise(noises, BENCH_MASK_NOISE)), -0.1F, 0.25F);
        DensityFunction benched = DensityFunctions.max(DensityFunctions.lerp(mask, risen, terraced),
                DensityFunctions.zero());
        DensityFunction floor = DensityFunctions.add(
                getBlend(FLOOR_Y, FLOOR_SWING, getNoise(noises, BASIN_NOISE)),
                getScaled(ROLLING_SWING, getNoise(noises, ROLLING_NOISE)));
        DensityFunction detail = DensityFunctions.mul(getNoise(noises, DETAIL_NOISE),
                getBlend(0.6, 2.5, getScaled(0.01, risen).clamp(0.0, 1.0)));
        DensityFunction summed = DensityFunctions.add(DensityFunctions.add(floor, benched), detail);
        return getSpline(getCoordinate(summed), GROUND_CAP_START, GROUND_CAP_START, 1.0F, GROUND_CAP_START + 40.0F,
                GROUND_CAP, 0.0F);
    }

    private static DensityFunction getColumnCached(DensityFunction height) {
        return DensityFunctions.flatCache(DensityFunctions.cache2d(height));
    }

    private static DensityFunction getMountainNoise(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key) {
        return getNoise(noises, key, MOUNTAIN_SCALE);
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
        return ResourceKey.create(Registries.NOISE, GuWorld.id("southern_border/" + name));
    }

    private static ResourceKey<DensityFunction> functionKey(String name) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, GuWorld.id("southern_border/" + name));
    }
}
