package net.alex.guzhenrenworld.dimension.southernborder;

import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * The Southern Border [南疆] terrain: Tectonic's overworld router as Tectonic runs it beside Terralith, ported
 * ({@link Ports}) with Tectonic's default config baked in, reshaped only through the hooks the port leaves in it,
 * {@code guworld:terrain/hook/*}, which {@link RegionBootstraps#registerDensityFunctions} registers beside the ported
 * functions:
 * <ul>
 *   <li>{@code ocean_offset}: Tectonic's continentalness is its folded continent noise plus this offset, where
 *   Tectonic's -0.8 leaves a third of the world land. {@link #OCEAN_OFFSET} lifts the lowest ground, the noise's zero
 *   lines where Tectonic's seas run, just inland of the coast band, so the region has no ocean, beach or island: its
 *   water is the rivers Tectonic cuts where its ridge noise crosses zero, at about y 56 in every region but the
 *   mountains, and the lakes along those lowest lines.</li>
 *   <li>{@code vertical_scale}: the stretch of the height above sea level, {@link #VERTICAL_SCALE}, Tectonic's
 *   own.</li>
 *   <li>{@code min_y} and {@code max_y}: Tectonic's height limits, the dimension's floor and top
 *   ({@link Region#MIN_Y}, {@link Region#TOP_Y}). The offset tops out 17 blocks under the top and the
 *   terrain fades to air from 30 to 10 blocks under it, so the highest peaks stand at about y 300.</li>
 *   <li>{@code dunes}: none. Tectonic raises dunes in hot, dry cells, which the region maps to plains.</li>
 *   <li>{@code jungle_pillars}, {@code underground_rivers} with {@code underground_river_water}, and
 *   {@code lava_tunnels}: three Tectonic features, switched in {@link RegionBootstraps} and off in every region.</li>
 * </ul>
 * Everything else stays as Tectonic and Terralith ship it: Tectonic's macro scale (continents about four times
 * vanilla's, erosion and ridges twice), its climate-picked landform regions, slope-warped mountain ridges with
 * weathering cracks and valley rivers; Terralith's cold spikes and megacave noise.
 *
 * <p>{@link ColumnCaches} wraps the ported column functions so a noise chunk computes each once per column. Every
 * function and the router register under this region's prefix ({@link Ports#getRegionalKey}),
 * the hooks at {@link Ports#getHookKey}.
 *
 * @author Alex
 * @version 1.0.0
 * @see SouthernBorder
 * @since 1.0.0
 */

public final class Terrain {

    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 1.125;
    public static final Region.Hooks HOOKS = new Region.Hooks(DensityFunctions.constant(OCEAN_OFFSET), VERTICAL_SCALE,
            0.0, Region.Hooks.AS_PORTED);

    private Terrain() {}
}
