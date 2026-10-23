package net.alex.guzhenrenworld.dimension.southernborder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Set;
import net.alex.guzhenrenworld.DatapackBootstrap;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * The Southern Border [南疆] terrain: Tectonic's overworld router as Tectonic runs it beside Terralith, ported
 * ({@link Ports}) with Tectonic's default config baked in, reshaped only through the hooks the port leaves in it,
 * {@code guworld:terrain/hook/*}, which {@link #registerDensityFunctions} registers beside the ported functions:
 * <ul>
 *   <li>{@code ocean_offset}: Tectonic's continentalness is its folded continent noise plus this offset, where
 *   Tectonic's -0.8 leaves a third of the world land. {@link #OCEAN_OFFSET} lifts the lowest ground, the noise's zero
 *   lines where Tectonic's seas run, just inland of the coast band, so the region has no ocean, beach or island: its
 *   water is the rivers Tectonic cuts where its ridge noise crosses zero, at about y 56 in every region but the
 *   mountains, and the lakes along those lowest lines.</li>
 *   <li>{@code vertical_scale}: the stretch of the height above sea level, {@link #VERTICAL_SCALE}, Tectonic's
 *   own.</li>
 *   <li>{@code min_y} and {@code max_y}: Tectonic's height limits, the dimension's floor and top
 *   ({@link SouthernBorder#MIN_Y}, {@link SouthernBorder#TOP_Y}). The offset tops out 17 blocks under the top and the
 *   terrain fades to air from 30 to 10 blocks under it, so the highest peaks stand at about y 300.</li>
 *   <li>{@code dunes}: none. Tectonic raises dunes in hot, dry cells, which the region maps to plains.</li>
 *   <li>{@code jungle_pillars}, {@code underground_rivers} with {@code underground_river_water}, and
 *   {@code lava_tunnels}: three Tectonic features, each switched by one constant here and off for now
 *   ({@link #JUNGLE_PILLARS}, {@link #UNDERGROUND_RIVERS}, {@link #LAVA_TUNNELS}). Jungle pillars are extruded
 *   discs about 11 blocks wide and up to 100 tall with flat tops and sheer sides, the regular pillar shape Alex turned
 *   down for the region's own landforms. Underground rivers carve water channels at y 32 to 72 under mountain
 *   valleys and flood the aquifers there, lava tunnels run along the floor; both read their noises at every cell
 *   corner or block of the column, about a quarter of the noise fill. A switch that is on points its hook back at the
 *   ported function, one that is off at zero, so an off feature costs nothing.</li>
 * </ul>
 * Everything else stays as Tectonic and Terralith ship it: Tectonic's macro scale (continents about four times
 * vanilla's, erosion and ridges twice), its climate-picked landform regions, slope-warped mountain ridges with
 * weathering cracks and valley rivers; Terralith's cold spikes and megacave noise.
 *
 * <p>{@link #getPorted} adds column caches that change no value, only how often one is computed. The ported functions
 * in {@link #COLUMN_CACHED} vary only across the map, yet the router reads them per block or per cell corner; each
 * gets a {@code flat_cache}, so a noise chunk computes it once per 4-block column, and every noise under them already
 * sits inside a {@code flat_cache}, so they read the same value anywhere in a column. A {@code flat_cache} only
 * serves its own chunk's grid, though: the aquifers' preliminary surface scan steps down columns of the neighboring
 * chunks 4 blocks at a time, and there a {@code flat_cache} recomputes its input at every step, Tectonic's whole map
 * chain of shifted noises and splines with it, a third of the noise fill. So every column cache, the ported ones
 * included, holds a {@code cache_2d}, which keeps the last column's value: the scan computes each column once. None
 * of them reads y.
 *
 * @author Alex
 * @version 1.0.0
 * @see SouthernBorder
 * @since 1.0.0
 */

public final class Terrain {

    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 1.125;
    private static final boolean JUNGLE_PILLARS = false;
    private static final boolean UNDERGROUND_RIVERS = false;
    private static final boolean LAVA_TUNNELS = false;
    private static final Set<String> COLUMN_CACHED = Set.of("terrain/minecraft/overworld/temperature",
            "terrain/minecraft/overworld/vegetation", "terrain/tectonic/noise/continent/erosion_folded",
            "terrain/tectonic/noise/continent/ridges_folded", "terrain/tectonic/region/diamond/dune/spline",
            "terrain/tectonic/terrain_spline/factor/continents", "terrain/tectonic/terrain_spline/factor/islands",
            "terrain/tectonic/underground_river/spline/continents",
            "terrain/tectonic/underground_river/spline/erosion", "terrain/terralith/overworld/spike/size_spline");

    private Terrain() {}

    public static void registerDensityFunctions(DatapackBootstrap<DensityFunction> context) {
        Ports.registerDensityFunctions(context, Terrain::getPorted);
        register(context, "ocean_offset", DensityFunctions.constant(OCEAN_OFFSET));
        register(context, "vertical_scale", DensityFunctions.constant(VERTICAL_SCALE));
        register(context, "min_y", DensityFunctions.constant(SouthernBorder.MIN_Y));
        register(context, "max_y", DensityFunctions.constant(SouthernBorder.TOP_Y));
        register(context, "dunes", DensityFunctions.zero());
        register(context, "jungle_pillars", DensityFunctions.constant(JUNGLE_PILLARS ? 1.0 : 0.0));
        registerSwitch(context, "underground_rivers", "tectonic/underground_river/total", UNDERGROUND_RIVERS);
        registerSwitch(context, "underground_river_water", "tectonic/underground_river/parameters",
                UNDERGROUND_RIVERS);
        registerSwitch(context, "lava_tunnels", "tectonic/lava_tunnel/total", LAVA_TUNNELS);
    }

    private static JsonElement getPorted(String name, JsonElement function) {
        if (COLUMN_CACHED.contains(name)) return getColumnCached(function);
        if (!isOfType(function, "flat_cache")) return function;
        JsonElement argument = function.getAsJsonObject().get("argument");
        return isOfType(argument, "cache_2d") ? function : getColumnCached(argument);
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

    private static void registerSwitch(DatapackBootstrap<DensityFunction> context, String hook, String ported,
            boolean on) {
        if (!on) {
            register(context, hook, DensityFunctions.zero());
            return;
        }
        Holder<DensityFunction> function = context.lookup(Registries.DENSITY_FUNCTION).getOrThrow(key(ported));
        register(context, hook, new DensityFunctions.HolderHolder(function));
    }

    private static void register(DatapackBootstrap<DensityFunction> context, String hook, DensityFunction function) {
        context.register(key("hook/" + hook), function);
    }

    private static ResourceKey<DensityFunction> key(String name) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, GuWorld.id("terrain/" + name));
    }
}
