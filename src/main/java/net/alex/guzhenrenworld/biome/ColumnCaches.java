package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * 移植密度函数（{@link Ports#registerDensityFunctions}）的列缓存补丁：不改值，只降计算频率。
 * <ul>
 *   <li>点名函数只随水平位置变，路由却逐方块或逐格角读；各套 {@code flat_cache}，噪声区块每 4 格一列只算一次，其下噪声
 *       已在 {@code flat_cache} 内，列内读值相同。</li>
 *   <li>{@code flat_cache} 只管本区块：含水层初步地表扫描在邻区块列上每 4 格下行一步，每步重算输入及 Tectonic 整条移位
 *       噪声与样条链，占噪声填充 1/3；故列缓存（含移植原有的）内套 {@code cache_2d} 记上一列，每列算一次。</li>
 *   <li>{@link #PORTED_COLUMNS}：域副本这样缓存的 10 个移植函数，{@link #ofPorted} 为其补丁；其中地下河的 2 条样条仅在
 *       域开该功能时存在，补丁遇不到的名字不管。</li>
 *   <li>⚠ 点名函数不得读 y：{@code cache_2d} 对任何 y 都返回首个 y 的值。</li>
 * </ul>
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
