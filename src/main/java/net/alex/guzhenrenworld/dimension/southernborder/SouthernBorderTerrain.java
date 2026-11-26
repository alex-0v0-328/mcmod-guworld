package net.alex.guzhenrenworld.dimension.southernborder;

import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * 南疆维度的地形参数配置，定制本域的 Tectonic 主世界噪声路由副本。
 * <ul>
 *   <li>基于 {@link Ports} 移植并烘焙默认配置的 Tectonic 路由，不改写底层密度函数，仅通过 {@link #HOOKS} 向
 *       {@link RegionBootstraps#registerDensityFunctions} 提供 {@link #OCEAN_OFFSET}、{@link #VERTICAL_SCALE} 并关闭沙
 *       丘（{@code dunes = 0}）。</li>
 *   <li>高度上下限取维度底与顶（{@link Region#MIN_Y}、{@link Region#TOP_Y}），三项高开销 Tectonic 特性开关由
 *       {@link RegionBootstraps} 统一关闭，水平列函数由 {@link ColumnCaches} 自动套上列缓存。</li>
 * </ul>
 */

public final class SouthernBorderTerrain {

    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 1.125;
    public static final Region.Hooks HOOKS = new Region.Hooks(DensityFunctions.constant(OCEAN_OFFSET), VERTICAL_SCALE,
            0.0, Region.Hooks.AS_PORTED);

    private SouthernBorderTerrain() {}
}
