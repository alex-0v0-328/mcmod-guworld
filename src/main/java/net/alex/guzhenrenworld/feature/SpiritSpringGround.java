package net.alex.guzhenrenworld.feature;

import net.alex.guzhenrenworld.GuzhenrenBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 元泉石盆落点的地面资格判定：筛选平整、干燥且无树木竹蔗遮挡的地表位置。
 * <ul>
 *   <li>{@link #findSurfaceGround} 负责校验 {@link SpiritSpringCluster} 尝试的每个候选列（公开供 GameTest 直接调用）：
 *       要求以中心列为基准、半径 {@link #FLAT_RADIUS}（15×15）内的地表高差不超过 {@link SpiritSpringBasin#MAX_RISE}
 *       格，控制在石盆整平能够吸收的起伏范围内。</li>
 *   <li>由于元泉挂载在 {@code TOP_LAYER_MODIFICATION} 阶段（晚于 {@code VEGETAL_DECORATION}），树木、水坑与陡坡均已反映
 *       在高度图中：中心列地表为原木或树叶时直接否决；无碰撞体积而不计入高度图的竹子与甘蔗则在石盆清空带（
 *       {@link SpiritSpringBasin#CLEAR_HEIGHT_ABOVE}）内逐格扫描否决。</li>
 *   <li>{@link #isTerrainWet} 否决结构列地表为冰块、或地表及上方一格存在流体的位置，防止泉群跨出群系边界落入河流、冻河
 *       或结冰水坑（原版 {@code freeze_top_layer} 作为群系内置地物先于群系修改器执行，会将水面冻成平整冰面）。</li>
 *   <li>{@link #isOnTheMountain} 否决石盆会写到的任一格（各盆柱从池底下 {@link SpiritSpringBasin#MAX_RISE} 格到清空带
 *       顶，及中心泉眼）为荡魂山石（按主模组 ID 判断）的落点：元泉晚于荡魂山写入，不否决就会把池底、垫土和清空带凿进山
 *       体。</li>
 *   <li>⚠ 15×15 檢查范围会伸入相邻区块，而相邻区块的树木可能尚未装饰完毕，因此候选点是否通过会受区块生成先后顺序影响（
 *       与原版树木放置机制一致）。</li>
 * </ul>
 */

public final class SpiritSpringGround {

    private static final int FLAT_RADIUS = 7;

    private SpiritSpringGround() {}

    public static @Nullable BlockPos findSurfaceGround(WorldGenLevel level, int x, int z) {
        BlockPos groundCenter = new BlockPos(x, SpiritSpringBasin.getGroundY(level, x, z), z);
        BlockState groundState = level.getBlockState(groundCenter);
        if (groundState.is(BlockTags.LOGS) || groundState.is(BlockTags.LEAVES)) return null;
        if (isTerrainRough(level, groundCenter) || isTerrainWet(level, groundCenter)) return null;
        if (isOnTheMountain(level, groundCenter)) return null;
        return groundCenter;
    }

    private static boolean isTerrainRough(WorldGenLevel level, BlockPos groundCenter) {
        int lowest = groundCenter.getY();
        int highest = groundCenter.getY();
        for (int offsetX = -FLAT_RADIUS; offsetX <= FLAT_RADIUS; offsetX++) {
            for (int offsetZ = -FLAT_RADIUS; offsetZ <= FLAT_RADIUS; offsetZ++) {
                int y = SpiritSpringBasin.getGroundY(level, groundCenter.getX() + offsetX,
                        groundCenter.getZ() + offsetZ);
                lowest = Math.min(lowest, y);
                highest = Math.max(highest, y);
                if (highest - lowest > SpiritSpringBasin.MAX_RISE) return true;
            }
        }
        for (BlockPos offset : SpiritSpringBasin.COLUMNS) {
            BlockPos ground = groundCenter.offset(offset);
            for (int offsetY = 1; offsetY <= SpiritSpringBasin.CLEAR_HEIGHT_ABOVE; offsetY++) {
                Block above = level.getBlockState(ground.above(offsetY)).getBlock();
                if (above instanceof BambooStalkBlock || above instanceof SugarCaneBlock) return true;
            }
        }
        return false;
    }

    private static boolean isOnTheMountain(WorldGenLevel level, BlockPos groundCenter) {
        int lowest = -1 - SpiritSpringBasin.MAX_RISE;
        for (BlockPos offset : SpiritSpringBasin.COLUMNS) {
            BlockPos column = groundCenter.offset(offset);
            for (int offsetY = lowest; offsetY <= SpiritSpringBasin.CLEAR_HEIGHT_ABOVE; offsetY++) {
                if (isSoulquakeStone(level, column.above(offsetY))) return true;
            }
        }
        return isSoulquakeStone(level, groundCenter.above());
    }

    private static boolean isSoulquakeStone(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlockHolder().is(GuzhenrenBlocks.SOULQUAKE_STONE.getKey());
    }

    private static boolean isTerrainWet(WorldGenLevel level, BlockPos groundCenter) {
        for (BlockPos offset : SpiritSpringBasin.COLUMNS) {
            BlockPos column = groundCenter.offset(offset);
            BlockPos ground = column.atY(SpiritSpringBasin.getGroundY(level, column.getX(), column.getZ()));
            if (level.getBlockState(ground).is(BlockTags.ICE)) return true;
            if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.above()).isEmpty()) return true;
        }
        return false;
    }
}
