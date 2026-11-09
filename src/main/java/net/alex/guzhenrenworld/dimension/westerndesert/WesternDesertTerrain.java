package net.alex.guzhenrenworld.dimension.westerndesert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Reshaping;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * The Western Desert [西漠] terrain: the region's copy of Tectonic's overworld router as Tectonic runs it beside
 * Terralith ({@link Ports#registerDensityFunctions}), with two of its functions reshaped ({@link #getReshaped}) and
 * the hooks the port leaves in it filled ({@link #HOOKS}, {@link RegionBootstraps}).
 *
 * <p>The two reshaped functions:
 * <ul>
 *   <li>The temperature, {@value Reshaping#TEMPERATURE}, which Terralith's table, Tectonic's landform regions and its
 *   dunes all read: Tectonic's temperature noise plus {@link #TEMPERATURE_OFFSET}. Tectonic's own centers on the
 *   temperate band and leaves the hot band nearly empty; shifted, about three quarters of the region is hot and the
 *   rest warm, with no cold cell, so the table's deserts and badlands take the hot cells and Tectonic's dunes grow
 *   there.</li>
 *   <li>The folded erosion, {@value Reshaping#EROSION_FOLDED}, the distance of Tectonic's erosion from zero, which sets
 *   both the terrain splines and the erosion Terralith's table reads: low means mountains. Below Tectonic's spire band
 *   it is stretched by {@link #EROSION_SCALE}, lifted by {@link #EROSION_OFFSET} and capped at {@link #EROSION_CAP}, so
 *   the mountain and hill bands narrow and the flats widen without spilling into the spire band; from {@link
 *   #SPIRE_EROSION} up it stays as Tectonic computes it, so the spire cells, the desert spires, keep the share they
 *   have in Tectonic.</li>
 * </ul>
 *
 * <p>The hooks:
 * <ul>
 *   <li>{@code ocean_offset}: Tectonic's continentalness is its folded continent noise plus this offset
 *   ({@link #OCEAN_OFFSET}), the Southern Border's [南疆] -0.1, which lifts the lowest ground inland of the coast
 *   band so no sea remains.</li>
 *   <li>{@code vertical_scale}: the stretch of the height above sea level, {@link #VERTICAL_SCALE}, so the highest
 *   peaks stand at about y 210 instead of Tectonic's 300.</li>
 *   <li>{@code dunes}: Tectonic's dunes, which rise in hot, dry cells of its diamond landform region on the flat
 *   erosion band, on ({@link #DUNES}).</li>
 *   <li>{@code min_y}, {@code max_y}, {@code jungle_pillars}, {@code underground_rivers} with
 *   {@code underground_river_water}, and {@code lava_tunnels}: as in every region; the underground rivers would also
 *   flood the aquifers under the valleys the region keeps dry.</li>
 * </ul>
 * {@link ColumnCaches} wraps the ported column functions, the reshaped ones included, so a noise chunk computes each
 * once per column.
 *
 * @author Alex
 * @version 1.0.0
 * @see WesternDesert
 * @since 1.0.0
 */

public final class WesternDesertTerrain {

    private static final double TEMPERATURE_OFFSET = 0.6;
    private static final double EROSION_SCALE = 1.45;
    private static final double EROSION_OFFSET = 0.16;
    private static final double EROSION_CAP = 0.98;
    private static final double SPIRE_EROSION = 1.0;
    private static final double SPIRE_EROSION_TOP = 1.0E6;
    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 0.65;
    private static final double DUNES = 1.0;
    public static final Region.Hooks HOOKS = new Region.Hooks(DensityFunctions.constant(OCEAN_OFFSET),
            VERTICAL_SCALE, DUNES, WesternDesertTerrain::getReshaped);

    private WesternDesertTerrain() {}

    private static JsonElement getReshaped(String name, JsonElement function) {
        if (name.equals(Reshaping.TEMPERATURE)) return Reshaping.getOperation("add", TEMPERATURE_OFFSET, function);
        if (name.equals(Reshaping.EROSION_FOLDED)) {
            JsonElement stretched = Reshaping.getOperation("add", EROSION_OFFSET,
                    Reshaping.getOperation("mul", EROSION_SCALE, function));
            JsonObject spires = new JsonObject();
            spires.addProperty("type", "minecraft:range_choice");
            spires.add("input", function);
            spires.addProperty("min_inclusive", SPIRE_EROSION);
            spires.addProperty("max_exclusive", SPIRE_EROSION_TOP);
            spires.add("when_in_range", function);
            spires.add("when_out_of_range", Reshaping.getOperation("min", stretched, EROSION_CAP));
            return spires;
        }
        return function;
    }
}
