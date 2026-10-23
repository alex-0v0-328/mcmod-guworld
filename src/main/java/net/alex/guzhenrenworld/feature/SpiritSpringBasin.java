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
 * The Spirit Spring [元泉] basin: a 7x7 structure laid out as three layers around the center column's ground
 * height g ({@code MOTION_BLOCKING_NO_LEAVES} - 1, {@link #getGroundY}), laid by {@link #placeStructure} on the spots
 * {@link SpiritSpringCluster} picks.
 *
 * <p>Layer one at g-1 buries the calcite basin; layer two at g paves the ground with slabs and
 * raises the calcite pillar under the spring; layer three at g+1 is only the source block. The
 * layer-two ring around the pillar is cleared to air ({@code a}), so the four streams fall into
 * the calcite basin and the rim holds the water instead of letting it sheet outward. Outer
 * {@code x} cells keep the original terrain (dirt stays dirt).
 * {@link #placeStructure} lays both grid layers and the source around the ground block at layer
 * two's level. Every non-{@code x} column ({@link #COLUMNS}) is first leveled to g: ground up to
 * {@link #MAX_RISE} lower is filled with dirt up to the basin, and {@link #CLEAR_HEIGHT_ABOVE} cells above g
 * are cleared, enough for ground up to {@link #MAX_RISE} higher with double plants or snow on it; that leveling is
 * the rise {@link SpiritSpringGround} accepts. Every block goes down with {@link Block#UPDATE_CLIENTS}.
 * {@link #placeStructure} is public for GameTests, which run on a {@code ServerLevel}.
 *
 * <p>The spring itself is Guzhenren's block, reached by id through {@link GuzhenrenBlocks#SPIRIT_SPRING}
 * because this mod never compiles against Guzhenren; the first flow tick goes to the fluid the
 * block's default state carries, its source. Guzhenren keeps the block, fluid, item and rendering.
 *
 * <p>⚠ The layout is Alex's pick, not a silent tunable. Symbol legend:
 * {@code x}=keep, {@code a}=air (the basin well), {@code y}=cobblestone, {@code t}=mossy cobblestone,
 * {@code f}=calcite, {@code c}=cobblestone slab (bottom), {@code m}=mossy cobblestone slab (bottom).
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
