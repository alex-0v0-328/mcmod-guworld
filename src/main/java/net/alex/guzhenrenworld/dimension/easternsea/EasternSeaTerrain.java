package net.alex.guzhenrenworld.dimension.easternsea;

import net.alex.guzhenrenworld.DatapackBootstrap;
import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.TerrainHooks;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The Eastern Sea [东海] terrain: the region's copy of Tectonic's overworld router as Tectonic runs it beside
 * Terralith ({@link Ports#registerDensityFunctions}), reshaped through the hooks the port leaves in it, which
 * {@link #registerDensityFunctions} fills ({@link TerrainHooks}):
 * <ul>
 *   <li>{@code ocean_offset}: Tectonic's continentalness is its folded continent noise plus this offset
 *   ({@link #OCEAN_OFFSET}). Where that sum falls under -0.5, Tectonic leaves its continents for its island
 *   noise: islands rise where that noise peaks, and on the deepest floor, past -1.07, flat islands stand in three
 *   erosion bands, Terralith's far-sea island cells. Tectonic's own -0.8 leaves continents several kilometers
 *   across in the region; -1.2 keeps continent land only on the continent noise's strongest peaks and hands the rest
 *   of the region to the island noise, about four fifths of it water.</li>
 *   <li>{@code vertical_scale}: the stretch of the height above sea level, {@link #VERTICAL_SCALE}, a fifth under
 *   Tectonic's 1.125, so the highest island peaks stay under y 260.</li>
 *   <li>{@code min_y} and {@code max_y}: Tectonic's height limits, the dimension's floor and top.</li>
 *   <li>{@code dunes}: none ({@link #DUNES}); the region maps Tectonic's hot, dry cells to sand islands.</li>
 *   <li>{@code jungle_pillars}, {@code underground_rivers} with {@code underground_river_water}, and
 *   {@code lava_tunnels}: three Tectonic features, each switched by one constant here and off, as in the Southern
 *   Border [南疆]: the pillars are the regular extruded columns turned down there, the rivers and tunnels cost
 *   about a quarter of the noise fill.</li>
 * </ul>
 * {@link ColumnCaches} wraps the ported column functions so a noise chunk computes each once per column.
 *
 * @author Alex
 * @version 1.0.0
 * @see EasternSea
 * @since 1.0.0
 */

public final class EasternSeaTerrain {

    private static final double OCEAN_OFFSET = -1.2;
    private static final double VERTICAL_SCALE = 0.9;
    private static final double DUNES = 0.0;
    private static final boolean JUNGLE_PILLARS = false;
    private static final boolean UNDERGROUND_RIVERS = false;
    private static final boolean LAVA_TUNNELS = false;

    private EasternSeaTerrain() {}

    public static void registerDensityFunctions(DatapackBootstrap<DensityFunction> context) {
        String region = EasternSea.NAME;
        Ports.registerDensityFunctions(context, region, ColumnCaches.ofPorted());
        TerrainHooks.constant(context, region, "ocean_offset", OCEAN_OFFSET);
        TerrainHooks.constant(context, region, "vertical_scale", VERTICAL_SCALE);
        TerrainHooks.constant(context, region, "min_y", EasternSea.MIN_Y);
        TerrainHooks.constant(context, region, "max_y", EasternSea.TOP_Y);
        TerrainHooks.constant(context, region, "dunes", DUNES);
        TerrainHooks.constant(context, region, "jungle_pillars", JUNGLE_PILLARS ? 1.0 : 0.0);
        TerrainHooks.switched(context, region, "underground_rivers", "tectonic/underground_river/total",
                UNDERGROUND_RIVERS);
        TerrainHooks.switched(context, region, "underground_river_water", "tectonic/underground_river/parameters",
                UNDERGROUND_RIVERS);
        TerrainHooks.switched(context, region, "lava_tunnels", "tectonic/lava_tunnel/total", LAVA_TUNNELS);
    }
}
