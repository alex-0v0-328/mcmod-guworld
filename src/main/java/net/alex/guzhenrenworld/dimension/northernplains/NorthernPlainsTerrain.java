package net.alex.guzhenrenworld.dimension.northernplains;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Reshaping;
import net.alex.guzhenrenworld.biome.ZGradient;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * 北原维度的地形参数配置，定制本域的 Tectonic 主世界噪声路由副本。
 * <ul>
 *   <li>基于 Tectonic 与 Terralith 共存时的移植路由（{@link Ports#registerDensityFunctions}），除填充 {@link #HOOKS} 地
 *       形钩子外，还通过 {@link #getReshaped} 重塑温度与折叠侵蚀度两个底层密度函数。</li>
 *   <li>折叠侵蚀度（{@value Reshaping#EROSION_FOLDED}，值越低地势越陡峭）先乘以 {@link #EROSION_SCALE} 再加上
 *       {@link #EROSION_OFFSET}，将山地带收窄到过零线附近并略微展宽丘陵带，最后以 {@link #EROSION_CAP} 封顶以防大面积落
 *       入冰川裂谷或触发尖柱地形。</li>
 *   <li>温度函数（{@value Reshaping#TEMPERATURE}）将原有噪声缩放 {@link #TEMPERATURE_SCALE} 后叠加沿 Z 轴变化的
 *       {@link ZGradient}，使 Z ≤ {@code -}{@link #GRADIENT_EDGE}（北侧）基准温度为 {@link #NORTH_TEMPERATURE}、Z ≥
 *       {@link #GRADIENT_EDGE}（南侧）基准温度为 {@link #SOUTH_TEMPERATURE}。</li>
 *   <li>重塑后的函数与其余水平列函数均由 {@link ColumnCaches} 自动套上列缓存，确保每个水平列仅求值一次。</li>
 * </ul>
 */

public final class NorthernPlainsTerrain {

    private static final int GRADIENT_EDGE = 2048;
    private static final double NORTH_TEMPERATURE = -0.75;
    private static final double SOUTH_TEMPERATURE = -0.05;
    private static final double TEMPERATURE_SCALE = 0.55;
    private static final double EROSION_SCALE = 0.8;
    private static final double EROSION_OFFSET = 0.038;
    private static final double EROSION_CAP = 0.8;
    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 0.70;
    private static final double DUNES = 0.0;
    public static final Region.Hooks HOOKS = new Region.Hooks(DensityFunctions.constant(OCEAN_OFFSET),
            VERTICAL_SCALE, DUNES, NorthernPlainsTerrain::getReshaped);

    private NorthernPlainsTerrain() {}

    private static JsonElement getReshaped(String name, JsonElement function) {
        if (name.equals(Reshaping.TEMPERATURE)) {
            JsonObject latitude = new JsonObject();
            latitude.addProperty("type", GuWorld.id(ZGradient.ID).toString());
            latitude.addProperty("from_z", -GRADIENT_EDGE);
            latitude.addProperty("to_z", GRADIENT_EDGE);
            latitude.addProperty("from_value", NORTH_TEMPERATURE);
            latitude.addProperty("to_value", SOUTH_TEMPERATURE);
            JsonObject noise = function.getAsJsonObject();
            return Reshaping.getOperation("add", latitude,
                    Reshaping.getOperation("mul", TEMPERATURE_SCALE, noise.get("argument2")));
        }
        if (name.equals(Reshaping.EROSION_FOLDED)) {
            JsonElement stretched = Reshaping.getOperation("add", EROSION_OFFSET,
                    Reshaping.getOperation("mul", EROSION_SCALE, function));
            return Reshaping.getOperation("min", stretched, EROSION_CAP);
        }
        return function;
    }
}
