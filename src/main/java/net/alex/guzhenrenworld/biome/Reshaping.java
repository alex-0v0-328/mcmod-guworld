package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * What a region needs to rewrite a ported density function [密度函数] in JSON before it registers: the names of the two
 * ported functions the regions reshape, {@link #TEMPERATURE}, which Terralith's table, Tectonic's landform regions and
 * its dunes all read, and {@link #EROSION_FOLDED}, the distance of Tectonic's erosion from zero, which sets both the
 * terrain splines and the erosion Terralith's table reads; and {@link #getOperation}, a two-argument vanilla operation
 * ({@code add}, {@code mul}, {@code min}) over numbers and functions.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class Reshaping {

    public static final String TEMPERATURE = "terrain/minecraft/overworld/temperature";
    public static final String EROSION_FOLDED = "terrain/tectonic/noise/continent/erosion_folded";

    private Reshaping() {}

    public static JsonObject getOperation(String type, double first, JsonElement second) {
        return getOperation(type, new JsonPrimitive(first), second);
    }

    public static JsonObject getOperation(String type, JsonElement first, double second) {
        return getOperation(type, first, new JsonPrimitive(second));
    }

    public static JsonObject getOperation(String type, JsonElement first, JsonElement second) {
        JsonObject operation = new JsonObject();
        operation.addProperty("type", "minecraft:" + type);
        operation.add("argument1", first);
        operation.add("argument2", second);
        return operation;
    }
}
