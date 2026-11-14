package net.alex.guzhenrenworld.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 元泉[Spirit Spring] 石盆落点：平整、干燥、无树的地面。
 *
 * <ul>
 *   <li>{@link #findSurfaceGround} 判定 {@link SpiritSpringCluster} 试的每个点，公开供 GameTest。</li>
 *   <li>山顶、山脚、平原皆可：中心 15×15（半径 {@link #FLAT_RADIUS}，约石盆边长两倍）
 *       内高差 ≤ {@link SpiritSpringBasin#MAX_RISE} 格，即整平能吸收的起伏。</li>
 *   <li>植被先跑（{@code VEGETAL_DECORATION} 早于 {@code TOP_LAYER_MODIFICATION}），坡、水坑、树都改高度图：
 *       范围内有树、中心是树干或树叶即否决；无碰撞、躲过高度图的竹子与甘蔗在清空带
 *       （{@link SpiritSpringBasin#CLEAR_HEIGHT_ABOVE}）否决。</li>
 *   <li>{@link #isTerrainWet}：结构列地面为冰、或地面及上一格有流体即否决：齐平的水过得了高度规则，
 *       原版 {@code freeze_top_layer} 同步更早（群系修改器排在群系自身地物后），冻住的水坑或河只是平整冰面。</li>
 *   <li>泉群搜索越出群系过滤，也会落到河流和冻洋。</li>
 *   <li>⚠ 检查伸进邻区块 ±16 格，那里的树可能未长：合格与否随区块生成顺序变，同原版放树。</li>
 *   <li>⚠ 平整度由 Alex 定，不得私调。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SpiritSpringBasin
 * @since 1.0.0
 */

public final class SpiritSpringGround {

    private static final int FLAT_RADIUS = 7;

    private SpiritSpringGround() {}

    public static @Nullable BlockPos findSurfaceGround(WorldGenLevel level, int x, int z) {
        BlockPos groundCenter = new BlockPos(x, SpiritSpringBasin.getGroundY(level, x, z), z);
        BlockState groundState = level.getBlockState(groundCenter);
        if (groundState.is(BlockTags.LOGS) || groundState.is(BlockTags.LEAVES)) return null;
        if (isTerrainRough(level, groundCenter) || isTerrainWet(level, groundCenter)) return null;
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
