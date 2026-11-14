package net.alex.guzhenrenworld.dimension.northernplains;

import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * 北原维度的入口 {@link #REGION}：中州（主世界）外四域之一。
 *
 * <ul>
 *   <li>寒冷草原，大体平坦开阔，南草北雪，少数冰封山峰；水系河床铺砾石。</li>
 *   <li>有限域，键、边界、数据包条目用共用的一套（{@link Region}）；地形钩子在 {@link NorthernPlainsTerrain}，群系表在
 *       {@link NorthernPlainsLayout}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see NorthernPlainsTerrain
 * @since 1.0.0
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
