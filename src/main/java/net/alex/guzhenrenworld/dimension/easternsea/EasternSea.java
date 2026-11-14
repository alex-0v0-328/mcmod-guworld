package net.alex.guzhenrenworld.dimension.easternsea;

import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * 东海维度的入口 {@link #REGION}：中州（主世界）外四域之一。
 *
 * <ul>
 *   <li>暖海群岛，约 4/5 是水，岛宽两公里到几十格；水系河床铺沙；{@code /guworld enter guworld:eastern_sea} 落在 0, 0
 *       上方地表，原点是水即落海面。</li>
 *   <li>有限域，键、边界、数据包条目用共用的一套（{@link Region}）；地形钩子在 {@link EasternSeaTerrain}，群系表在
 *       {@link EasternSeaLayout}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see EasternSeaTerrain
 * @since 1.0.0
 */

public final class EasternSea {

    private static final String NAME = "eastern_sea";
    public static final Region REGION = new Region(NAME, new Region.Title("Eastern Sea", "东海"), 63, false,
            Waterways.Bed.SAND,
            new Region.Biomes(EasternSeaRoster.REPLICAS, EasternSeaRoster.PORTED, EasternSeaLayout.ROSTER,
                    EasternSeaLayout.LAND, EasternSeaRoster.NAMES, EasternSeaRoster.CROPS,
                    EasternSeaLayout.TABLE::getParameters),
            EasternSeaTerrain.HOOKS);

    private EasternSea() {}
}
