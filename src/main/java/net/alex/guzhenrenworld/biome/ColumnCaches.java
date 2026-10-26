package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * The column caches a region puts on the ported density functions ({@link Ports#registerDensityFunctions}), a patch
 * that changes no value, only how often one is computed. The named functions vary only across the map, yet the router
 * reads them per block or per cell corner; each gets a {@code flat_cache}, so a noise chunk computes it once per
 * 4-block column, and every noise under them already sits inside a {@code flat_cache}, so they read the same value
 * anywhere in a column. A {@code flat_cache} only serves its own chunk's grid, though: the aquifers' preliminary
 * surface scan steps down columns of the neighboring chunks 4 blocks at a time, and there a {@code flat_cache}
 * recomputes its input at every step, Tectonic's whole map chain of shifted noises and splines with it, a third of
 * the noise fill. So every column cache, the ported ones included, holds a {@code cache_2d}, which keeps the last
 * column's value: the scan computes each column once.
 *
 * <p>{@link #PORTED_COLUMNS} are the ten ported functions every region's copy caches this way, and
 * {@link #ofPorted} the patch that caches them.
 *
 * <p>⚠ A function named here must not read y: a {@code cache_2d} returns the first y's value for every y.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class ColumnCaches {

    public static final Set<String> PORTED_COLUMNS = Set.of("terrain/minecraft/overworld/temperature",
            "terrain/minecraft/overworld/vegetation", "terrain/tectonic/noise/continent/erosion_folded",
            "terrain/tectonic/noise/continent/ridges_folded", "terrain/tectonic/region/diamond/dune/spline",
            "terrain/tectonic/terrain_spline/factor/continents", "terrain/tectonic/terrain_spline/factor/islands",
            "terrain/tectonic/underground_river/spline/continents",
            "terrain/tectonic/underground_river/spline/erosion", "terrain/terralith/overworld/spike/size_spline");

    private ColumnCaches() {}

    public static BiFunction<String, JsonElement, JsonElement> ofPorted() {
        return of(PORTED_COLUMNS);
    }

    public static BiFunction<String, JsonElement, JsonElement> of(Set<String> columnFunctions) {
        return (name, function) -> {
            if (columnFunctions.contains(name)) return getColumnCached(function);
            if (!isOfType(function, "flat_cache")) return function;
            JsonElement argument = function.getAsJsonObject().get("argument");
            return isOfType(argument, "cache_2d") ? function : getColumnCached(argument);
        };
    }

    private static JsonObject getColumnCached(JsonElement function) {
        JsonObject column = new JsonObject();
        column.addProperty("type", "minecraft:cache_2d");
        column.add("argument", function);
        JsonObject grid = new JsonObject();
        grid.addProperty("type", "minecraft:flat_cache");
        grid.add("argument", column);
        return grid;
    }

    private static boolean isOfType(JsonElement function, String type) {
        if (!function.isJsonObject() || !function.getAsJsonObject().has("type")) return false;
        String name = function.getAsJsonObject().get("type").getAsString();
        return name.equals(type) || name.equals("minecraft:" + type);
    }
}
