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
 * 为单个区块的水系河槽注入水体并铺设河床材质的地物实现（{@code guworld:waterways}）。
 * <ul>
 *   <li>挂载在首个装饰阶段 {@code RAW_GENERATION}（表层规则执行完毕后、任何湖泊与植被装饰之前），每个区块执行一次；若当
 *       前维度未挂载 {@link WaterwaysCarver} 则直接跳过。</li>
 *   <li>对命中河道的水平列（{@link WaterwaysCarver#getChannel}），从目标水面顶层水块（{@code ceil(level) - 1}）向下穿过
 *       至多{@link #WATER_CAP} 格开放方块（空气，或无碰撞、无流体且无方块实体的植物）寻找河底。</li>
 *   <li>探测到的底面必须不低于理论刻河河底以下 1 格，若下方为深坑、洞穴或悬崖断层则整列跳过，交由相邻河道列的水体自然漫
 *       入。</li>
 *   <li>若河底方块为可替换实心方块（非矿石、非方块实体）且其下方能托住重力方块，则将其替换为本域配置的河床方块（
 *       {@link WaterwaysCarver#getBed}）；有水河道随后从河底上方填充水源至水面，干涸河道则仅铺设河床；若探测停止在原有
 *       水体（海洋或含水层）上，则直接以该水体为底。</li>
 *   <li>新放置或充当底面的水源方块若水平四邻接存在开放方块（如顺流而下的河道台阶），会立即调度一次水流体刻，使区块加载
 *       后水流自然衔接。</li>
 * </ul>
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

    @SuppressWarnings("deprecation")
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

    @SuppressWarnings("deprecation")
    private static boolean isReplaceable(BlockState state) {
        return state.getFluidState().isEmpty() && state.isSolid() && !state.hasBlockEntity()
                && !state.is(Tags.Blocks.ORES);
    }
}
