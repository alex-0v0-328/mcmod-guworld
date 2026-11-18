package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * 各域注册地形密度函数前改写移植 JSON 的公共辅助类。提供两个常被各域重塑的移植函数路径常量：{@link #TEMPERATURE}（同时
 * 被Terralith 多噪声群系表、Tectonic 地貌分区与沙丘读取）与 {@link #EROSION_FOLDED}（Tectonic 折叠侵蚀度，决定地形样条
 * 走势与Terralith 群系表读取的侵蚀度），并通过 {@link #getOperation} 构造原版二元算术节点（如 {@code add}、{@code mul}
 * 、{@code min}）。
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
