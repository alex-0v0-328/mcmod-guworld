package net.alex.guzhenrenworld.waterways;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;

/**
 * The feature {@code guworld:waterways}: fills one chunk's waterways [水系] channels with water and lays their beds,
 * run once per chunk in the first decoration step, {@code raw_generation}, after the surface rules and before the
 * chunk's own lakes, trees and plants. A chunk outside a carved region, with no {@link WaterwaysCarver} on its level,
 * is left alone.
 *
 * <p>Each column inside a channel ({@link WaterwaysCarver#getChannel}) is scanned down from the top water block, one
 * under the water surface rounded up, through open blocks to its floor, at most {@link #WATER_CAP} blocks. Open is
 * air or a block that is neither solid, a fluid nor a block entity: a neighbouring chunk's features run before this
 * chunk's whenever that chunk gets there first, and may have planted grass or flowers in the channel, which the water
 * then replaces. The floor has to lie where the carving put the channel's bottom, at most one block deeper; a column
 * open further down, into a cave, a pit or over a cliff the water profile spilled across, is left as it is, and the
 * water placed beside it flows in. The floor's block becomes the region's bed ({@link WaterwaysCarver#getBed}) when it
 * is open above, solid, neither an ore nor a block entity, and resting on a block a falling bed would not drop
 * through; a wet channel then gets vanilla water sources from the floor up, a dry one only its bed. Water already
 * standing in the column, the sea's or an aquifer's, is the floor and stays.
 *
 * <p>Water next to an open block, placed or found as the floor, at a step down the stream or where a bank falls short,
 * gets a fluid tick, so vanilla water flows out over it once the chunk is loaded; no other water rule changes.
 * {@link #fill} and {@link #touchesOpen} take plain block access, so a test can run them on chunks of its own.
 *
 * @author Alex
 * @version 1.0.0
 * @see WaterwaysRegistration
 * @since 1.0.0
 */

public final class WaterwaysFeature extends Feature<NoneFeatureConfiguration> {

    public static final String ID = "waterways";
    private static final int WATER_CAP = 6;

    public WaterwaysFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        WaterwaysCarver carver = WaterwaysRuntime.get(level.getLevel());
        if (carver == null) return false;
        List<BlockPos> water = fill(carver, new ChunkPos(context.origin()), level,
                (pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
        for (BlockPos source : water) {
            if (touchesOpen(level, source)) level.scheduleTick(source, Fluids.WATER, 0);
        }
        return !water.isEmpty();
    }

    public static List<BlockPos> fill(WaterwaysCarver carver, ChunkPos chunk, BlockGetter level,
            BiConsumer<BlockPos, BlockState> setter) {
        List<BlockPos> water = new ArrayList<>();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                fill(carver, x, z, level, setter, water);
            }
        }
        return water;
    }

    public static boolean touchesOpen(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isOpen(level.getBlockState(pos.relative(direction)))) return true;
        }
        return false;
    }

    public static boolean isOpen(BlockState state) {
        return state.isAir() || !state.isSolid() && state.getFluidState().isEmpty() && !state.hasBlockEntity();
    }

    private static void fill(WaterwaysCarver carver, int x, int z, BlockGetter level,
            BiConsumer<BlockPos, BlockState> setter, List<BlockPos> water) {
        WaterwaysIndex.Channel channel = carver.getChannel(x, z);
        if (channel == null) return;
        int top = Mth.ceil(channel.level()) - 1;
        BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos(x, top, z);
        int floor = top;
        while (floor > top - WATER_CAP && isOpen(level.getBlockState(column.setY(floor)))) floor--;
        BlockState bed = level.getBlockState(column.setY(floor));
        if (floor >= top || floor < Mth.ceil(channel.bottom()) - 2 || isOpen(bed)) return;
        if (isReplaceable(bed) && isOpen(level.getBlockState(column.setY(floor + 1)))
                && !FallingBlock.isFree(level.getBlockState(column.setY(floor - 1)))) {
            setter.accept(column.setY(floor), carver.getBed(x, z));
        }
        if (!channel.wet()) return;
        if (bed.getFluidState().is(FluidTags.WATER)) water.add(column.setY(floor).immutable());
        for (int y = floor + 1; y <= top; y++) {
            BlockPos source = column.setY(y).immutable();
            setter.accept(source, Blocks.WATER.defaultBlockState());
            water.add(source);
        }
    }

    private static boolean isReplaceable(BlockState state) {
        return state.getFluidState().isEmpty() && state.isSolid() && !state.hasBlockEntity()
                && !state.is(Tags.Blocks.ORES);
    }
}
