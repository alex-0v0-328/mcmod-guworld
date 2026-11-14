package net.alex.guzhenrenworld.feature;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenrenworld.GuzhenrenBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * 元泉[Spirit Spring] 石盆：7×7，以中心列地面高 g 分三层。
 *
 * <ul>
 *   <li>g = {@code MOTION_BLOCKING_NO_LEAVES} − 1；g − 1 层方解石池底，g 层铺半砖、立方解石柱，g + 1 层放源方块。</li>
 *   <li>g 层柱周清空（{@code a}），四股水落池、池沿兜住不外漫；外缘 {@code x} 格留原地形。</li>
 *   <li>非 {@code x} 列（{@link #COLUMNS}）先整平到 g：低 ≤ {@link #MAX_RISE} 格垫泥土至池底下，
 *       g 上清空 {@link #CLEAR_HEIGHT_ABOVE} 格（容高 {@link #MAX_RISE} 格带双格植物或雪的地面），
 *       即 {@link SpiritSpringGround} 容许的起伏。</li>
 *   <li>{@link #placeStructure} 落在 {@link SpiritSpringCluster} 选的点，公开供 GameTest（{@code ServerLevel}）。</li>
 *   <li>泉方块（连同流体、物品、渲染）属主模组，因不编译依赖主模组，经 {@link GuzhenrenBlocks#SPIRIT_SPRING} 按 ID 取；
 *       首次流体刻给默认状态的流体（源）。</li>
 *   <li>⚠ 布局由 Alex 定，不得私调。符号：{@code x} 保留，{@code a} 空气（池井），{@code y} 圆石，{@code t}
 *       苔石，{@code f} 方解石，{@code c}、{@code m} 圆石、苔石半砖（下半）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SpiritSpringCluster
 * @since 1.0.0
 */

public final class SpiritSpringBasin {

    static final String[] LAYER_ONE = {
            "xxtttxx",
            "xtffftx",
            "tfffffy",
            "yfffffy",
            "yffffft",
            "xtfffyx",
            "xxytyxx" };
    static final String[] LAYER_TWO = {
            "xxyytxx",
            "xtmcmyx",
            "ycaaamy",
            "tmafact",
            "ymaaacy",
            "xyccmyx",
            "xxttyxx" };
    static final int MAX_RISE = 3;
    static final int CLEAR_HEIGHT_ABOVE = MAX_RISE + 2;
    private static final int RADIUS = 3;
    static final List<BlockPos> COLUMNS = collectColumns();

    private SpiritSpringBasin() {}

    public static void placeStructure(LevelAccessor level, BlockPos groundCenter) {
        for (BlockPos offset : COLUMNS) {
            BlockPos column = groundCenter.offset(offset);
            for (int y = getGroundY(level, column.getX(), column.getZ()) + 1; y < groundCenter.getY() - 1; y++) {
                level.setBlock(column.atY(y), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        placeLayer(level, groundCenter.below(), LAYER_ONE);
        placeLayer(level, groundCenter, LAYER_TWO);
        for (BlockPos offset : COLUMNS) {
            for (int offsetY = 1; offsetY <= CLEAR_HEIGHT_ABOVE; offsetY++) {
                level.setBlock(groundCenter.offset(offset).above(offsetY), Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_CLIENTS);
            }
        }
        BlockPos source = groundCenter.above();
        BlockState sourceState = GuzhenrenBlocks.SPIRIT_SPRING.get().defaultBlockState();
        level.setBlock(source, sourceState, Block.UPDATE_CLIENTS);
        level.scheduleTick(source, sourceState.getFluidState().getType(), 0);
    }

    static int getGroundY(LevelAccessor level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    static @Nullable BlockState getState(char symbol) {
        return switch (symbol) {
            case 'y' -> Blocks.COBBLESTONE.defaultBlockState();
            case 't' -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 'f' -> Blocks.CALCITE.defaultBlockState();
            case 'c' -> Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case 'm' -> Blocks.MOSSY_COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case 'a' -> Blocks.AIR.defaultBlockState();
            default -> null;
        };
    }

    private static void placeLayer(LevelAccessor level, BlockPos layerCenter, String[] grid) {
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length(); column++) {
                BlockState state = getState(grid[row].charAt(column));
                if (state == null) continue;
                level.setBlock(layerCenter.offset(getOffset(row, column)), state, Block.UPDATE_CLIENTS);
            }
        }
    }

    private static List<BlockPos> collectColumns() {
        List<BlockPos> columns = new ArrayList<>();
        for (int row = 0; row < LAYER_TWO.length; row++) {
            for (int column = 0; column < LAYER_TWO[row].length(); column++) {
                if (LAYER_TWO[row].charAt(column) != 'x') columns.add(getOffset(row, column));
            }
        }
        return List.copyOf(columns);
    }

    private static BlockPos getOffset(int row, int column) { return new BlockPos(column - RADIUS, 0, row - RADIUS); }
}
