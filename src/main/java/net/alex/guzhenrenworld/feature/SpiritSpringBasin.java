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
 * 元泉 7×7 石盆结构的方块放置逻辑，以中心列的地表高度 {@code g} 为基准分三层构筑。
 * <ul>
 *   <li>基准高 {@code g} 取 {@code MOTION_BLOCKING_NO_LEAVES - 1}：{@code g - 1} 层（{@link #LAYER_ONE}）铺设方解石池底
 *       与外缘石料，{@code g} 层（{@link #LAYER_TWO}）铺设下半砖池沿、留出池井空气（{@code a}）并立起中心方解石柱，
 *       {@code g + 1}层在中心柱顶放置元泉液体源方块并立即调度一次流体刻使其向四周流下池井。</li>
 *   <li>放置前先对非保留列（{@link #COLUMNS}，即非 {@code x} 格）做地形整平：地面低于 {@code g - 1} 的列（至多低
 *       {@link #MAX_RISE} 格）向上垫泥土至池底下方，并在 {@code g} 层以上清空 {@link #CLEAR_HEIGHT_ABOVE} 格高度以清除
 *       隆起地面上的双格植物或积雪。</li>
 *   <li>{@link #placeStructure} 在 {@link SpiritSpringCluster} 选定的落点生成石盆（公开供 GameTest 直接调用）；元泉液体
 *       方块通过 {@link GuzhenrenBlocks#SPIRIT_SPRING} 按注册 ID 获取，避免编译期依赖主模组。</li>
 *   <li>模板字符映射：{@code x} 保留原地形，{@code a} 为空气（池井），{@code y} 为圆石，{@code t} 为苔石，{@code f} 为
 *       方解石，{@code c} 与 {@code m} 分别为圆石下半砖与苔石下半砖。</li>
 * </ul>
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
