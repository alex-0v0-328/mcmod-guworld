package net.alex.guzhenrenworld.dimension.southernborder;

import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * 南疆地形：Tectonic 与 Terralith 同装时的 Tectonic 主世界路由。
 * <ul>
 *   <li>经 {@link Ports} 移植并烘焙 Tectonic 默认配置，只经钩子 {@code guworld:terrain/hook/*} 改形，钩子由
 *       {@link RegionBootstraps#registerDensityFunctions} 随移植函数注册；函数与路由用本域前缀
 *       （{@link Ports#getRegionalKey}），钩子用 {@link Ports#getHookKey}。</li>
 *   <li>钩子取值与理由见 wiki《南疆#地形方法》：{@link #OCEAN_OFFSET}、{@link #VERTICAL_SCALE}、{@code dunes} 关，
 *       {@code min_y}、{@code max_y} 取维度底与顶（{@link Region#MIN_Y}、{@link Region#TOP_Y}）。</li>
 *   <li>三项 Tectonic 特性开关见 {@link RegionBootstraps}，四域全关；{@link ColumnCaches} 包住移植列函数，每列算一
 *       次。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SouthernBorder
 * @since 1.0.0
 */

public final class Terrain {

    private static final double OCEAN_OFFSET = -0.1;
    private static final double VERTICAL_SCALE = 1.125;
    public static final Region.Hooks HOOKS = new Region.Hooks(DensityFunctions.constant(OCEAN_OFFSET), VERTICAL_SCALE,
            0.0, Region.Hooks.AS_PORTED);

    private Terrain() {}
}
