package net.alex.guzhenrenworld.dimension.westerndesert;

import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * 西漠维度的 {@link #REGION}：中州（主世界）外四域之一。
 * <ul>
 *   <li>热旱少水：多为平沙与沙丘，余为台地与峡谷，山峰最高约 y 210（{@link WesternDesertTerrain}）。</li>
 *   <li>海平面比主世界与地形基准 63 低 11 格；Tectonic 按自身样条，不随海平面变：约 y 56 的河及两旁低地成干河床。</li>
 *   <li>水只在含水层露头处（约 2% 的列）与绿洲（湖为 Terralith 地物，任何高度的洼地都填）；水系除最宽河段外为干沟，铺沙
 *       床。</li>
 *   <li>区域有限，键、边界、数据包条目用共用的（{@link Region}）；地形钩子见 {@link WesternDesertTerrain}，群系表见
 *       {@link WesternDesertLayout}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see WesternDesertTerrain
 * @since 1.0.0
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
