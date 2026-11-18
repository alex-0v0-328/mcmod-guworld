package net.alex.guzhenrenworld.dimension.westerndesert;

import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * 西漠维度的域描述入口（{@link #REGION}），为中州（主世界）外四域之一。
 * <ul>
 *   <li>海平面设为 Y=52（比主世界与 Tectonic 地形基准低 11 格），由于 Tectonic 地形高度由样条直接决定而不随海平面升降，
 *       原本位于 Y≈56 的河谷与两侧低地自然裸露为干涸河床；水系仅最宽的主干河道有水（{@code dry = true}），河床统一铺设
 *       沙子。</li>
 *   <li>作为有限边界域，其注册键、世界边界与数据包引导统一由 {@link Region} 体系驱动；专属地形钩子定义在
 *       {@link WesternDesertTerrain}，多噪声群系映射定义在 {@link WesternDesertLayout}，独有群系清单定义在
 *       {@link WesternDesertRoster}。</li>
 * </ul>
 */

public final class WesternDesert {

    private static final String NAME = "western_desert";
    public static final Region REGION = new Region(NAME, new Region.Title("Western Desert", "西漠"), 52, true,
            Waterways.Bed.SAND,
            new Region.Biomes(WesternDesertRoster.REPLICAS, WesternDesertRoster.PORTED, WesternDesertLayout.ROSTER,
                    WesternDesertLayout.LAND, WesternDesertRoster.NAMES, WesternDesertRoster.CROPS,
                    WesternDesertLayout.TABLE::getParameters),
            WesternDesertTerrain.HOOKS);

    private WesternDesert() {}
}
