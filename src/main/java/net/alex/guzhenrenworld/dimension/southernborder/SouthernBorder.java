package net.alex.guzhenrenworld.dimension.southernborder;

import java.util.List;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * 南疆维度的 {@link #REGION}：中州（主世界）外四域之首。
 * <ul>
 *   <li>山峰最高约 y 300（{@link Terrain}），比建筑上限低约 20 格；海平面 63；水系河床为砾石夹黏土。</li>
 *   <li>只放四域共用的复刻，无本域名单；区域有限，键、边界、数据包条目用共用的（{@link Region}）；地形钩子见
 *       {@link Terrain}，群系表见 {@link BiomeLayout}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Terrain
 * @since 1.0.0
 */

public final class SouthernBorder {

    private static final String NAME = "southern_border";
    public static final Region REGION = new Region(NAME, new Region.Title("Southern Border", "南疆"), 63, false,
            Waterways.Bed.GRAVEL_CLAY,
            new Region.Biomes(List.of(), List.of(), BiomeLayout.ROSTER, BiomeLayout.LAND, List.of(),
                    BiomeLayout.CROPS, BiomeLayout.TABLE::getParameters),
            Terrain.HOOKS);

    private SouthernBorder() {}
}
