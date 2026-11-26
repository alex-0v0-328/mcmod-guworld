package net.alex.guzhenrenworld.dimension.westerndesert;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Reshaping;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * 西漠维度的地形参数配置，定制本域的 Tectonic 主世界噪声路由副本。
 * <ul>
 *   <li>基于 Tectonic 与 Terralith 共存时的移植路由（{@link Ports#registerDensityFunctions}），除填充 {@link #HOOKS} 地
 *       形钩子外，还通过 {@link #getReshaped} 重塑温度与折叠侵蚀度两个底层密度函数。</li>
 *   <li>温度函数（{@value Reshaping#TEMPERATURE}）在原有噪声基础上整体上移 {@link #TEMPERATURE_OFFSET}，使多噪声群系
 *       表、Tectonic 地貌分区与沙丘均采样到干热区间。</li>
 *   <li>折叠侵蚀度（{@value Reshaping#EROSION_FOLDED}）在低于尖柱带阈值（{@link #SPIRE_EROSION}）时先乘以
 *       {@link #EROSION_SCALE} 再加上 {@link #EROSION_OFFSET} 并以 {@link #EROSION_CAP} 封顶，从而收窄山地与丘陵带、展
 *       宽平原带且不溢入尖柱区间；达到 {@link #SPIRE_EROSION} 及以上的区间保持原值，使沙漠石柱占比与原 Tectonic 一致。
 *       </li>
 *   <li>开启沙丘开关（{@link #DUNES}），使 Tectonic 在钻石地貌区的平坦干热格生成沙丘；重塑后的函数与其余水平列函数均由
 *       {@link ColumnCaches} 自动套上列缓存。</li>
 * </ul>
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
