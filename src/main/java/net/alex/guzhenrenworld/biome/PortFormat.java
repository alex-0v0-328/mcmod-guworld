package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the ported Terralith and Tectonic files, written in Minecraft 1.21.1's data format, into the format this
 * version's codecs read, before {@link Ports} decodes them.
 *
 * <p>The files under {@code src/ports} are the same on every Minecraft version the mod targets; only this class
 * differs. On 1.20.1, {@link #adapt} rewrites a copy of each file three ways:
 * <ul>
 *   <li>An int or float provider whose type has more than one field ({@link #NESTED_PROVIDERS}) keeps those fields
 *   under {@code value}, where 1.21 writes them beside {@code type}. A height provider of the same type names
 *   vertical anchors, objects, so a provider is only nested when its fields are numbers or, for {@code clamped}, a
 *   provider {@code source}.</li>
 *   <li>A block 1.21 renamed takes its 1.20.1 id ({@link #RENAMED}), and a block tag 1.21 added becomes the list
 *   of blocks it holds ({@link #EXPANDED}), spliced into a list or standing in for a lone tag.</li>
 *   <li>What 1.20.1 does not have ({@link #ABSENT}) is dropped: the armadillo and the bogged from the spawn lists
 *   and spawn costs, and the medium diamond ore from the feature lists, which leaves those biomes vanilla 1.20.1's
 *   other two diamond ores.</li>
 * </ul>
 *
 * <p>⚠ A re-port that brings in another 1.21-only id or format fails {@code runData} on the decode, naming the file;
 * it then gets its rule here.
 *
 * @author Alex
 * @version 1.0.0
 * @see Ports
 * @since 1.0.0
 */

public final class PortFormat {

    private static final Set<String> NESTED_PROVIDERS = Set.of("uniform", "biased_to_bottom", "clamped",
            "clamped_normal", "trapezoid");
    private static final Map<String, String> RENAMED = Map.of("minecraft:short_grass", "minecraft:grass");
    private static final Map<String, List<String>> EXPANDED = Map.of("#minecraft:air",
            List.of("minecraft:air", "minecraft:void_air", "minecraft:cave_air"));
    private static final Set<String> ABSENT = Set.of("minecraft:armadillo", "minecraft:bogged",
            "minecraft:ore_diamond_medium");

    private PortFormat() {}

    public static JsonElement adapt(JsonElement json) {
        if (json.isJsonArray()) return adaptArray(json.getAsJsonArray());
        if (json.isJsonObject()) return adaptObject(json.getAsJsonObject());
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) return adaptString(json);
        return json;
    }

    private static JsonArray adaptArray(JsonArray array) {
        JsonArray kept = new JsonArray();
        for (JsonElement element : array) {
            if (isAbsent(element)) continue;
            JsonElement adapted = adapt(element);
            boolean expandedTag = adapted.isJsonArray() && element.isJsonPrimitive();
            if (expandedTag) {
                kept.addAll(adapted.getAsJsonArray());
            } else {
                kept.add(adapted);
            }
        }
        return kept;
    }

    private static JsonObject adaptObject(JsonObject object) {
        JsonObject adapted = new JsonObject();
        object.entrySet().forEach(entry -> {
            if (!ABSENT.contains(entry.getKey())) adapted.add(entry.getKey(), adapt(entry.getValue()));
        });
        return isNestedProvider(adapted) ? nest(adapted) : adapted;
    }

    private static JsonElement adaptString(JsonElement json) {
        String renamed = RENAMED.get(json.getAsString());
        if (renamed != null) return new JsonPrimitive(renamed);
        List<String> expanded = EXPANDED.get(json.getAsString());
        if (expanded == null) return json;
        JsonArray blocks = new JsonArray();
        expanded.forEach(blocks::add);
        return blocks;
    }

    private static boolean isAbsent(JsonElement element) {
        if (element.isJsonPrimitive()) return ABSENT.contains(element.getAsString());
        if (!element.isJsonObject() || !element.getAsJsonObject().has("type")) return false;
        JsonElement type = element.getAsJsonObject().get("type");
        return type.isJsonPrimitive() && ABSENT.contains(type.getAsString());
    }

    private static boolean isNestedProvider(JsonObject object) {
        if (!object.has("type") || object.has("value") || object.size() < 3) return false;
        JsonElement type = object.get("type");
        if (!type.isJsonPrimitive() || !NESTED_PROVIDERS.contains(getPath(type.getAsString()))) return false;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getKey().equals("type")) continue;
            boolean number = entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber();
            if (!number && !(entry.getKey().equals("source") && entry.getValue().isJsonObject())) return false;
        }
        return true;
    }

    private static JsonObject nest(JsonObject provider) {
        JsonObject value = new JsonObject();
        provider.entrySet().stream().filter(entry -> !entry.getKey().equals("type"))
                .forEach(entry -> value.add(entry.getKey(), entry.getValue()));
        JsonObject nested = new JsonObject();
        nested.add("type", provider.get("type"));
        nested.add("value", value);
        return nested;
    }

    private static String getPath(String id) {
        return id.substring(id.indexOf(':') + 1);
    }
}
