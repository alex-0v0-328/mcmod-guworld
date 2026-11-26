package net.alex.guzhenrenworld.secretrealm;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.alex.guzhenrenworld.GuzhenrenBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.common.world.PieceBeardifierModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 荡魂山的结构片，每个与山形相交的区块一块：按 {@link SoulsunderShape} 的列跨度在本区块里逐列直接写荡魂山石。
 * <ul>
 *   <li>这是《世界#结构模板约定》（形状存 {@code .nbt} 模板、由共用模板地物加载）的特例：山体 160×128×160，按 48 格上
 *       限要拆成 4×3×4 块拼图，模板逐格存调色板序号，体积数十万格，放置时还要按模板逐块走 {@code StructureTemplate}
 *       的处理器与方块实体路径；而山只用一种方块、形状是程序生成的高度类数据，列跨度约 54 KB，逐列写最省。</li>
 *   <li>山体只用荡魂山石（主模组「移山」只搬山石）；山底以下是地基，归原版地形。山形高度不低于 {@link #FOLLOW} 格的列是
 *       山核，固定立在山底 y；更矮的山脚列随原地表渐变：权重 w 按山形高度从山核的 0 平滑升到最外缘的 1，列底从山底向写
 *       入前的 {@code OCEAN_FLOOR} 地面移 w 份，所以山脚的外缘贴着原地形起伏，坡上就搭在坡上、谷里就垂进谷里，不在山底
 *       拉出一圈平台。</li>
 *   <li>地基：结构设 {@code beard_thin}，每块结构片经 NeoForge 的 {@link PieceBeardifierModifier} 只交出本区块山核列的
 *       水平范围、底在山底 y，原版 {@code Beardifier} 在噪声阶段把山核下的地形抬到山底；没有山核列的结构片不改地形。胡
 *       须框若铺满山脚，会把整圈地面压成山底高度的平台（Alex 反馈的「一层一层的底座」之一），所以只铺山核。</li>
 *   <li>⚠ 胡须核按 {@code exp(-d²/16)} 衰减，只能填平山底下约 6 格内的落差；列底以下更深的空隙（河谷、峭壁、洞口，及
 *       山脚权重不足 1 的列与地面之间）从列底往下补 {@code minecraft:stone}，穿过空气、流体、可替换方块和树叶，直到碰到
 *       别的实心方块或世界底部，所以山脚不会悬空。补石顶总被山石盖着，不盖群系表层方块。段底不为 0 的列是山形自带的悬
 *       挑，不补。</li>
 *   <li>列顶不低于写入前的地面：原地面高过列顶时整列再上移到齐平（只在原地形高出山底 {@link #FOLLOW} 格以上时发生），所
 *       以山石上不压原地形、也不在坡里挖坑。列顶以上到写入前的 {@code WORLD_SURFACE} 高度清空：非流体方块（草、花、雪
 *       层）清成空气；含流体的方块（含水方块、海草、海带）换成其流体本身，流体源保留，所以泡在水下的山脚仍在水里，不挖
 *       出气泡。</li>
 *   <li>写入走 {@link ChunkAccess#setBlockState}（装饰阶段为 {@code ProtoChunk}）：只更新区块高度图，不经
 *       {@code WorldGenRegion} 的邻块与方块实体逻辑，也不排光照更新；光照在其后的 {@code INITIALIZE_LIGHT} 阶段对整个区
 *       块一次算出；山石不透光（主模组定 lightBlock 15），山体内部没有天空光可传。</li>
 *   <li>被覆盖的方块若带方块实体，同时移除该方块实体，免得转正式区块时留下对不上方块的实体。</li>
 *   <li>边界框是本区块里山形非空列的范围，竖直从世界底部到山顶，因为向下补的深度在装饰前不可知；山脚随地形上移的列可能
 *       略出框顶，只影响 {@code /locate} 一类的框判断。</li>
 *   <li>每座山的耗时按结构片累加，最后一片写完时记一行 INFO（山石、补石、清除数、区块数、总耗时、最慢区块）；中途重启的
 *       山只记不全，不再补记。累加表按山心坐标记，福地山心总在同一处，所以停服时清空（{@link #clearProgress}），单人
 *       换存档后不接着上一个世界的计数。</li>
 * </ul>
 */

public final class SoulsunderMountainPiece extends StructurePiece implements PieceBeardifierModifier {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<BlockPos, Progress> PROGRESS = new ConcurrentHashMap<>();
    /** 山形高度不低于此值的列是山核：立在山底、铺胡须；更矮的山脚列随原地表渐变。 */
    public static final int FOLLOW = 24;
    private final BlockPos centre;
    private final int pieces;
    private @Nullable BoundingBox core;
    private boolean coreFound;

    private SoulsunderMountainPiece(BlockPos centre, BoundingBox box, int pieces) {
        super(SoulsunderMountainRegistration.PIECE_TYPE, 0, box);
        this.centre = centre;
        this.pieces = pieces;
    }

    public SoulsunderMountainPiece(CompoundTag tag) {
        super(SoulsunderMountainRegistration.PIECE_TYPE, tag);
        this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("BY"), tag.getInt("CZ"));
        this.pieces = tag.getInt("N");
    }

    /** 山心 {@code centre}（y 为山底）的全部结构片，每个与山形非空列相交的区块一块。 */
    public static List<SoulsunderMountainPiece> forMountain(BlockPos centre, int floor) {
        SoulsunderShape shape = SoulsunderShape.get();
        int originX = centre.getX() - shape.getCentre();
        int originZ = centre.getZ() - shape.getCentre();
        int lastX = originX + shape.getWidth() - 1;
        int lastZ = originZ + shape.getDepth() - 1;
        int bottom = Math.min(floor, centre.getY());
        int top = centre.getY() + shape.getHeight() - 1;
        List<BoundingBox> boxes = new ArrayList<>();
        for (int chunkZ = originZ >> 4; chunkZ <= lastZ >> 4; chunkZ++) {
            for (int chunkX = originX >> 4; chunkX <= lastX >> 4; chunkX++) {
                int minX = Integer.MAX_VALUE;
                int minZ = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxZ = Integer.MIN_VALUE;
                for (int z = Math.max(chunkZ << 4, originZ); z <= Math.min((chunkZ << 4) + 15, lastZ); z++) {
                    for (int x = Math.max(chunkX << 4, originX); x <= Math.min((chunkX << 4) + 15, lastX); x++) {
                        if (shape.getSpanCount((z - originZ) * shape.getWidth() + x - originX) == 0) continue;

                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minZ = Math.min(minZ, z);
                        maxZ = Math.max(maxZ, z);
                    }
                }
                if (minX <= maxX) boxes.add(new BoundingBox(minX, bottom, minZ, maxX, top, maxZ));
            }
        }
        return boxes.stream().map(box -> new SoulsunderMountainPiece(centre, box, boxes.size())).toList();
    }

    public BlockPos getCentre() { return centre; }

    @Override
    protected void addAdditionalSaveData(@NotNull StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", centre.getX());
        tag.putInt("BY", centre.getY());
        tag.putInt("CZ", centre.getZ());
        tag.putInt("N", pieces);
    }

    @Override
    public @NotNull BoundingBox getBeardifierBox() {
        BoundingBox found = getCore();
        return found != null ? found : getBoundingBox();
    }

    @Override
    public @NotNull TerrainAdjustment getTerrainAdjustment() {
        return getCore() != null ? TerrainAdjustment.BEARD_THIN : TerrainAdjustment.NONE;
    }

    private @Nullable BoundingBox getCore() {
        if (coreFound) return core;

        SoulsunderShape shape = SoulsunderShape.get();
        int originX = centre.getX() - shape.getCentre();
        int originZ = centre.getZ() - shape.getCentre();
        BoundingBox box = getBoundingBox();
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                int column = (z - originZ) * shape.getWidth() + x - originX;
                int count = shape.getSpanCount(column);
                if (count == 0 || shape.getTop(column, count - 1) + 1 < FOLLOW) continue;

                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        core = minX <= maxX ? new BoundingBox(minX, centre.getY(), minZ, maxX, centre.getY(), maxZ) : null;
        coreFound = true;
        return core;
    }

    @Override
    public int getGroundLevelDelta() { return 0; }

    @Override
    public void postProcess(@NotNull WorldGenLevel level, @NotNull StructureManager structureManager,
            @NotNull ChunkGenerator generator, @NotNull RandomSource random, @NotNull BoundingBox box,
            @NotNull ChunkPos chunkPos, @NotNull BlockPos pos) {
        long start = System.nanoTime();
        int[] placed = fill(level.getChunk(chunkPos.x, chunkPos.z),
                GuzhenrenBlocks.SOULSUNDER_STONE.get().defaultBlockState(), Blocks.STONE.defaultBlockState());
        recordProgress(System.nanoTime() - start, placed);
    }

    /** 在 {@code chunk} 里写本结构片的列，返回 {山石数, 补石数, 清除数}。 */
    public int[] fill(ChunkAccess chunk, BlockState mountain, BlockState foundation) {
        SoulsunderShape shape = SoulsunderShape.get();
        int originX = centre.getX() - shape.getCentre();
        int originZ = centre.getZ() - shape.getCentre();
        BoundingBox box = getBoundingBox();
        ChunkPos chunkPos = chunk.getPos();
        int minX = Math.max(chunkPos.getMinBlockX(), box.minX());
        int maxX = Math.min(chunkPos.getMaxBlockX(), box.maxX());
        int minZ = Math.max(chunkPos.getMinBlockZ(), box.minZ());
        int maxZ = Math.min(chunkPos.getMaxBlockZ(), box.maxZ());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int[] placed = new int[3];
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                int column = (z - originZ) * shape.getWidth() + x - originX;
                fillColumn(chunk, shape, column, cursor.set(x, centre.getY(), z), mountain, foundation, placed);
            }
        }
        return placed;
    }

    private void fillColumn(ChunkAccess chunk, SoulsunderShape shape, int column, BlockPos.MutableBlockPos cursor,
            BlockState mountain, BlockState foundation, int[] placed) {
        int count = shape.getSpanCount(column);
        if (count == 0) return;

        int floor = chunk.getMinBuildHeight();
        int ceiling = chunk.getMaxBuildHeight() - 1;
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, cursor.getX(), cursor.getZ());
        int ground = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, cursor.getX(), cursor.getZ());
        int height = shape.getTop(column, count - 1) + 1;
        int base = centre.getY() + getShift(height, ground);
        for (int span = 0; span < count; span++) {
            int top = Math.min(ceiling, base + shape.getTop(column, span));
            for (int y = Math.max(floor, base + shape.getBottom(column, span)); y <= top; y++) {
                setBlock(chunk, cursor.setY(y), mountain);
                placed[0]++;
            }
        }
        for (int y = Math.max(floor, base + height); y <= surface; y++) {
            BlockState state = chunk.getBlockState(cursor.setY(y));
            BlockState bare = state.getFluidState().createLegacyBlock();
            if (state == bare) continue;

            setBlock(chunk, cursor, bare);
            placed[2]++;
        }
        if (shape.getBottom(column, 0) != 0) return;

        for (int y = base - 1; y >= floor && isOpen(chunk.getBlockState(cursor.setY(y))); y--) {
            setBlock(chunk, cursor, foundation);
            placed[1]++;
        }
    }

    private int getShift(int height, int ground) {
        double outward = Math.clamp(1.0 - (double) height / FOLLOW, 0.0, 1.0);
        double weight = outward * outward * (3 - 2 * outward);
        int follow = (int) Math.round(weight * (ground + 1 - centre.getY()));
        return Math.max(follow, ground + 1 - height - centre.getY());
    }

    private static boolean isOpen(BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES);
    }

    private static void setBlock(ChunkAccess chunk, BlockPos pos, BlockState state) {
        BlockState previous = chunk.setBlockState(pos, state, false);
        if (previous != null && previous.hasBlockEntity()) chunk.removeBlockEntity(pos);
    }

    static void clearProgress() {
        PROGRESS.clear();
    }

    private void recordProgress(long nanos, int[] placed) {
        Progress progress = PROGRESS.computeIfAbsent(centre, key -> new Progress());
        progress.mountain.addAndGet(placed[0]);
        progress.foundation.addAndGet(placed[1]);
        progress.cleared.addAndGet(placed[2]);
        progress.nanos.addAndGet(nanos);
        progress.slowest.accumulateAndGet(nanos, Math::max);
        if (progress.pieces.incrementAndGet() < pieces || PROGRESS.remove(centre) == null) return;

        LOGGER.info("Soulsunder Mountain at {}: {} mountain + {} foundation blocks, {} cleared, in {} chunks, {} ms "
                        + "in all, slowest chunk {} ms", centre.toShortString(), progress.mountain.get(),
                progress.foundation.get(), progress.cleared.get(), pieces, progress.nanos.get() / 1_000_000,
                progress.slowest.get() / 1_000_000);
    }

    private static final class Progress {

        private final AtomicInteger pieces = new AtomicInteger();
        private final AtomicInteger mountain = new AtomicInteger();
        private final AtomicInteger foundation = new AtomicInteger();
        private final AtomicInteger cleared = new AtomicInteger();
        private final AtomicLong nanos = new AtomicLong();
        private final AtomicLong slowest = new AtomicLong();
    }
}
