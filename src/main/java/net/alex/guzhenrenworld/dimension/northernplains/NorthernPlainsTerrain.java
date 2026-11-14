package net.alex.guzhenrenworld.dimension.northernplains;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Reshaping;
import net.alex.guzhenrenworld.biome.ZGradient;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * 北原地形：本域的 Tectonic 主世界路由副本。
 *
 * <ul>
 *   <li>移植自 Tectonic 与 Terralith 同装时的路由（{@link Ports#registerDensityFunctions}）；改写两个函数
 *       （{@link #getReshaped}），其余经移植留下的钩子（{@link #HOOKS}、{@link RegionBootstraps}）。</li>
 *   <li>折叠侵蚀 {@value Reshaping#EROSION_FOLDED}：Tectonic 侵蚀离零的距离，定地形样条与 Terralith 表读的侵蚀，
 *       越低越山。乘 {@link #EROSION_SCALE} 加 {@link #EROSION_OFFSET}：山地带收窄到过零线附近，丘陵带略宽；
 *       上限 {@link #EROSION_CAP} 即 Terralith 低地段上沿，超过则表的下一段冰冻格全是冰川裂谷，超过 1 则 Tectonic
 *       长尖柱地形。</li>
 *   <li>温度 {@value Reshaping#TEMPERATURE}（Terralith 表、Tectonic 地貌区、Terralith 冰刺都读）：Tectonic 温度噪声乘
 *       {@link #TEMPERATURE_SCALE} 加纬度 {@code guworld:z_gradient}，z {@code -}{@link #GRADIENT_EDGE} 及以北（-z
 *       为北）取 {@link #NORTH_TEMPERATURE}，z {@link #GRADIENT_EDGE} 及以南取 {@link #SOUTH_TEMPERATURE}。</li>
 *   <li>{@link ColumnCaches} 包住移植的列函数（含两个改写函数），噪声区块每列只算一次；{@code min_y}、{@code max_y}、
 *       {@code jungle_pillars}、{@code underground_rivers} 与 {@code underground_river_water}、{@code lava_tunnels}
 *       同各域。</li>
 *   <li>{@code ocean_offset}（{@link #OCEAN_OFFSET}）、{@code vertical_scale}（{@link #VERTICAL_SCALE}）、{@code dunes}
 *       （{@link #DUNES}）的取值与温度、侵蚀的取舍见 wiki《北原#地形方法》。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see NorthernPlains
 * @since 1.0.0
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
