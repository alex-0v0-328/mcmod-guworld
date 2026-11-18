package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * 移植密度函数（{@link Ports#registerDensityFunctions}）的列缓存补丁：不改变计算结果，仅降低水平列函数的求值频率。
 * <ul>
 *   <li>{@link #PORTED_COLUMNS} 列出的 10 个移植函数仅随水平坐标变化，但噪声路由会逐方块或逐网格角读取；补丁为其套上
 *       {@code flat_cache}，使每个噪声区块内每 4×4 水平格仅计算一次（其底层噪声本身已位于 {@code flat_cache} 内，列内读
 *       值不变）。</li>
 *   <li>原版 {@code flat_cache} 仅缓存当前正在生成的区块：含水层初步地表扫描会在相邻区块列上按每 4 格向下探测，每步都会
 *       重算输入及 Tectonic 整条移位噪声与样条链（约占噪声填充耗时的 1/3）；因此补丁会在列缓存（含移植自带的
 *       {@code flat_cache}）内层再套一层 {@code cache_2d} 记住上一列结果，确保跨区块扫描时每列也只计算一次。</li>
 *   <li>{@link #ofPorted} 为默认补丁入口；其中地下河的 2 条样条仅在启用地下河的域中存在，补丁遇到未注册的函数名会自动跳
 *       过。</li>
 *   <li>⚠ 列入缓存名单的函数严禁依赖 Y 坐标，因为 {@code cache_2d} 对同一水平列的任意 Y 坐标都会返回首次求值的结果。
 *       </li>
 * </ul>
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
