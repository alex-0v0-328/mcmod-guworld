package net.alex.guzhenrenworld.dimension;

import static net.alex.guzhenrenworld.dimension.DensityShapes.getChoice;
import static net.alex.guzhenrenworld.dimension.DensityShapes.getFunction;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Builds the noise router of a heightfield region: terrain whose surface is one 2D height in blocks, with a little
 * 3D detail on top and vanilla's caves, aquifers and ore veins carved out of it.
 *
 * <p>{@link #create} takes the {@link Region}: the {@link Surface}, the cave ceiling, the shore, the cliff weight,
 * and the five 2D fields its biome table reads. The surface height turns into the density {@code (height - y) / 8},
 * so the surface lies where the height says and the slopes are the height's own. Eight blocks per unit is roughly
 * the rate vanilla's surface density falls at, so vanilla's cave thresholds keep their depth below ground. The
 * {@code depth} biome parameter is the same difference over 128 blocks, 0 at the surface and positive below, which is
 * what vanilla's cave biomes read.
 *
 * <p>Cliffs: the game samples the density every 4 blocks across and up and interpolates between, which turns any
 * jump in height into a 4-block ramp. So the surface also carries a cliff's foot, top and edge, and near the surface
 * the density is {@code max(foot, min(top, edge))}, the heights as {@code (height - y) / 8} clamped to ±1 and the
 * edge already within ±1, as its region registers it (a clamp around a registered function cannot be encoded).
 * Between foot and top both height terms saturate and the density is the edge field alone: the wall stands where the
 * interpolated edge crosses zero, anywhere inside a sampling cell, as smooth as the edge field is. A region without
 * cliffs passes its height as foot and top, and the density is its height's.
 *
 * <p>{@link Detail} adds what a 2D height cannot hold, each term a 3D density the region gates to where it belongs:
 * <ul>
 *   <li>Roughness, added to both height terms: a few blocks of vanilla-style undulation on high ground, enough for
 *   the odd overhang.</li>
 *   <li>Faces, added to the edge: grooves and joints in a cliff wall, which move the wall a block or two in and out
 *   along its height.</li>
 *   <li>Solids, joined by {@code max}: whole bodies standing on the ground, such as tapering pillars, -1 where a
 *   region has none.</li>
 * </ul>
 *
 * <p>Everything below the surface mirrors vanilla's {@code NoiseRouterData.overworld}: the cave layer, cheese,
 * entrance, spaghetti, pillar and noodle functions by their vanilla ids, the same aquifer noises, and the ore
 * veins between {@link #VEIN_MIN_Y} and {@link #VEIN_MAX_Y}, vanilla's copper-to-iron vein band (its
 * {@code OreVeinifier.VeinType} is not public). Three gates keep them from breaking the terrain open:
 * <ul>
 *   <li>The full cave set (cheese caverns, spaghetti, pillars) runs only some 12 blocks below both the 2D surface
 *   and the region's cave ceiling, a flat height. Vanilla measures that depth straight down, so on a steep
 *   flank a cavern 12 blocks below the surface lies a few blocks behind the slope and opens a cross-section on
 *   it; above the ceiling a mountain is solid rock with only the entrance and noodle tunnels, the caves vanilla
 *   keeps in its top 12 blocks.</li>
 *   <li>Entrances stay closed where the shore is below {@link #SHORE_GUARD}, so no cave mouth opens in a
 *   riverbed or on its bank, and wherever the cliff weight is above zero, so no tunnel opens in a cliff or a
 *   pillar.</li>
 *   <li>Noodle caves start 12 blocks below the surface, so they never riddle the ground or a riverbed; under
 *   cliffs they start 12 blocks below the cave ceiling too, so a pillar never shows one in its wall.</li>
 * </ul>
 *
 * <p>The noise column is {@link #NOISE_SETTINGS}, -64..383 in cells 4 blocks across and 4 high, twice vanilla's
 * vertical resolution, so the 3D detail keeps its shape; a region's dimension type must cover it. The slide fades
 * the density to air over the top 16 blocks and to rock over the bottom 24, as vanilla's does over its own column; a
 * height above 368 gets its peak flattened.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class HeightfieldRouter {

    public static final NoiseSettings NOISE_SETTINGS = NoiseSettings.create(-64, 448, 1, 1);
    private static final int MIN_Y = NOISE_SETTINGS.minY();
    private static final int TOP_Y = MIN_Y + NOISE_SETTINGS.height();
    private static final double SURFACE_BLOCKS_PER_UNIT = 8.0;
    private static final double DEPTH_BLOCKS_PER_UNIT = 128.0;
    private static final double SURFACE_DENSITY_THRESHOLD = 1.5625;
    private static final double CHEESE_NOISE_TARGET = -0.703125;
    private static final double SHORE_GUARD = 1.6;
    private static final double CLIFF_GUARD = 0.001;
    private static final double NO_CAVE = 64.0;
    private static final int VEIN_MIN_Y = -60;
    private static final int VEIN_MAX_Y = 50;
    private static final ResourceKey<DensityFunction> Y = vanillaKey("y");
    private static final ResourceKey<DensityFunction> ENTRANCES = vanillaKey("overworld/caves/entrances");
    private static final ResourceKey<DensityFunction> NOODLE = vanillaKey("overworld/caves/noodle");
    private static final ResourceKey<DensityFunction> PILLARS = vanillaKey("overworld/caves/pillars");
    private static final ResourceKey<DensityFunction> SPAGHETTI_2D = vanillaKey("overworld/caves/spaghetti_2d");
    private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS =
            vanillaKey("overworld/caves/spaghetti_roughness_function");

    private HeightfieldRouter() {}

    public record Detail(DensityFunction roughness, DensityFunction faces, DensityFunction solids) {}

    public record Surface(DensityFunction height, DensityFunction foot, DensityFunction top, DensityFunction edge,
            Detail detail) {}

    public record Region(Surface surface, double caveCeiling, DensityFunction shore, DensityFunction cliffs,
            DensityFunction temperature, DensityFunction vegetation, DensityFunction continents,
            DensityFunction erosion, DensityFunction ridges) {}

    public static NoiseRouter create(HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises, Region region) {
        DensityFunction surface = getRelativeHeight(region.surface().height(), SURFACE_BLOCKS_PER_UNIT);
        DensityFunction wall = getWall(region.surface());
        DensityFunction cover = DensityFunctions.min(surface,
                getRelativeHeight(DensityFunctions.constant(region.caveCeiling()), SURFACE_BLOCKS_PER_UNIT));
        DensityFunction open = getChoice(region.cliffs(), CLIFF_GUARD, DensityFunctions.min(wall,
                DensityFunctions.mul(DensityFunctions.constant(5.0), getFunction(functions, ENTRANCES))), wall);
        DensityFunction entrances = getChoice(region.shore(), SHORE_GUARD, wall, open);
        DensityFunction carved = getChoice(cover, SURFACE_DENSITY_THRESHOLD, entrances,
                getUnderground(functions, noises, cover));
        DensityFunction noodleCover = getChoice(region.cliffs(), CLIFF_GUARD, surface, cover);
        DensityFunction noodles = getChoice(noodleCover, SURFACE_DENSITY_THRESHOLD, DensityFunctions.constant(NO_CAVE),
                getFunction(functions, NOODLE));
        DensityFunction initial = DensityFunctions.add(surface, DensityFunctions.constant(CHEESE_NOISE_TARGET));
        return new NoiseRouter(
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_BARRIER), 0.5),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_LAVA)),
                region.temperature(),
                region.vegetation(),
                region.continents(),
                region.erosion(),
                getRelativeHeight(region.surface().height(), DEPTH_BLOCKS_PER_UNIT),
                region.ridges(),
                slide(initial.clamp(-64.0, 64.0)),
                DensityFunctions.min(postProcess(slide(carved)), noodles),
                getVeinBand(functions, DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEININESS), 1.5, 1.5)),
                getVeinRidged(functions, noises),
                DensityFunctions.noise(noises.getOrThrow(Noises.ORE_GAP)));
    }

    private static DensityFunction getWall(Surface surface) {
        Detail detail = surface.detail();
        DensityFunction foot = DensityFunctions.add(getRelativeHeight(surface.foot(), SURFACE_BLOCKS_PER_UNIT),
                detail.roughness()).clamp(-1.0, 1.0);
        DensityFunction top = DensityFunctions.add(getRelativeHeight(surface.top(), SURFACE_BLOCKS_PER_UNIT),
                detail.roughness()).clamp(-1.0, 1.0);
        DensityFunction wall = DensityFunctions.max(foot,
                DensityFunctions.min(top, DensityFunctions.add(surface.edge(), detail.faces())));
        return DensityFunctions.max(wall, detail.solids());
    }

    private static DensityFunction getRelativeHeight(DensityFunction height, double blocksPerUnit) {
        return DensityFunctions.add(DensityFunctions.mul(height, DensityFunctions.constant(1.0 / blocksPerUnit)),
                DensityFunctions.yClampedGradient(MIN_Y, TOP_Y, -MIN_Y / blocksPerUnit, -TOP_Y / blocksPerUnit));
    }

    private static DensityFunction getUnderground(HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises, DensityFunction cover) {
        DensityFunction layer = DensityFunctions.mul(DensityFunctions.constant(4.0),
                DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_LAYER), 8.0).square());
        DensityFunction cheese = DensityFunctions.add(
                DensityFunctions.add(DensityFunctions.constant(0.27),
                        DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666))
                        .clamp(-1.0, 1.0),
                DensityFunctions.add(DensityFunctions.constant(1.5),
                        DensityFunctions.mul(DensityFunctions.constant(-0.64), cover)).clamp(0.0, 0.5));
        DensityFunction spaghetti =
                DensityFunctions.add(getFunction(functions, SPAGHETTI_2D), getFunction(functions, SPAGHETTI_ROUGHNESS));
        DensityFunction cheeseOrEntrance =
                DensityFunctions.min(DensityFunctions.add(layer, cheese), getFunction(functions, ENTRANCES));
        DensityFunction caves = DensityFunctions.min(cheeseOrEntrance, spaghetti);
        DensityFunction pillars = getFunction(functions, PILLARS);
        return DensityFunctions.max(caves, DensityFunctions.rangeChoice(pillars, -1000000.0, 0.03,
                DensityFunctions.constant(-1000000.0), pillars));
    }

    private static DensityFunction getVeinRidged(HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises) {
        DensityFunction veinA = getVeinBand(functions,
                DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEIN_A), 4.0, 4.0)).abs();
        DensityFunction veinB = getVeinBand(functions,
                DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEIN_B), 4.0, 4.0)).abs();
        return DensityFunctions.add(DensityFunctions.constant(-0.08F), DensityFunctions.max(veinA, veinB));
    }

    private static DensityFunction getVeinBand(HolderGetter<DensityFunction> functions, DensityFunction inBand) {
        return DensityFunctions.interpolated(DensityFunctions.rangeChoice(getFunction(functions, Y), VEIN_MIN_Y,
                VEIN_MAX_Y + 1, inBand, DensityFunctions.zero()));
    }

    private static DensityFunction slide(DensityFunction density) {
        DensityFunction top = DensityFunctions.lerp(DensityFunctions.yClampedGradient(TOP_Y - 16, TOP_Y, 1.0, 0.0),
                -0.078125, density);
        return DensityFunctions.lerp(DensityFunctions.yClampedGradient(MIN_Y, MIN_Y + 24, 0.0, 1.0), 0.1171875, top);
    }

    private static DensityFunction postProcess(DensityFunction density) {
        return DensityFunctions.mul(DensityFunctions.interpolated(DensityFunctions.blendDensity(density)),
                DensityFunctions.constant(0.64)).squeeze();
    }

    private static ResourceKey<DensityFunction> vanillaKey(String path) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(path));
    }
}
