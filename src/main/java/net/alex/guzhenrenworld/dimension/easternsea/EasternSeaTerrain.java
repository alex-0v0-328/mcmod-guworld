package net.alex.guzhenrenworld.dimension.easternsea;

import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.XGradient;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * 东海维度的地形参数配置，定制本域的 Tectonic 主世界噪声路由副本。
 * <ul>
 *   <li>基于 Tectonic 与 Terralith 共存时的移植路由（{@link Ports#registerDensityFunctions}），不改写底层密度函数结构，
 *       仅通过 {@link #HOOKS} 填充 {@link RegionBootstraps} 注册的地形钩子。</li>
 *   <li>{@code ocean_offset} 钩子由两个沿 X 轴渐变的 {@link XGradient} 相加而成：从西侧边界（{@link #WEST_X}）的
 *       {@link #WEST_OCEAN_OFFSET} 线性过渡到 {@link #ISLAND_EDGE_X} 处的 {@link #ISLAND_OCEAN_OFFSET}，再向东至
 *       {@link #OPEN_SEA_X} 额外下沉 {@link #OPEN_SEA_DROP}，形成西侧群岛向东侧开阔海面过渡的地势。</li>
 *   <li>其余高度上下限与特性开关与四域保持一致，并通过 {@link ColumnCaches} 为水平列函数套上列缓存。</li>
 * </ul>
 */

public final class EasternSeaTerrain {

    private static final int WEST_X = (int) (-Region.BORDER_SIZE / 2);
    private static final int ISLAND_EDGE_X = 1365;
    private static final int OPEN_SEA_X = 2048;
    private static final double WEST_OCEAN_OFFSET = -0.8;
    private static final double ISLAND_OCEAN_OFFSET = -1.0;
    private static final double OPEN_SEA_DROP = -0.9;
    private static final double VERTICAL_SCALE = 0.9;
    private static final double DUNES = 0.0;
    public static final Region.Hooks HOOKS = new Region.Hooks(
            DensityFunctions.add(
                    new XGradient(WEST_X, ISLAND_EDGE_X, WEST_OCEAN_OFFSET, ISLAND_OCEAN_OFFSET),
                    new XGradient(ISLAND_EDGE_X, OPEN_SEA_X, 0.0, OPEN_SEA_DROP)),
            VERTICAL_SCALE, DUNES, Region.Hooks.AS_PORTED);

    private EasternSeaTerrain() {}
}
