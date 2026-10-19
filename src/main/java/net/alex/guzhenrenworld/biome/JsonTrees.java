package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.Consumer;

/**
 * The one walk over a JSON tree that datagen patches share: {@link #forEachObject} hands every object of a tree to
 * a visitor, parent before children, so a visitor that edits an object's members sees its own edits on the way down.
 * Replicas add themselves to vanilla's surface rules this way ({@link Replicas#getSurfaceRules}), and the Southern
 * Border moves Terralith's top slide inside every ported density function.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class JsonTrees {

    private JsonTrees() {}

    public static void forEachObject(JsonElement element, Consumer<JsonObject> visitor) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> forEachObject(child, visitor));
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            visitor.accept(object);
            object.entrySet().forEach(entry -> forEachObject(entry.getValue(), visitor));
        }
    }
}
