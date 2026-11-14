package net.alex.guzhenrenworld.dimension.easternsea;

import net.alex.guzhenrenworld.biome.ColumnCaches;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.XGradient;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * 东海地形：本域的 Tectonic 主世界路由副本。
 *
 * <ul>
 *   <li>移植自 Tectonic 与 Terralith 同装时的路由（{@link Ports#registerDensityFunctions}），只经移植留下的钩子改形
 *       （{@link #HOOKS}、{@link RegionBootstraps}）。</li>
 *   <li>{@code ocean_offset} 沿 x 由两个 {@code guworld:x_gradient}（{@link XGradient}）相加：西边界
 *       {@link #WEST_OCEAN_OFFSET}，到 x {@link #ISLAND_EDGE_X} 为 {@link #ISLAND_OCEAN_OFFSET}，到 x
 *       {@link #OPEN_SEA_X} 再降 {@link #OPEN_SEA_DROP}；各段成因与 {@link #VERTICAL_SCALE}、{@link #DUNES} 的取值见
 *       wiki《东海#地形方法》。</li>
 *   <li>{@code min_y}、{@code max_y}、{@code jungle_pillars}、{@code underground_rivers} 与
 *       {@code underground_river_water}、{@code lava_tunnels} 同各域；{@link ColumnCaches} 包住移植的列函数，噪声区块每
 *       列只算一次。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see EasternSea
 * @since 1.0.0
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
