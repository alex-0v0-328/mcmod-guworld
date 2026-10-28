package net.alex.guzhenrenworld.dimension.westerndesert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.function.BiFunction;
import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.TerrainHooks;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The Western Desert [西漠] terrain: the region's copy of Tectonic's overworld router as Tectonic runs it beside
 * Terralith ({@link Ports#registerDensityFunctions}), with two of its functions reshaped and the hooks the port
 * leaves in it filled ({@link TerrainHooks}).
 *
 * <p>The two reshaped functions:
 * <ul>
 *   <li>The temperature, {@value #TEMPERATURE}, which Terralith's table, Tectonic's landform regions and its dunes
 *   all read: Tectonic's temperature noise plus {@link #TEMPERATURE_OFFSET}. Tectonic's own centers on the temperate
 *   band and leaves the hot band nearly empty; shifted, about half the region is hot and the rest warm, with a
 *   temperate tenth and no cold cell, so the table's deserts and badlands take the hot half and Tectonic's dunes
 *   grow there.</li>
 *   <li>The folded erosion, {@value #EROSION_FOLDED}, the distance of Tectonic's erosion from zero, which sets both
 *   the terrain splines and the erosion Terralith's table reads: low means mountains. Below Tectonic's spire band it
 *   is stretched by {@link #EROSION_SCALE}, lifted by {@link #EROSION_OFFSET} and capped at {@link #EROSION_CAP},
 *   so the mountain and hill bands narrow and the flats widen without spilling into the spire band; from
 *   {@link #SPIRE_EROSION} up it stays as Tectonic computes it, so the spire cells, the desert spires, keep the
 *   share they have in Tectonic.</li>
 * </ul>
 *
 * <p>The hooks:
 * <ul>
 *   <li>{@code ocean_offset}: Tectonic's continentalness is its folded continent noise plus this offset
 *   ({@link #OCEAN_OFFSET}), the Southern Border's [南疆] -0.1, which lifts the lowest ground inland of the coast
 *   band so no sea remains.</li>
 *   <li>{@code vertical_scale}: the stretch of the height above sea level, {@link #VERTICAL_SCALE}, so the highest
 *   peaks stand at about y 210 instead of Tectonic's 300.</li>
 *   <li>{@code min_y} and {@code max_y}: Tectonic's height limits, the dimension's floor and top.</li>
 *   <li>{@code dunes}: Tectonic's dunes, which rise in hot, dry cells of its diamond landform region on the flat
 *   erosion band, on ({@link #DUNES}).</li>
 *   <li>{@code jungle_pillars}, {@code underground_rivers} with {@code underground_river_water}, and
 *   {@code lava_tunnels}: three Tectonic features, off as in the Southern Border; the underground rivers would also
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

    private static final String TEMPERATURE = "terrain/minecraft/overworld/temperature";
    private static final String EROSION_FOLDED = "terrain/tectonic/noise/continent/erosion_folded";
    private static final double TEMPERATURE_OFFSET = 0.6;
    private static final double EROSION_SCALE = 1.45;
    private static final double EROSION_OFFSET = 0.04;
    private static final double EROSION_CAP = 0.98;
    private static final double SPIRE_EROSION = 1.0;
    private static final double SPIRE_EROSION_TOP = 1.0E6;
    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 0.65;
    private static final double DUNES = 1.0;
    private static final boolean JUNGLE_PILLARS = false;
    private static final boolean UNDERGROUND_RIVERS = false;
    private static final boolean LAVA_TUNNELS = false;

    private WesternDesertTerrain() {}

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context) {
        String region = WesternDesert.NAME;
        BiFunction<String, JsonElement, JsonElement> columnCaches = ColumnCaches.ofPorted();
        Ports.registerDensityFunctions(context, region,
                (name, function) -> columnCaches.apply(name, getReshaped(name, function)));
        TerrainHooks.constant(context, region, "ocean_offset", OCEAN_OFFSET);
        TerrainHooks.constant(context, region, "vertical_scale", VERTICAL_SCALE);
        TerrainHooks.constant(context, region, "min_y", WesternDesert.MIN_Y);
        TerrainHooks.constant(context, region, "max_y", WesternDesert.TOP_Y);
        TerrainHooks.constant(context, region, "dunes", DUNES);
        TerrainHooks.constant(context, region, "jungle_pillars", JUNGLE_PILLARS ? 1.0 : 0.0);
        TerrainHooks.switched(context, region, "underground_rivers", "tectonic/underground_river/total",
                UNDERGROUND_RIVERS);
        TerrainHooks.switched(context, region, "underground_river_water", "tectonic/underground_river/parameters",
                UNDERGROUND_RIVERS);
        TerrainHooks.switched(context, region, "lava_tunnels", "tectonic/lava_tunnel/total", LAVA_TUNNELS);
    }

    private static JsonElement getReshaped(String name, JsonElement function) {
        if (name.equals(TEMPERATURE)) return getOperation("add", TEMPERATURE_OFFSET, function);
        if (name.equals(EROSION_FOLDED)) {
            JsonElement stretched = getOperation("add", EROSION_OFFSET, getOperation("mul", EROSION_SCALE, function));
            JsonObject spires = new JsonObject();
            spires.addProperty("type", "minecraft:range_choice");
            spires.add("input", function);
            spires.addProperty("min_inclusive", SPIRE_EROSION);
            spires.addProperty("max_exclusive", SPIRE_EROSION_TOP);
            spires.add("when_in_range", function);
            spires.add("when_out_of_range", getOperation("min", stretched, EROSION_CAP));
            return spires;
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
