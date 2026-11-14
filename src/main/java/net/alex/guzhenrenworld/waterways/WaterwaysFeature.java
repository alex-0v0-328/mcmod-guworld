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
 * 地物 {@code guworld:waterways}：给一个区块的水系河道放水、铺河床。
 *
 * <ul>
 *   <li>每区块一次，首个装饰步 {@code raw_generation}：表层规则后，本区块湖、树、植物前；维度无 {@link WaterwaysCarver}
 *       则不动。</li>
 *   <li>河道列（{@link WaterwaysCarver#getChannel}）从顶层水块（水面向上取整减 1）下穿空格找底，≤
 *       {@link #WATER_CAP} 格；空格：空气或非实心、非流体、非方块实体（邻区块先种进河道的草花由水盖掉）。</li>
 *   <li>底须在刻河河底，至多再深 1 格；更深（洞穴、坑、水面跨过的崖下）整列不动，靠邻列水流入。</li>
 *   <li>底方块上空、实心、非矿石非方块实体、下方托得住下落河床时换成区域河床（{@link WaterwaysCarver#getBed}）；
 *       有水河道从底往上放原版水源，干河道只铺床；列中原有的水（海、含水层）作底保留。</li>
 *   <li>放下或作底的水侧挨空格（下游台阶、矮岸）时排一次流体刻，加载后按原版流出；不改其他水规则。</li>
 *   <li>{@link #fill}、{@link #touchesOpen} 只取普通方块访问，测试可在自建区块上跑。</li>
 * </ul>
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
