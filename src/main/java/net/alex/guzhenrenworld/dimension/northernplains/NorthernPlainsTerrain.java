package net.alex.guzhenrenworld.dimension.northernplains;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.function.BiFunction;
import net.alex.guzhenrenworld.DatapackBootstrap;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.TerrainHooks;
import net.alex.guzhenrenworld.biome.ZGradient;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The Northern Plains [北原] terrain: the region's copy of Tectonic's overworld router as Tectonic runs it beside
 * Terralith ({@link Ports#registerDensityFunctions}), reshaped in two of its functions ({@link #getReshaped}) and
 * through the hooks the port leaves in it, which {@link #registerDensityFunctions} fills ({@link TerrainHooks}).
 *
 * <p>The two reshaped functions:
 * <ul>
 *   <li>The folded erosion, {@value #EROSION_FOLDED}, the distance of Tectonic's erosion from zero, which sets both
 *   the terrain splines and the erosion Terralith's table reads: low means mountains. The region stretches it by
 *   {@link #EROSION_SCALE} and lifts it by {@link #EROSION_OFFSET}, so the mountain and hill bands narrow to the
 *   lines where the erosion crosses zero and the lowland bands widen, and caps it at {@link #EROSION_CAP}, the top of
 *   Terralith's lowland band: past it the table's next band holds the glacial chasm in every frozen cell, which took
 *   a fifth of the region uncapped, and past 1 Tectonic grows its spire terrain. Terrain and biomes move together,
 *   flats, hills and mountains from 47, 37 and 15% to 65, 26 and 7%.</li>
 *   <li>The temperature, {@value #TEMPERATURE}, which Terralith's table, Tectonic's landform regions and Terralith's
 *   cold spikes all read: Tectonic's temperature noise scaled by {@link #TEMPERATURE_SCALE} plus the latitude, a
 *   {@code guworld:z_gradient} from {@link #NORTH_TEMPERATURE} at z {@code -}{@link #GRADIENT_EDGE} and north (minus
 *   z is north) to {@link #SOUTH_TEMPERATURE} at z {@link #GRADIENT_EDGE} and south. The north sits deep in the
 *   frozen band, the south across the cold and temperate bands; the scaled noise keeps both strips clean. Since the
 *   landform regions and the spikes read it too, the north also grows colder landforms and more ice spikes, an
 *   accepted trade.</li>
 * </ul>
 *
 * <p>The hooks:
 * <ul>
 *   <li>{@code ocean_offset}: Tectonic's continentalness is its folded continent noise plus this offset
 *   ({@link #OCEAN_OFFSET}), the Southern Border's [南疆] -0.1, which lifts the lowest ground inland of the coast
 *   band so no sea remains.</li>
 *   <li>{@code vertical_scale}: the stretch of the height above sea level, {@link #VERTICAL_SCALE}, two thirds of
 *   Tectonic's 1.125, so the few peaks top out at about y 230 instead of 290.</li>
 *   <li>{@code min_y} and {@code max_y}: Tectonic's height limits, the dimension's floor and top.</li>
 *   <li>{@code dunes}: off; Tectonic raises dunes in hot, dry cells, which the gradient leaves almost unreached.</li>
 *   <li>{@code jungle_pillars}, {@code underground_rivers} with {@code underground_river_water}, and
 *   {@code lava_tunnels}: off, as in the Southern Border.</li>
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

    private static final String TEMPERATURE = "terrain/minecraft/overworld/temperature";
    private static final String EROSION_FOLDED = "terrain/tectonic/noise/continent/erosion_folded";
    private static final int GRADIENT_EDGE = 2048;
    private static final double NORTH_TEMPERATURE = -0.75;
    private static final double SOUTH_TEMPERATURE = -0.05;
    private static final double TEMPERATURE_SCALE = 0.55;
    private static final double EROSION_SCALE = 1.2;
    private static final double EROSION_OFFSET = 0.01;
    private static final double EROSION_CAP = 0.8;
    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 0.75;
    private static final boolean JUNGLE_PILLARS = false;
    private static final boolean UNDERGROUND_RIVERS = false;
    private static final boolean LAVA_TUNNELS = false;

    private NorthernPlainsTerrain() {}

    public static void registerDensityFunctions(DatapackBootstrap<DensityFunction> context) {
        String region = NorthernPlains.NAME;
        BiFunction<String, JsonElement, JsonElement> columnCaches = ColumnCaches.ofPorted();
        Ports.registerDensityFunctions(context, region,
                (name, function) -> columnCaches.apply(name, getReshaped(name, function)));
        TerrainHooks.constant(context, region, "ocean_offset", OCEAN_OFFSET);
        TerrainHooks.constant(context, region, "vertical_scale", VERTICAL_SCALE);
        TerrainHooks.constant(context, region, "min_y", NorthernPlains.MIN_Y);
        TerrainHooks.constant(context, region, "max_y", NorthernPlains.TOP_Y);
        TerrainHooks.constant(context, region, "dunes", 0.0);
        TerrainHooks.constant(context, region, "jungle_pillars", JUNGLE_PILLARS ? 1.0 : 0.0);
        TerrainHooks.switched(context, region, "underground_rivers", "tectonic/underground_river/total",
                UNDERGROUND_RIVERS);
        TerrainHooks.switched(context, region, "underground_river_water", "tectonic/underground_river/parameters",
                UNDERGROUND_RIVERS);
        TerrainHooks.switched(context, region, "lava_tunnels", "tectonic/lava_tunnel/total", LAVA_TUNNELS);
    }

    private static JsonElement getReshaped(String name, JsonElement function) {
        if (name.equals(TEMPERATURE)) {
            JsonObject latitude = new JsonObject();
            latitude.addProperty("type", GuWorld.id(ZGradient.ID).toString());
            latitude.addProperty("from_z", -GRADIENT_EDGE);
            latitude.addProperty("to_z", GRADIENT_EDGE);
            latitude.addProperty("from_value", NORTH_TEMPERATURE);
            latitude.addProperty("to_value", SOUTH_TEMPERATURE);
            JsonObject noise = function.getAsJsonObject();
            return getOperation("add", latitude, getOperation("mul", TEMPERATURE_SCALE, noise.get("argument2")));
        }
        if (name.equals(EROSION_FOLDED)) {
            JsonElement stretched = getOperation("add", EROSION_OFFSET, getOperation("mul", EROSION_SCALE, function));
            return getOperation("min", stretched, EROSION_CAP);
        }
        return function;
    }

    private static JsonObject getOperation(String type, Object first, Object second) {
        JsonObject operation = new JsonObject();
        operation.addProperty("type", "minecraft:" + type);
        operation.add("argument1", toJson(first));
        operation.add("argument2", toJson(second));
        return operation;
    }

    private static JsonElement toJson(Object argument) {
        return argument instanceof Double value ? new JsonPrimitive(value) : (JsonElement) argument;
    }
}
