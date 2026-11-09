package net.alex.guzhenrenworld.dimension.northernplains;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Reshaping;
import net.alex.guzhenrenworld.biome.ZGradient;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * The Northern Plains [北原] terrain: the region's copy of Tectonic's overworld router as Tectonic runs it beside
 * Terralith ({@link Ports#registerDensityFunctions}), reshaped in two of its functions ({@link #getReshaped}) and
 * through the hooks the port leaves in it ({@link #HOOKS}, {@link RegionBootstraps}).
 *
 * <p>The two reshaped functions:
 * <ul>
 *   <li>The folded erosion, {@value Reshaping#EROSION_FOLDED}, the distance of Tectonic's erosion from zero, which sets
 *   both the terrain splines and the erosion Terralith's table reads: low means mountains. The region scales it by
 *   {@link #EROSION_SCALE} and lifts it by {@link #EROSION_OFFSET}, so the mountain band narrows to the lines where the
 *   erosion crosses zero and the hill band widens a little, and caps it at {@link #EROSION_CAP}, the top of Terralith's
 *   lowland band: past it the table's next band holds the glacial chasm in every frozen cell, and past 1 Tectonic grows
 *   its spire terrain. Terrain and biomes move together: flats, hills and mountains take 74, 15 and 10%.</li>
 *   <li>The temperature, {@value Reshaping#TEMPERATURE}, which Terralith's table, Tectonic's landform regions and
 *   Terralith's cold spikes all read: Tectonic's temperature noise scaled by {@link #TEMPERATURE_SCALE} plus the
 *   latitude, a {@code guworld:z_gradient} from {@link #NORTH_TEMPERATURE} at z {@code -}{@link #GRADIENT_EDGE} and
 *   north (minus z is north) to {@link #SOUTH_TEMPERATURE} at z {@link #GRADIENT_EDGE} and south. The north sits deep
 *   in the frozen band, the south across the cold and temperate bands; the scaled noise keeps both strips clean. Since
 *   the landform regions and the spikes read it too, the north also grows colder landforms and more ice spikes, an
 *   accepted trade.</li>
 * </ul>
 *
 * <p>The hooks:
 * <ul>
 *   <li>{@code ocean_offset}: Tectonic's continentalness is its folded continent noise plus this offset
 *   ({@link #OCEAN_OFFSET}), the Southern Border's [南疆] -0.1, which lifts the lowest ground inland of the coast
 *   band so no sea remains.</li>
 *   <li>{@code vertical_scale}: the stretch of the height above sea level, {@link #VERTICAL_SCALE}, under two
 *   thirds of Tectonic's 1.125, so the few peaks top out at about y 215.</li>
 *   <li>{@code dunes}: off ({@link #DUNES}); Tectonic raises dunes in hot, dry cells, which the gradient leaves almost
 *   unreached.</li>
 *   <li>{@code min_y}, {@code max_y}, {@code jungle_pillars}, {@code underground_rivers} with
 *   {@code underground_river_water}, and {@code lava_tunnels}: as in every region.</li>
 * </ul>
 * {@link ColumnCaches} wraps the ported column functions, both reshaped ones among them, so a noise chunk computes
 * each once per column.
 *
 * @author Alex
 * @version 1.0.0
 * @see NorthernPlains
 * @since 1.0.0
 */

public final class NorthernPlainsTerrain {

    private static final int GRADIENT_EDGE = 2048;
    private static final double NORTH_TEMPERATURE = -0.75;
    private static final double SOUTH_TEMPERATURE = -0.05;
    private static final double TEMPERATURE_SCALE = 0.55;
    private static final double EROSION_SCALE = 0.8;
    private static final double EROSION_OFFSET = 0.038;
    private static final double EROSION_CAP = 0.8;
    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 0.70;
    private static final double DUNES = 0.0;
    public static final Region.Hooks HOOKS = new Region.Hooks(DensityFunctions.constant(OCEAN_OFFSET),
            VERTICAL_SCALE, DUNES, NorthernPlainsTerrain::getReshaped);

    private NorthernPlainsTerrain() {}

    private static JsonElement getReshaped(String name, JsonElement function) {
        if (name.equals(Reshaping.TEMPERATURE)) {
            JsonObject latitude = new JsonObject();
            latitude.addProperty("type", GuWorld.id(ZGradient.ID).toString());
            latitude.addProperty("from_z", -GRADIENT_EDGE);
            latitude.addProperty("to_z", GRADIENT_EDGE);
            latitude.addProperty("from_value", NORTH_TEMPERATURE);
            latitude.addProperty("to_value", SOUTH_TEMPERATURE);
            JsonObject noise = function.getAsJsonObject();
            return Reshaping.getOperation("add", latitude,
                    Reshaping.getOperation("mul", TEMPERATURE_SCALE, noise.get("argument2")));
        }
        if (name.equals(Reshaping.EROSION_FOLDED)) {
            JsonElement stretched = Reshaping.getOperation("add", EROSION_OFFSET,
                    Reshaping.getOperation("mul", EROSION_SCALE, function));
            return Reshaping.getOperation("min", stretched, EROSION_CAP);
        }
        return function;
    }
}
