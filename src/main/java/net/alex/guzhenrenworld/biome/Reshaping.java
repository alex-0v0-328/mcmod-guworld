package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * 域注册前改写移植密度函数 JSON 的共用部件。被改写的两个移植函数：{@link #TEMPERATURE}，Terralith 群系表、Tectonic
 * 地貌区与沙丘都读；{@link #EROSION_FOLDED}，Tectonic 侵蚀离零的距离，决定地形样条与 Terralith 表读到的侵蚀。
 * {@link #getOperation}：原版二元运算（{@code add}、{@code mul}、{@code min}），参数为数或函数。
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
