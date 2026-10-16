package net.alex.guzhenrenworld.dimension;

import static net.alex.guzhenrenworld.dimension.DensityShapes.getBlend;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getCoordinate;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getScaled;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getSpline;

import java.util.List;
import java.util.function.UnaryOperator;
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
 * The shapes of the four Southern Border [南疆] landforms, in the order {@link SouthernBorderLandforms} blends them;
 * noise ids live under {@code guworld:southern_border/landform/}.
 *
 * <p>Each {@link Shape} is a base height in blocks, the cliff foot and top above that base, the edge field, scaled
 * so it runs from -1 to 1 across some 8 blocks of its noise, the 3D faces added to that edge, and the 3D solids
 * standing on the final base. The heights below are crests above the base; the sheer wall is the part of that rise
 * the top starts at, given for each:
 * <ul>
 *   <li>Karst [喀斯特峰林]: two 64-block noises, each above {@link #KARST_THRESHOLD} only on its highest twelfth,
 *   so their union is round towers 20..35 blocks across with crests 40..90 above a plain at y 64..70: sheer for six
 *   tenths of that, then a domed top; deeper in the zone they stand a little closer on a base raised by up to 4.
 *   Their faces carry deep vertical joints, the fluting rain cuts into limestone.</li>
 *   <li>Danxia [丹霞]: 256-block mesas above {@link #DANXIA_THRESHOLD}, tops 30..80 above the base, with a gentle
 *   apron rising a tenth of that, a sheer wall of about three quarters, and a nearly flat top: flat on top, steep in
 *   the body, gentle at the foot [顶平、身陡、麓缓]; smaller castle peaks [堡状峰] stand between them where a
 *   128-block noise passes {@link #CASTLE_THRESHOLD}. Their faces carry strong bedding grooves.</li>
 *   <li>Pillars [砂岩峰林]: solid bodies, not walls ({@link #getPillarSolids}), on a canyon floor some 40..90 below
 *   a plateau near y 175. Two noises, a 25-block one for slender pillars and a 43-block one for stout ones, rise
 *   above {@link #PILLAR_THRESHOLD}, so pillars run some 10..25 blocks across; each stands to the plateau level
 *   ±{@link #PILLAR_TOP_SWING} by a 32-block noise, so neighbors differ in height, with a slightly domed top. A
 *   pillar flares {@link #PILLAR_SKIRT} at its foot and steps in {@link #PILLAR_STEP_IN} every {@link #PILLAR_STEP}
 *   blocks upward, a ledge of about three quarters of a block, so it narrows some three blocks of radius over 60; the
 *   steps start ±{@link #PILLAR_STEP_PHASE} blocks off from place to place, and joints split the outline. The
 *   outline narrows upward, the joints aside, which lean a little, so no piece of a pillar hangs free of the rock
 *   below it. In the
 *   outer 25 blocks or so of the zone the plateau stays whole.</li>
 *   <li>Cliffs [嶂石岩]: three tiers ({@link #TIERS}) along the contours of a kilometer-scale noise, each rising
 *   33..57 blocks, 85% of it as a sheer riser, then a ledge some 35 blocks wide; the zero lines of a 256-block noise
 *   bend the walls into U-shaped coves [围谷]. Each point carries the wall nearest to it ({@link #getNearest}), so
 *   the three walls never share one edge field. Their faces carry bedding grooves and joints.</li>
 * </ul>
 * A top that only matters inside its edge reads its noise from the threshold up ({@link #getAbove}), so the top
 * stays level across the edge and the wall stands on even ground.
 *
 * <p>Two 3D noises shape every face ({@link #getFaces}): bedding, 16 blocks per cycle vertically and stretched 33
 * times horizontally, so soft beds run as near-level bands, recessed where the noise passes 0.2; joints, 32 blocks
 * across and stretched 12 times vertically, so near-vertical clefts a block or two wide open where it lies within
 * ±0.07 of zero.
 *
 * <p>⚠ Every number here is a prototype for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see SouthernBorderLandforms
 * @since 1.0.0
 */

final class SouthernBorderShapes {

    private static final ResourceKey<NormalNoise.NoiseParameters> PLAIN_NOISE = noiseKey("plain");
    private static final ResourceKey<NormalNoise.NoiseParameters> TOWER_NOISE = noiseKey("tower");
    private static final ResourceKey<NormalNoise.NoiseParameters> TWIN_TOWER_NOISE = noiseKey("twin_tower");
    private static final ResourceKey<NormalNoise.NoiseParameters> TOWER_HEIGHT_NOISE = noiseKey("tower_height");
    private static final ResourceKey<NormalNoise.NoiseParameters> HILLS_NOISE = noiseKey("hills");
    private static final ResourceKey<NormalNoise.NoiseParameters> MESA_NOISE = noiseKey("mesa");
    private static final ResourceKey<NormalNoise.NoiseParameters> MESA_HEIGHT_NOISE = noiseKey("mesa_height");
    private static final ResourceKey<NormalNoise.NoiseParameters> MESA_TOP_NOISE = noiseKey("mesa_top");
    private static final ResourceKey<NormalNoise.NoiseParameters> CASTLE_NOISE = noiseKey("castle");
    private static final ResourceKey<NormalNoise.NoiseParameters> PLATEAU_NOISE = noiseKey("plateau");
    private static final ResourceKey<NormalNoise.NoiseParameters> PILLAR_NOISE = noiseKey("pillar");
    private static final ResourceKey<NormalNoise.NoiseParameters> STOUT_PILLAR_NOISE = noiseKey("stout_pillar");
    private static final ResourceKey<NormalNoise.NoiseParameters> PILLAR_DEPTH_NOISE = noiseKey("pillar_depth");
    private static final ResourceKey<NormalNoise.NoiseParameters> PILLAR_TOP_NOISE = noiseKey("pillar_top");
    private static final ResourceKey<NormalNoise.NoiseParameters> BEDDING_NOISE = noiseKey("bedding");
    private static final ResourceKey<NormalNoise.NoiseParameters> JOINT_NOISE = noiseKey("joint");
    private static final ResourceKey<NormalNoise.NoiseParameters> CANYON_NOISE = noiseKey("canyon");
    private static final ResourceKey<NormalNoise.NoiseParameters> ESCARPMENT_NOISE = noiseKey("escarpment");
    private static final ResourceKey<NormalNoise.NoiseParameters> COVE_NOISE = noiseKey("cove");
    private static final ResourceKey<NormalNoise.NoiseParameters> TIER_NOISE = noiseKey("tier");
    private static final float KARST_THRESHOLD = 0.38F;
    private static final double TOWER_SCALE = 2.0;
    private static final float DANXIA_THRESHOLD = 0.13F;
    private static final float CASTLE_THRESHOLD = 0.38F;
    private static final float PILLAR_THRESHOLD = 0.24F;
    private static final double PILLAR_SCALE = 2.6;
    private static final double STOUT_PILLAR_SCALE = 1.5;
    private static final double STOUT_PILLAR_LIFT = -0.12;
    private static final double PILLAR_TOP_SWING = 14.0;
    private static final float PILLAR_STEP = 16.0F;
    private static final float PILLAR_STEP_IN = 0.03F;
    private static final int PILLAR_STEPS = 12;
    private static final double PILLAR_STEP_PHASE = 8.0;
    private static final double PILLAR_SKIRT = 0.06;
    private static final double PILLAR_JOINT = 0.06;
    private static final float[] TIERS = { -0.05F, 0.02F, 0.09F };
    private static final double COVE_DEPTH = 0.08;
    private static final double KARST_EDGE = 10.0;
    private static final double MESA_EDGE = 26.0;
    private static final double CASTLE_EDGE = 17.0;
    private static final double PILLAR_EDGE = 3.2;
    private static final double CLIFF_EDGE = 130.0;
    private static final double BEDDING_XZ_SCALE = 0.03;
    private static final double JOINT_Y_SCALE = 0.08;

    static final UnaryOperator<DensityFunction> NO_SOLIDS = base -> DensityFunctions.constant(-1.0);

    private SouthernBorderShapes() {}

    record Shape(DensityFunction base, DensityFunction foot, DensityFunction top, DensityFunction edge,
            DensityFunction faces, UnaryOperator<DensityFunction> solids) {}

    static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        context.register(PLAIN_NOISE, new NormalNoise.NoiseParameters(-8, 1.0));
        context.register(TOWER_NOISE, new NormalNoise.NoiseParameters(-7, 1.0));
        context.register(TWIN_TOWER_NOISE, new NormalNoise.NoiseParameters(-7, 1.0));
        context.register(TOWER_HEIGHT_NOISE, new NormalNoise.NoiseParameters(-9, 1.0));
        context.register(HILLS_NOISE, new NormalNoise.NoiseParameters(-9, 1.0));
        context.register(MESA_NOISE, new NormalNoise.NoiseParameters(-8, 1.0, 0.5));
        context.register(MESA_HEIGHT_NOISE, new NormalNoise.NoiseParameters(-10, 1.0));
        context.register(MESA_TOP_NOISE, new NormalNoise.NoiseParameters(-6, 1.0));
        context.register(CASTLE_NOISE, new NormalNoise.NoiseParameters(-7, 1.0));
        context.register(PLATEAU_NOISE, new NormalNoise.NoiseParameters(-10, 1.0));
        context.register(PILLAR_NOISE, new NormalNoise.NoiseParameters(-6, 1.0));
        context.register(STOUT_PILLAR_NOISE, new NormalNoise.NoiseParameters(-6, 1.0));
        context.register(PILLAR_DEPTH_NOISE, new NormalNoise.NoiseParameters(-9, 1.0));
        context.register(PILLAR_TOP_NOISE, new NormalNoise.NoiseParameters(-5, 1.0));
        context.register(BEDDING_NOISE, new NormalNoise.NoiseParameters(-4, 1.0, 0.5));
        context.register(JOINT_NOISE, new NormalNoise.NoiseParameters(-5, 1.0));
        context.register(CANYON_NOISE, new NormalNoise.NoiseParameters(-7, 1.0));
        context.register(ESCARPMENT_NOISE, new NormalNoise.NoiseParameters(-10, 1.0, 0.5));
        context.register(COVE_NOISE, new NormalNoise.NoiseParameters(-8, 1.0));
        context.register(TIER_NOISE, new NormalNoise.NoiseParameters(-9, 1.0));
    }

    static List<Shape> getShapes(HolderGetter<NormalNoise.NoiseParameters> noises, Holder<DensityFunction> zone) {
        return List.of(getKarst(noises, zone), getDanxia(noises), getPillars(noises, zone), getCliffs(noises, zone));
    }

    private static Shape getKarst(HolderGetter<NormalNoise.NoiseParameters> noises, Holder<DensityFunction> zone) {
        DensityFunction cluster = getSpline(getCoordinate(zone), 0.25F, 0.0F, 0.0F, 0.4F, 1.0F, 0.0F);
        DensityFunction base = DensityFunctions.add(getBlend(67.0, 3.0, getNoise(noises, PLAIN_NOISE)),
                getScaled(4.0, cluster));
        DensityFunction towers = DensityFunctions.add(DensityFunctions.max(
                getNoise(noises, TOWER_NOISE, TOWER_SCALE), getNoise(noises, TWIN_TOWER_NOISE, TOWER_SCALE)),
                getScaled(0.02, cluster));
        DensityFunction dome = getSpline(getCoordinate(getAbove(towers, KARST_THRESHOLD + 0.01F)),
                KARST_THRESHOLD + 0.01F, 0.6F, 3.0F, KARST_THRESHOLD + 0.12F, 1.0F, 0.5F);
        DensityFunction height = getBlend(65.0, 60.0, getNoise(noises, TOWER_HEIGHT_NOISE)).clamp(40.0, 90.0);
        return new Shape(base, DensityFunctions.zero(), DensityFunctions.mul(height, dome),
                getScaled(KARST_EDGE, getBlend(-KARST_THRESHOLD, 1.0, towers)), getFaces(noises, 0.3, 0.8), NO_SOLIDS);
    }

    private static Shape getDanxia(HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction mesa = getNoise(noises, MESA_NOISE);
        DensityFunction castle = getNoise(noises, CASTLE_NOISE);
        DensityFunction height = getBlend(55.0, 50.0, getNoise(noises, MESA_HEIGHT_NOISE)).clamp(30.0, 80.0);
        DensityFunction apron = getSpline(getCoordinate(mesa),
                DANXIA_THRESHOLD - 0.08F, 0.0F, 0.0F, DANXIA_THRESHOLD - 0.005F, 0.12F, 0.0F);
        DensityFunction flat = getSpline(getCoordinate(getAbove(mesa, DANXIA_THRESHOLD + 0.005F)),
                DANXIA_THRESHOLD + 0.005F, 0.9F, 0.0F, DANXIA_THRESHOLD + 0.15F, 1.0F, 0.0F);
        DensityFunction mesaTop = DensityFunctions.add(DensityFunctions.mul(height, flat),
                getScaled(1.5, getNoise(noises, MESA_TOP_NOISE)));
        DensityFunction castleTop = DensityFunctions.mul(getScaled(0.8, height),
                getSpline(getCoordinate(getAbove(castle, CASTLE_THRESHOLD + 0.01F)),
                        CASTLE_THRESHOLD + 0.01F, 0.85F, 2.0F, CASTLE_THRESHOLD + 0.1F, 1.0F, 0.0F));
        DensityFunction mesaEdge = getScaled(MESA_EDGE, getBlend(-DANXIA_THRESHOLD, 1.0, mesa));
        DensityFunction castleEdge = getScaled(CASTLE_EDGE, getBlend(-CASTLE_THRESHOLD, 1.0, castle));
        DensityFunction castleLeads = DensityFunctions.add(castleEdge, getScaled(-1.0, mesaEdge));
        return new Shape(getBlend(74.0, 10.0, getNoise(noises, HILLS_NOISE)), DensityFunctions.mul(height, apron),
                DensityFunctions.rangeChoice(castleLeads, -1000000.0, 0.0, mesaTop, castleTop),
                DensityFunctions.max(mesaEdge, castleEdge), getFaces(noises, 0.6, 0.5), NO_SOLIDS);
    }

    private static Shape getPillars(HolderGetter<NormalNoise.NoiseParameters> noises, Holder<DensityFunction> zone) {
        DensityFunction depth = getBlend(65.0, 40.0, getNoise(noises, PILLAR_DEPTH_NOISE)).clamp(40.0, 90.0);
        DensityFunction floor = DensityFunctions.add(DensityFunctions.add(
                getBlend(175.0, 20.0, getNoise(noises, PLATEAU_NOISE)), getScaled(-1.0, depth)),
                getScaled(3.0, getNoise(noises, CANYON_NOISE)));
        DensityFunction rim = getSpline(getCoordinate(zone),
                SouthernBorderLandforms.ZONE_MIDDLE, 1.0F, 0.0F, 0.18F, 0.0F, 0.0F);
        DensityFunction pillars = DensityFunctions.add(DensityFunctions.max(
                getNoise(noises, PILLAR_NOISE, PILLAR_SCALE),
                getBlend(STOUT_PILLAR_LIFT, 1.0, getNoise(noises, STOUT_PILLAR_NOISE, STOUT_PILLAR_SCALE))), rim);
        DensityFunction flat = getSpline(getCoordinate(getAbove(pillars, PILLAR_THRESHOLD + 0.01F)),
                PILLAR_THRESHOLD + 0.01F, 0.92F, 0.0F, PILLAR_THRESHOLD + 0.2F, 1.0F, 0.0F);
        DensityFunction height = DensityFunctions.mul(flat, DensityFunctions.add(depth,
                getScaled(PILLAR_TOP_SWING, getNoise(noises, PILLAR_TOP_NOISE))));
        DensityFunction core = DensityFunctions.flatCache(pillars);
        DensityFunction tops = DensityFunctions.flatCache(height);
        return new Shape(floor, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.constant(-1.0),
                DensityFunctions.zero(), base -> getPillarSolids(noises, core, tops, base));
    }

    private static DensityFunction getPillarSolids(HolderGetter<NormalNoise.NoiseParameters> noises,
            DensityFunction core, DensityFunction height, DensityFunction base) {
        DensityFunction rise = DensityFunctions.add(getY(), getScaled(-1.0, base));
        DensityFunction phased = DensityFunctions.add(rise,
                getScaled(PILLAR_STEP_PHASE, getNoise(noises, PILLAR_TOP_NOISE, 0.5)));
        float[] steps = new float[(PILLAR_STEPS + 1) * 6];
        for (int step = 0; step <= PILLAR_STEPS; step++) {
            int index = step * 6;
            steps[index] = step * PILLAR_STEP;
            steps[index + 1] = step * PILLAR_STEP_IN;
            steps[index + 3] = step * PILLAR_STEP + 2.0F;
            steps[index + 4] = (step + 1) * PILLAR_STEP_IN;
        }
        DensityFunction threshold = DensityFunctions.add(DensityFunctions.add(
                getBlend(PILLAR_THRESHOLD - PILLAR_STEP_IN, 1.0, getSpline(getCoordinate(phased), steps)),
                getScaled(-PILLAR_SKIRT, getSpline(getCoordinate(rise), 0.0F, 1.0F, 0.0F, 12.0F, 0.0F, 0.0F))),
                getScaled(-1.0, getFaces(noises, 0.0, PILLAR_JOINT)));
        DensityFunction body = getScaled(PILLAR_EDGE, DensityFunctions.add(core, getScaled(-1.0, threshold)));
        DensityFunction cap = getScaled(0.125, DensityFunctions.add(height, getScaled(-1.0, rise)));
        return DensityFunctions.min(body, cap).clamp(-1.0, 1.0);
    }

    private static Shape getCliffs(HolderGetter<NormalNoise.NoiseParameters> noises, Holder<DensityFunction> zone) {
        DensityFunction coves = getSpline(getCoordinate(getNoise(noises, COVE_NOISE).abs()),
                0.0F, 1.0F, 0.0F, 0.1F, 0.0F, 0.0F);
        DensityFunction slope = DensityFunctions.add(DensityFunctions.add(
                getBlend(-0.5 * SouthernBorderLandforms.ZONE_MIDDLE, 0.5, new DensityFunctions.HolderHolder(zone)),
                getNoise(noises, ESCARPMENT_NOISE)), getScaled(-COVE_DEPTH, coves));
        DensityFunction riser = getBlend(45.0, 12.0, getNoise(noises, TIER_NOISE));
        DensityFunction[] feet = new DensityFunction[TIERS.length];
        DensityFunction[] tops = new DensityFunction[TIERS.length];
        DensityFunction[] edges = new DensityFunction[TIERS.length];
        DensityFunction below = DensityFunctions.zero();
        for (int tier = 0; tier < TIERS.length; tier++) {
            DensityFunction step = DensityFunctions.mul(riser, getSpline(getCoordinate(slope),
                    TIERS[tier], 0.85F, 0.0F, TIERS[tier] + 0.06F, 1.0F, 0.0F));
            feet[tier] = below;
            tops[tier] = DensityFunctions.add(below, step);
            edges[tier] = getScaled(CLIFF_EDGE, getBlend(-TIERS[tier], 1.0, slope));
            below = tops[tier];
        }
        return new Shape(getBlend(80.0, 6.0, getNoise(noises, HILLS_NOISE)), getNearest(slope, feet),
                getNearest(slope, tops), getNearest(slope, edges), getFaces(noises, 0.6, 0.4), NO_SOLIDS);
    }

    private static DensityFunction getNearest(DensityFunction slope, DensityFunction[] tiers) {
        float lower = (TIERS[0] + TIERS[1]) / 2.0F;
        float upper = (TIERS[1] + TIERS[2]) / 2.0F;
        return DensityFunctions.rangeChoice(slope, -1000000.0, lower, tiers[0],
                DensityFunctions.rangeChoice(slope, lower, upper, tiers[1], tiers[2]));
    }

    private static DensityFunction getFaces(HolderGetter<NormalNoise.NoiseParameters> noises, double bedding,
            double joints) {
        DensityFunction beds = getSpline(getCoordinate(DensityFunctions.noise(noises.getOrThrow(BEDDING_NOISE),
                BEDDING_XZ_SCALE, 1.0)), 0.2F, 0.0F, 0.0F, 0.45F, 1.0F, 0.0F);
        DensityFunction clefts = getSpline(getCoordinate(DensityFunctions.noise(noises.getOrThrow(JOINT_NOISE), 1.0,
                JOINT_Y_SCALE).abs()), 0.0F, 1.0F, 0.0F, 0.07F, 0.0F, 0.0F);
        return DensityFunctions.add(getScaled(-bedding, beds), getScaled(-joints, clefts));
    }

    private static DensityFunction getY() {
        return DensityFunctions.yClampedGradient(-4064, 4062, -4064.0, 4062.0);
    }

    private static DensityFunction getAbove(DensityFunction value, float floor) {
        return DensityFunctions.max(value, DensityFunctions.constant(floor));
    }

    private static DensityFunction getNoise(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key) {
        return getNoise(noises, key, 1.0);
    }

    private static DensityFunction getNoise(HolderGetter<NormalNoise.NoiseParameters> noises,
            ResourceKey<NormalNoise.NoiseParameters> key, double scale) {
        return DensityFunctions.noise(noises.getOrThrow(key), scale, 0.0);
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
        return ResourceKey.create(Registries.NOISE, GuWorld.id("southern_border/landform/" + name));
    }
}
