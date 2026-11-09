package net.alex.guzhenrenworld.dimension.easternsea;

import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.TerrainHooks;
import net.alex.guzhenrenworld.biome.XGradient;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * The Eastern Sea [东海] terrain: the region's copy of Tectonic's overworld router as Tectonic runs it beside
 * Terralith ({@link Ports#registerDensityFunctions}), reshaped through the hooks the port leaves in it, which
 * {@link #registerDensityFunctions} fills ({@link TerrainHooks}):
 * <ul>
 *   <li>{@code ocean_offset}: Tectonic's continentalness is its folded continent noise plus this offset. Where that
 *   sum falls under -0.5, Tectonic leaves its continents for its island noise: islands rise where that noise peaks,
 *   and where the island noise sinks past -1.07, flat islands stand in three erosion bands, Terralith's far-sea
 *   island cells; under -1 neither continent nor island remains, only deep sea. Land gives way to sea within a
 *   narrow band of offsets, about -0.9 to -1, so the offset runs along x in two {@code guworld:x_gradient}s
 *   ({@link XGradient}) added together: from {@link #WEST_OCEAN_OFFSET} at the west border to
 *   {@link #ISLAND_OCEAN_OFFSET} at x {@link #ISLAND_EDGE_X}, the archipelago thinning from a coast of continent
 *   peaks and large islands into scattered islands, then a further {@link #OPEN_SEA_DROP} by x
 *   {@link #OPEN_SEA_X}, so east of it, under the island range, lies open sea.</li>
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

    private static final int WEST_X = (int) (-EasternSea.BORDER_SIZE / 2);
    private static final int ISLAND_EDGE_X = 1365;
    private static final int OPEN_SEA_X = 2048;
    private static final double WEST_OCEAN_OFFSET = -0.8;
    private static final double ISLAND_OCEAN_OFFSET = -1.0;
    private static final double OPEN_SEA_DROP = -0.9;
    private static final double VERTICAL_SCALE = 0.9;
    private static final double DUNES = 0.0;
    private static final boolean JUNGLE_PILLARS = false;
    private static final boolean UNDERGROUND_RIVERS = false;
    private static final boolean LAVA_TUNNELS = false;

    private EasternSeaTerrain() {}

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context) {
        String region = EasternSea.NAME;
        Ports.registerDensityFunctions(context, region, ColumnCaches.ofPorted());
        context.register(Ports.getHookKey(region, "ocean_offset"), DensityFunctions.add(
                new XGradient(WEST_X, ISLAND_EDGE_X, WEST_OCEAN_OFFSET, ISLAND_OCEAN_OFFSET),
                new XGradient(ISLAND_EDGE_X, OPEN_SEA_X, 0.0, OPEN_SEA_DROP)));
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
