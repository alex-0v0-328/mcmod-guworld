package net.alex.guzhenrenworld.dimension.southernborder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CubicSpline;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Noises;

/**
 * The Southern Border [南疆] terrain: Terralith's overworld router as ported ({@link Ports}), reshaped only through
 * the hooks the port leaves in it, {@code guworld:terralith/hook/*}, which {@link #registerDensityFunctions} registers
 * beside the ported functions:
 * <ul>
 *   <li>{@code continents}: vanilla's continentalness noise stretched {@link #CONTINENT_STRETCH} times wider, then
 *   remapped onto Terralith's inland bands ({@link #INLAND}), so the region has no ocean or beach; its water is the
 *   rivers, where weirdness folds to valleys, and the lakes aquifers fill below sea level.</li>
 *   <li>{@code erosion}: Terralith's erosion remapped band by band ({@link #EROSION_BANDS}, from its measured spread:
 *   tenth -0.41, median 0.10, ninth 0.47): the lowest 62% fall in its mountain bands, the highest 15% in its cliff
 *   and spire bands, and the hill and flat bands share the rest, so mountains, landforms included, take about half
 *   the land.</li>
 *   <li>{@code offset} and {@code depth}: Terralith's offset with everything above sea level raised
 *   {@link #VERTICAL_SCALE} times, the vertical stretch that, with the wider mountain bands, puts the highest peak at
 *   about y 275 (seed 12345 over the whole region); {@code depth} is vanilla's, the offset over the -64..320
 *   gradient.</li>
 *   <li>{@code dunes}: none. The other extra terrain, Terralith's arches and cold spikes, stays.</li>
 * </ul>
 * Everything else stays Terralith's, its 2× wider erosion regions included, so the three axes stretch only a little:
 * continents 1.5×, erosion 2× (Terralith's own), height above sea level 1.02×. Measured over the region: heights p50 y
 * 99, p90 y 155, 6% of columns under water, all of it river or lake.
 *
 * <p>The ported router fades every column to air from y {@link #TOP_SLIDE_START} up to {@link #TOP_SLIDE_END};
 * {@link #getPatched} moves Terralith's slide, 250..292, there, so the raised peaks keep their tops.
 *
 * <p>⚠ These numbers are a first measured pass for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see SouthernBorder
 * @since 1.0.0
 */

public final class Terrain {

    private static final double CONTINENT_STRETCH = 1.5;
    private static final double CONTINENT_SCALE = 0.25;
    private static final float[][] INLAND = {
            { -1.2F, -0.455F, -0.19F, -0.11F, 0.03F, 0.3F, 1.0F },
            { -0.1F, 0.0F, 0.1F, 0.25F, 0.4F, 0.6F, 1.0F } };
    private static final float[][] EROSION_BANDS = {
            { -1.0F, -0.409F, 0.19F, 0.39F, 0.52F, 1.0F },
            { -1.0F, -0.78F, -0.375F, 0.4F, 0.565F, 1.0F } };
    private static final double VERTICAL_SCALE = 1.02;
    private static final double SEA_LEVEL_OFFSET = 0.50375;
    private static final int TOP_SLIDE_START = 280;
    private static final int TOP_SLIDE_END = 330;
    private static final int TERRALITH_SLIDE_START = 250;
    private static final int TERRALITH_SLIDE_END = 292;

    private Terrain() {}

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context) {
        Ports.registerDensityFunctions(context, Terrain::getPatched);
        HolderGetter<DensityFunction> functions = context.lookup(Registries.DENSITY_FUNCTION);
        DensityFunction continents = DensityFunctions.shiftedNoise2d(
                getFunction(functions, ResourceLocation.withDefaultNamespace("shift_x")),
                getFunction(functions, ResourceLocation.withDefaultNamespace("shift_z")),
                CONTINENT_SCALE / CONTINENT_STRETCH,
                context.lookup(Registries.NOISE).getOrThrow(Noises.CONTINENTALNESS));
        register(context, "continents", DensityFunctions.flatCache(getRemapped(Holder.direct(continents), INLAND)));
        register(context, "erosion", getRemapped(functions.getOrThrow(
                ResourceKey.create(Registries.DENSITY_FUNCTION, GuWorld.id("terralith/overworld/erosion"))), EROSION_BANDS));
        DensityFunction offset = getFunction(functions, GuWorld.id("terralith/overworld/offset"));
        DensityFunction aboveSea = DensityFunctions.add(offset, DensityFunctions.constant(SEA_LEVEL_OFFSET)).clamp(
                0.0, 1000.0);
        register(context, "offset", DensityFunctions.add(offset,
                DensityFunctions.mul(DensityFunctions.constant(VERTICAL_SCALE - 1.0), aboveSea)));
        register(context, "depth", DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5, -1.5),
                getFunction(functions, GuWorld.id("terralith/hook/offset"))));
        register(context, "dunes", DensityFunctions.constant(-1.0));
    }

    private static JsonElement getPatched(JsonElement function) {
        if (function.isJsonArray()) {
            function.getAsJsonArray().forEach(Terrain::getPatched);
        } else if (function.isJsonObject()) {
            JsonObject object = function.getAsJsonObject();
            if (isTerralithSlide(object)) {
                object.add("from_y", new JsonPrimitive(TOP_SLIDE_START));
                object.add("to_y", new JsonPrimitive(TOP_SLIDE_END));
            }
            object.entrySet().forEach(entry -> getPatched(entry.getValue()));
        }
        return function;
    }

    private static boolean isTerralithSlide(JsonObject object) {
        return "minecraft:y_clamped_gradient".equals(getString(object, "type"))
                && object.has("from_y") && object.get("from_y").getAsInt() == TERRALITH_SLIDE_START
                && object.has("to_y") && object.get("to_y").getAsInt() == TERRALITH_SLIDE_END;
    }

    private static String getString(JsonObject object, String member) {
        return object.has(member) && object.get(member).isJsonPrimitive() ? object.get(member).getAsString() : null;
    }

    private static DensityFunction getRemapped(Holder<DensityFunction> input, float[][] points) {
        CubicSpline.Builder<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> builder =
                CubicSpline.builder(new DensityFunctions.Spline.Coordinate(input));
        float[] from = points[0];
        float[] to = points[1];
        for (int i = 0; i < from.length; i++) {
            float before = i > 0 ? (to[i] - to[i - 1]) / (from[i] - from[i - 1]) : Float.NaN;
            float after = i < from.length - 1 ? (to[i + 1] - to[i]) / (from[i + 1] - from[i]) : Float.NaN;
            float slope = Float.isNaN(before) ? after : Float.isNaN(after) ? before : (before + after) / 2.0F;
            builder.addPoint(from[i], to[i], slope);
        }
        return DensityFunctions.spline(builder.build());
    }

    private static DensityFunction getFunction(HolderGetter<DensityFunction> functions, ResourceLocation id) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(
                ResourceKey.create(Registries.DENSITY_FUNCTION, id)));
    }

    private static void register(BootstrapContext<DensityFunction> context, String hook, DensityFunction function) {
        context.register(ResourceKey.create(Registries.DENSITY_FUNCTION, GuWorld.id("terralith/hook/" + hook)),
                function);
    }
}
