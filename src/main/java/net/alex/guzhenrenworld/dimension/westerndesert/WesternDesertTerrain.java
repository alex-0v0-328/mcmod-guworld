package net.alex.guzhenrenworld.dimension.westerndesert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Reshaping;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * 西漠地形：本域那份 Tectonic 主世界路由（{@link Ports#registerDensityFunctions}）。
 * <ul>
 *   <li>温度 {@value Reshaping#TEMPERATURE}（{@link #getReshaped} 改写；Terralith 表、Tectonic 地貌区与沙丘都读）=
 *       Tectonic 温度噪声 + {@link #TEMPERATURE_OFFSET}：原本以温带为中心、热带几乎空，平移后约四分之三格子热、余暖、无
 *       冷格，表的沙漠与恶地占热格，沙丘长在那里。</li>
 *   <li>折叠侵蚀 {@value Reshaping#EROSION_FOLDED}（同样改写）是 Tectonic 侵蚀离零的距离，兼定地形样条与 Terralith 表读
 *       到的侵蚀，低即山地；低于 Tectonic 尖柱带时乘 {@link #EROSION_SCALE}、加 {@link #EROSION_OFFSET}、封顶
 *       {@link #EROSION_CAP}，山地与丘陵带收窄、平地带放宽而不溢进尖柱带；{@link #SPIRE_EROSION} 及以上保持原值，尖柱格
 *       （沙漠石柱）占比同 Tectonic。</li>
 *   <li>{@link ColumnCaches} 包住移植列函数（含改写的两个），噪声区块每列只算一次。</li>
 *   <li>钩子（{@link #HOOKS}，经 {@link RegionBootstraps} 注册）：{@code ocean_offset} 取南疆的 -0.1
 *       （{@link #OCEAN_OFFSET}），最低处抬到海岸带内侧，不留海；{@code vertical_scale} 取 {@link #VERTICAL_SCALE}，最
 *       高峰约 y 210，非 Tectonic 的 300。</li>
 *   <li>{@code dunes} 开（{@link #DUNES}）：Tectonic 沙丘长在其钻石地貌区平坦侵蚀带的热干格；其余钩子同四域，但地下河还
 *       会给本域要保持干的河谷底下含水层灌水。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see WesternDesert
 * @since 1.0.0
 */

public final class WesternDesertTerrain {

    private static final double TEMPERATURE_OFFSET = 0.6;
    private static final double EROSION_SCALE = 1.45;
    private static final double EROSION_OFFSET = 0.16;
    private static final double EROSION_CAP = 0.98;
    private static final double SPIRE_EROSION = 1.0;
    private static final double SPIRE_EROSION_TOP = 1.0E6;
    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 0.65;
    private static final double DUNES = 1.0;
    public static final Region.Hooks HOOKS = new Region.Hooks(DensityFunctions.constant(OCEAN_OFFSET),
            VERTICAL_SCALE, DUNES, WesternDesertTerrain::getReshaped);

    private WesternDesertTerrain() {}

    private static JsonElement getReshaped(String name, JsonElement function) {
        if (name.equals(Reshaping.TEMPERATURE)) return Reshaping.getOperation("add", TEMPERATURE_OFFSET, function);
        if (name.equals(Reshaping.EROSION_FOLDED)) {
            JsonElement stretched = Reshaping.getOperation("add", EROSION_OFFSET,
                    Reshaping.getOperation("mul", EROSION_SCALE, function));
            JsonObject spires = new JsonObject();
            spires.addProperty("type", "minecraft:range_choice");
            spires.add("input", function);
            spires.addProperty("min_inclusive", SPIRE_EROSION);
            spires.addProperty("max_exclusive", SPIRE_EROSION_TOP);
            spires.add("when_in_range", function);
            spires.add("when_out_of_range", Reshaping.getOperation("min", stretched, EROSION_CAP));
            return spires;
        }
        return function;
    }
}
