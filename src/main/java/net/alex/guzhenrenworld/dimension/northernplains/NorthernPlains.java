package net.alex.guzhenrenworld.dimension.northernplains;

import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * 北原维度的域描述入口（{@link #REGION}），为中州（主世界）外四域之一。
 * <ul>
 *   <li>地貌以开阔平坦的寒冷草原为主，呈现南草北雪的纬度过渡与局部冰封山峰，水系河床统一铺设砾石。</li>
 *   <li>作为有限边界域，其注册键、世界边界与数据包引导统一由 {@link Region} 体系驱动；专属地形钩子与纬度重塑定义在
 *       {@link NorthernPlainsTerrain}，多噪声群系映射定义在 {@link NorthernPlainsLayout}，独有群系清单定义在
 *       {@link NorthernPlainsRoster}。</li>
 * </ul>
 */

public final class NorthernPlains {

    private static final String NAME = "northern_plains";
    public static final Region REGION = new Region(NAME, new Region.Title("Northern Plains", "北原"), 63, false,
            Waterways.Bed.GRAVEL,
            new Region.Biomes(NorthernPlainsRoster.REPLICAS, NorthernPlainsRoster.PORTED, NorthernPlainsLayout.ROSTER,
                    NorthernPlainsLayout.LAND, NorthernPlainsRoster.NAMES, NorthernPlainsRoster.CROPS,
                    NorthernPlainsLayout.TABLE::getParameters),
            NorthernPlainsTerrain.HOOKS);

    private NorthernPlains() {}
}
