package net.alex.guzhenrenworld.dimension.southernborder;

import static net.alex.guzhenrenworld.terrain.DensityShapes.getBlend;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getCoordinate;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getDifference;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getFunction;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getScaled;
import static net.alex.guzhenrenworld.terrain.DensityShapes.getSmoothstep;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The mountain detail of the Southern Border [南疆]: what turns {@code Terrain}'s smooth ridged rise
 * into mountains that read as worn by water, every id under {@code guworld:southern_border/}. The method is
 * Tectonic's mountain ridges.
 *
 * <p>{@link #getDetail} is a 2D height in blocks added to the rise before its benches are cut: a 150-block base noise
 * plus 0.6 of a 64-block detail noise whose sampling point is pushed {@link #SLOPE_SHIFT} times the base noise's own
 * slope along each axis, so the detail stretches down the fall line and the flanks carry spurs and ravines that
 * follow the slope instead of round bumps; ±{@link #RIDGE_BLOCKS} blocks per unit. It scales with the rise, from
 * nothing at {@link #DETAIL_START} blocks to full at {@link #DETAIL_FULL}, so plains and valley floors stay smooth.
 * A groove where a single noise passes zero, Tectonic's weathering, draws worm-shaped lines across every flank at
 * this scale, since the line ignores the slope, so the detail has none.
 *
 * <p>{@link #ROUGHNESS} is the router's 3D roughness: a 32-block noise squeezed to 16 blocks vertically, up to
 * ±{@link #ROUGHNESS_MAX} density units (8 blocks each) on the highest ground, from nothing below a rise of
 * {@link #ROUGHNESS_START} and nothing inside the landform zones, whose walls carry their own faces. It moves the
 * surface a few blocks in and out along the height, enough for the odd overhang on a peak.
 *
 * <p>⚠ Every number here is a prototype for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see Terrain
 * @since 1.0.0
 */

final class Mountains {

    private static final ResourceKey<NormalNoise.NoiseParameters> RIDGE_BASE_NOISE = noiseKey("ridge_base");
    private static final ResourceKey<NormalNoise.NoiseParameters> RIDGE_DETAIL_NOISE = noiseKey("ridge_detail");
    private static final ResourceKey<NormalNoise.NoiseParameters> ROUGHNESS_NOISE = noiseKey("roughness");
    private static final ResourceKey<NormalNoise.NoiseParameters> JAGGED_NOISE = ResourceKey.create(Registries.NOISE,
            ResourceLocation.withDefaultNamespace("jagged"));
    private static final ResourceKey<DensityFunction> ROUGHNESS = functionKey("roughness");
    private static final double BASE_SCALE = 1.7;
    private static final double DETAIL_SCALE = 2.0;
    private static final double SLOPE_STEP = 10.0;
    private static final double SLOPE_SHIFT = 3000.0;
    private static final double RIDGE_BLOCKS = 20.0;
    private static final double JAGGED_SCALE = 1500.0;
    private static final float JAGGED_BLOCKS = 28.0F;
    private static final float JAGGED_START = 160.0F;
    private static final float JAGGED_FULL = 230.0F;
    private static final float DETAIL_START = 60.0F;
    private static final float DETAIL_FULL = 180.0F;
    private static final float ROUGHNESS_START = 90.0F;
    private static final float ROUGHNESS_FULL = 200.0F;
    private static final float ROUGHNESS_MAX = 0.75F;

    private Mountains() {}

    static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        context.register(RIDGE_BASE_NOISE, new NormalNoise.NoiseParameters(-8, 1.0, 0.2));
        context.register(RIDGE_DETAIL_NOISE, new NormalNoise.NoiseParameters(-7, 0.2, 1.0));
        context.register(ROUGHNESS_NOISE, new NormalNoise.NoiseParameters(-5, 1.0, 0.5));
    }

    static DensityFunction getDetail(HolderGetter<NormalNoise.NoiseParameters> noises, Holder<DensityFunction> rise) {
        Holder<NormalNoise.NoiseParameters> base = noises.getOrThrow(RIDGE_BASE_NOISE);
        DensityFunction here = DensityFunctions.noise(base, BASE_SCALE, 0.0);
        DensityFunction slopeX = getDifference(here, DensityFunctions.shiftedNoise2d(
                DensityFunctions.constant(SLOPE_STEP), DensityFunctions.zero(), BASE_SCALE, base));
        DensityFunction slopeZ = getDifference(here, DensityFunctions.shiftedNoise2d(
                DensityFunctions.zero(), DensityFunctions.constant(SLOPE_STEP), BASE_SCALE, base));
        DensityFunction detail = DensityFunctions.shiftedNoise2d(getScaled(SLOPE_SHIFT, slopeX.abs()),
                getScaled(SLOPE_SHIFT, slopeZ.abs()), DETAIL_SCALE, noises.getOrThrow(RIDGE_DETAIL_NOISE));
        DensityFunction ridges = DensityFunctions.add(here, getScaled(0.6, detail));
        DensityFunction jagged =
                DensityFunctions.noise(noises.getOrThrow(JAGGED_NOISE), JAGGED_SCALE, 0.0).halfNegative();
        return DensityFunctions.add(DensityFunctions.mul(getScaled(-RIDGE_BLOCKS, ridges),
                        getSmoothstep(getCoordinate(rise), DETAIL_START, DETAIL_FULL)),
                DensityFunctions.mul(jagged,
                        getSmoothstep(getCoordinate(rise), JAGGED_START, 0.0F, JAGGED_FULL, JAGGED_BLOCKS)));
    }

    static void registerRoughness(BootstrapContext<DensityFunction> context, Holder<DensityFunction> rise,
            Holder<DensityFunction> zones) {
        HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
        DensityFunction gate = DensityFunctions.mul(
                getSmoothstep(getCoordinate(rise), ROUGHNESS_START, 0.0F, ROUGHNESS_FULL, ROUGHNESS_MAX),
                getBlend(1.0, -1.0, new DensityFunctions.HolderHolder(zones)));
        context.register(ROUGHNESS, DensityFunctions.mul(DensityFunctions.flatCache(gate),
                DensityFunctions.noise(noises.getOrThrow(ROUGHNESS_NOISE), 1.0, 2.0)));
    }

    static DensityFunction getRoughness(HolderGetter<DensityFunction> functions) {
        return getFunction(functions, ROUGHNESS);
    }

    private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
        return ResourceKey.create(Registries.NOISE, GuWorld.id("southern_border/" + name));
    }

    private static ResourceKey<DensityFunction> functionKey(String name) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, GuWorld.id("southern_border/" + name));
    }
}
