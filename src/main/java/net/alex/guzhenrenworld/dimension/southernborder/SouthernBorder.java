package net.alex.guzhenrenworld.dimension.southernborder;

import java.util.List;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * 南疆维度的域描述入口（{@link #REGION}），为中州（主世界）外四域之首。
 * <ul>
 *   <li>地形以群山峻岭为主，山峰最高约达 Y=300（{@link Terrain}），海平面为 63，水系河床铺设砾石夹黏土。</li>
 *   <li>南疆使用的移植群系与原版复刻群系均已收录在公共集合（{@link net.alex.guzhenrenworld.biome.Ports#BIOMES} 与
 *       {@link net.alex.guzhenrenworld.biome.Replicas#SHARED}）中，因此本域独有复刻与移植列表为空；专属地形钩子定义在
 *       {@link Terrain}，多噪声群系映射定义在 {@link BiomeLayout}。</li>
 * </ul>
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
