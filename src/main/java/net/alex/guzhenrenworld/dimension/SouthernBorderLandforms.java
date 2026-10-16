package net.alex.guzhenrenworld.dimension;

import static net.alex.guzhenrenworld.dimension.DensityShapes.getBlend;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getCoordinate;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getScaled;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getSpline;

import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The cliff country of the Southern Border [南疆]: where its four landforms, modeled on southern China, replace
 * {@code SouthernBorderTerrain}'s ground. {@link SouthernBorderShapes} holds what each looks like; ids live under
 * {@code guworld:southern_border/landform/}.
 *
 * <p>A cliff is three 2D fields ({@link Cliffs}): the ground at its foot, the ground on its top, and an edge field,
 * positive where the top holds and passing zero along the cliff line. The router stands a vertical wall on the
 * zero line, wherever it falls inside the game's 4-block sampling grid, so the edge field must change smoothly, from
 * -1 to 1 across some 8 blocks; {@link #EDGE} holds it clamped to that range. Open ground has foot and top equal.
 *
 * <p>Zones: a kilometer-scale noise ({@link #ZONE}) above {@link #ZONE_MIDDLE} marks about a third of the region.
 * {@link #WEIGHT} rises from 0 to 1 across ±{@link #ZONE_HALF_WIDTH} of that line, some 45 blocks either side, and
 * {@link #register} blends foot and top toward the landform by it. A two-kilometer type noise picks the landform: its
 * quartiles {@link #TYPE_CUTS} split karst, Danxia, pillars and cliffs in that order, crossfading over
 * ±{@link #TYPE_FADE}, so neighbors in the noise are neighbors on the map. Great-river valleys lower every landform
 * base toward the floodplain, and within four channel half-widths of a channel the shapes fade out, so a river always
 * runs through. Foot and top above y 300 bend smoothly toward {@link #PEAK_Y}, which nothing passes.
 *
 * <p>Beside the 2D cliff, each landform adds 3D detail that the router joins in ({@code HeightfieldRouter.Detail}):
 * {@link #FACES}, the grooves and joints its walls carry, and {@link #SOLIDS}, bodies standing on its ground, the
 * sandstone pillars. A groove is deepest on the wall and fades out {@link #FACE_REACH} of the edge field into the
 * rock, a block or two, so it cuts a groove into the face but never through a thin ledge or castle wall. Both are
 * blended by type like the heights, fade out toward the channels, and hold their neutral value outside the zones,
 * where their range gates skip them, so the region pays for the 3D noises only inside.
 *
 * <p>{@link #LANDFORM} tells the biome table where each landform holds: -1 outside the zones and wherever the zone
 * or type weight is below one half, {@link #CODES} inside, each code with its own band. {@link #WEIGHT} also tells
 * the router to keep caves out of the cliffs.
 *
 * <p>⚠ Every number here is a prototype for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see SouthernBorderTerrain
 * @since 1.0.0
 */

public final class SouthernBorderLandforms {

    static final List<Float> CODES = List.of(0.2F, 0.4F, 0.6F, 0.8F);
    static final float ZONE_MIDDLE = 0.133F;
    static final float ZONE_HALF_WIDTH = 0.08F;
    static final float PEAK_Y = 320.0F;
    static final ResourceKey<NormalNoise.NoiseParameters> ZONE_NOISE = noiseKey("zone");
    static final ResourceKey<NormalNoise.NoiseParameters> TYPE_NOISE = noiseKey("type");
    private static final ResourceKey<DensityFunction> ZONE = functionKey("zone");
    private static final ResourceKey<DensityFunction> WEIGHT = functionKey("weight");
    private static final ResourceKey<DensityFunction> LANDFORM = functionKey("landform");
    private static final ResourceKey<DensityFunction> SHAPED_FOOT = functionKey("foot");
    private static final ResourceKey<DensityFunction> SHAPED_TOP = functionKey("top");
    private static final ResourceKey<DensityFunction> EDGE = functionKey("edge");
    private static final ResourceKey<DensityFunction> FACES = functionKey("faces");
    private static final ResourceKey<DensityFunction> SOLIDS = functionKey("solids");
    static final float[] TYPE_CUTS = { -0.216F, 0.007F, 0.214F };
    private static final float TYPE_FADE = 0.03F;
    private static final double VALLEY_SINK = 0.85;
    private static final float FACE_REACH = 0.5F;

    private SouthernBorderLandforms() {}

    record Cliffs(Holder<DensityFunction> foot, Holder<DensityFunction> top, Holder<DensityFunction> edge,
            Holder<DensityFunction> weight) {}

    private record Mix(DensityFunction foot, DensityFunction top, DensityFunction edge, DensityFunction faces,
            DensityFunction solids, DensityFunction solidWeight, DensityFunction codes) {}

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        context.register(ZONE_NOISE, new NormalNoise.NoiseParameters(-10, 1.0, 0.5));
        context.register(TYPE_NOISE, new NormalNoise.NoiseParameters(-11, 1.0));
        SouthernBorderShapes.registerNoises(context);
    }

    static Cliffs register(BootstrapContext<DensityFunction> context, Holder<DensityFunction> ground,
            DensityFunction valley, Holder<DensityFunction> channel) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
        Holder<DensityFunction> zone = context.register(ZONE, DensityFunctions.flatCache(
                DensityFunctions.noise(noises.getOrThrow(ZONE_NOISE), 1.0, 0.0)));
        Holder<DensityFunction> weight = context.register(WEIGHT, DensityFunctions.flatCache(getSpline(
                getCoordinate(zone), ZONE_MIDDLE - ZONE_HALF_WIDTH, 0.0F, 0.0F, ZONE_MIDDLE + ZONE_HALF_WIDTH, 1.0F,
                0.0F)));
        Mix mix = getMix(getTypes(DensityFunctions.noise(noises.getOrThrow(TYPE_NOISE), 1.0, 0.0)),
                SouthernBorderShapes.getShapes(noises, zone),
                getSpline(getCoordinate(channel), 1.5F, 0.0F, 0.0F, 4.0F, 1.0F, 0.0F),
                getBlend(1.0, -VALLEY_SINK, valley));
        context.register(LANDFORM, DensityFunctions.flatCache(getBlend(-1.0, 1.0,
                DensityFunctions.mul(getHalf(getCoordinate(weight)), mix.codes()))));
        DensityFunction zoneWeight = new DensityFunctions.HolderHolder(weight);
        Holder<DensityFunction> edge = context.register(EDGE, DensityFunctions.flatCache(mix.edge().clamp(-1.0, 1.0)));
        DensityFunction faces = DensityFunctions.mul(mix.faces(),
                getSpline(getCoordinate(edge), 0.0F, 1.0F, 0.0F, FACE_REACH, 0.0F, 0.0F));
        context.register(FACES, DensityFunctions.rangeChoice(zoneWeight, -1000000.0, 0.001, DensityFunctions.zero(),
                faces));
        context.register(SOLIDS, DensityFunctions.rangeChoice(
                DensityFunctions.flatCache(DensityFunctions.mul(zoneWeight, mix.solidWeight())), -1000000.0, 0.001,
                DensityFunctions.constant(-1.0), DensityFunctions.lerp(zoneWeight, DensityFunctions.constant(-1.0),
                        mix.solids())));
        return new Cliffs(
                context.register(SHAPED_FOOT, DensityFunctions.flatCache(getCapped(ground, weight, mix.foot()))),
                context.register(SHAPED_TOP, DensityFunctions.flatCache(getCapped(ground, weight, mix.top()))),
                edge, weight);
    }

    static DensityFunction getWeight(HolderGetter<DensityFunction> functions) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(WEIGHT));
    }

    static DensityFunction getLandform(HolderGetter<DensityFunction> functions) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(LANDFORM));
    }

    static DensityFunction getEdge(HolderGetter<DensityFunction> functions) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(EDGE));
    }

    static DensityFunction getFaces(HolderGetter<DensityFunction> functions) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(FACES));
    }

    static DensityFunction getSolids(HolderGetter<DensityFunction> functions) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(SOLIDS));
    }

    private static DensityFunction getCapped(Holder<DensityFunction> ground, Holder<DensityFunction> weight,
            DensityFunction landform) {
        DensityFunction blended = DensityFunctions.lerp(new DensityFunctions.HolderHolder(weight),
                new DensityFunctions.HolderHolder(ground), landform);
        return getSpline(getCoordinate(blended), 300.0F, 300.0F, 1.0F, 340.0F, PEAK_Y, 0.0F);
    }

    private static Mix getMix(List<DensityFunction> types, List<SouthernBorderShapes.Shape> shapes,
            DensityFunction clear, DensityFunction sink) {
        Mix mix = new Mix(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(),
                DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
        for (int index = 0; index < shapes.size(); index++) {
            SouthernBorderShapes.Shape shape = shapes.get(index);
            DensityFunction type = types.get(index);
            DensityFunction base = getBlend(SouthernBorderRivers.FLOODPLAIN_Y, 1.0,
                    DensityFunctions.mul(getBlend(-SouthernBorderRivers.FLOODPLAIN_Y, 1.0, shape.base()), sink));
            DensityFunction body = DensityFunctions.lerp(clear, DensityFunctions.constant(-1.0),
                    shape.solids().apply(base));
            boolean solid = shape.solids() != SouthernBorderShapes.NO_SOLIDS;
            mix = new Mix(
                    getSum(mix.foot(), type, DensityFunctions.add(base, DensityFunctions.mul(shape.foot(), clear))),
                    getSum(mix.top(), type, DensityFunctions.add(base, DensityFunctions.mul(shape.top(), clear))),
                    getSum(mix.edge(), type, shape.edge()),
                    getSum(mix.faces(), type, shape.faces()),
                    getSum(mix.solids(), type, body),
                    solid ? DensityFunctions.add(mix.solidWeight(), type) : mix.solidWeight(),
                    DensityFunctions.add(mix.codes(),
                            getScaled(CODES.get(index) + 1.0, getHalf(getCoordinate(type)))));
        }
        return mix;
    }

    private static DensityFunction getSum(DensityFunction sum, DensityFunction type, DensityFunction value) {
        return DensityFunctions.add(sum, DensityFunctions.mul(type, value));
    }

    private static List<DensityFunction> getTypes(DensityFunction noise) {
        DensityFunction first = getRise(noise, TYPE_CUTS[0]);
        DensityFunction second = getRise(noise, TYPE_CUTS[1]);
        DensityFunction third = getRise(noise, TYPE_CUTS[2]);
        return List.of(getBlend(1.0, -1.0, first), DensityFunctions.add(first, getScaled(-1.0, second)),
                DensityFunctions.add(second, getScaled(-1.0, third)), third);
    }

    private static DensityFunction getRise(DensityFunction noise, float cut) {
        return getSpline(getCoordinate(noise), cut - TYPE_FADE, 0.0F, 0.0F, cut + TYPE_FADE, 1.0F, 0.0F);
    }

    private static DensityFunction getHalf(DensityFunctions.Spline.Coordinate weight) {
        return getSpline(weight, 0.499F, 0.0F, 0.0F, 0.501F, 1.0F, 0.0F);
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
        return ResourceKey.create(Registries.NOISE, GuWorld.id("southern_border/landform/" + name));
    }

    private static ResourceKey<DensityFunction> functionKey(String name) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, GuWorld.id("southern_border/landform/" + name));
    }
}
