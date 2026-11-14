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
 *   <li>{@code ocean_offset}：大陆度为折叠大陆噪声加此偏移，Tectonic 默认 -0.8 只留三分之一陆地；{@link #OCEAN_OFFSET}
 *       把最低处（Tectonic 海所在的噪声零线）抬到海岸带内侧，无海洋、海滩、岛屿。</li>
 *   <li>水只有河湖：河在山脊噪声过零处，河床约 y 56，除山地外各地貌区都有；湖沿最低线分布。</li>
 *   <li>{@code vertical_scale}：海平面以上的高度拉伸，{@link #VERTICAL_SCALE} 取 Tectonic 默认；{@code dunes}：关，
 *       Tectonic 沙丘所在的热干格本域映射成平原。</li>
 *   <li>{@code min_y}、{@code max_y}：Tectonic 高度上下限，取维度底与顶（{@link Region#MIN_Y}、{@link Region#TOP_Y}）；
 *       偏移最高到顶下 17 格，顶下 30 至 10 格渐隐成空气，最高峰约 y 300。</li>
 *   <li>三项 Tectonic 特性开关见 {@link RegionBootstraps}，四域全关；{@link ColumnCaches} 包住移植列函数，每列算一
 *       次。</li>
 *   <li>其余照原样：Tectonic 宏观尺度（大陆约原版 4 倍，侵蚀与山脊 2 倍）、按气候选的地貌区、沿坡错开的山脊连同风化裂纹
 *       和河谷；Terralith 寒带尖刺与巨型洞穴噪声。</li>
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
