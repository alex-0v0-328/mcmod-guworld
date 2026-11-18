package net.alex.guzhenrenworld.dimension.easternsea;

import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * 东海维度的域描述入口（{@link #REGION}），为中州（主世界）外四域之一。
 * <ul>
 *   <li>全域为暖海群岛地貌，约 4/5 面积为海域，岛屿跨度从数十格至两公里不等，水系河床统一铺设沙子；通过
 *       {@code /guworld enter guworld:eastern_sea} 进入时会落在原点 {@code (0, 0)} 上方地表（原点为海域时落在海面）。
 *       </li>
 *   <li>作为有限边界域，其注册键、世界边界与数据包引导统一由 {@link Region} 体系驱动；专属地形钩子定义在
 *       {@link EasternSeaTerrain}，多噪声群系映射定义在 {@link EasternSeaLayout}，独有群系清单定义在
 *       {@link EasternSeaRoster}。</li>
 * </ul>
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
