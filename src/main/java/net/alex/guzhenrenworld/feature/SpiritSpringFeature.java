package net.alex.guzhenrenworld.feature;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenrenworld.GuzhenrenBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * The Spirit Spring [元泉] surface structure: a 7x7 basin laid out as three layers around the center
 * column's ground height g ({@code MOTION_BLOCKING_NO_LEAVES} - 1, judged by {@link #findSurfaceGround}).
 * {@link SpiritSpring} registers it and writes its placement data.
 *
 * <p>Layer one at g-1 buries the calcite basin; layer two at g paves the ground with slabs and
 * raises the calcite pillar under the spring; layer three at g+1 is only the source block. The
 * layer-two ring around the pillar is cleared to air ({@code a}), so the four streams fall into
 * the calcite basin and the rim holds the water instead of letting it sheet outward (Alex,
 * 2026-09-23). Outer {@code x} cells keep the original terrain (dirt stays dirt).
 * {@link #placeStructure} lays both grid layers and the source around the ground block at layer
 * two's level, first clearing {@link #CLEAR_HEIGHT_ABOVE} cells over every non-{@code x} column
 * ({@link #BASIN_COLUMNS}, enough for double plants and snow layers); it is public for GameTests,
 * which run on a {@code ServerLevel}.
 *
 * <p>The spring itself is Guzhenren's block, reached by id through {@link GuzhenrenBlocks#SPIRIT_SPRING}
 * because this mod never compiles against Guzhenren; the first flow tick goes to the fluid the
 * block's default state carries, its source. Guzhenren keeps the block, fluid, item and rendering;
 * this structure moved here on 2026-10-02 (Alex).
 *
 * <p>Clusters (Alex, 2026-09-26): a spot that passes the rarity roll grows 1..5 springs -- 50%,
 * 20%, 15%, 10%, 5% ({@link #CLUSTER_SIZE_WEIGHTS}; {@link #getClusterSize} maps a [0,100)
 * roll through the cumulative weights). Every spring of one cluster sits 8..16 blocks (horizontal)
 * from every other ({@link #isSeparationAcceptable}), so the 7x7 basins never overlap. Members are
 * scattered around the feature origin within the Chebyshev bound {@link #MAX_ORIGIN_OFFSET}: the
 * origin sits inside the generating chunk and features may write one chunk past its border, so ±12
 * keeps every 7x7 inside the 3x3 chunk window instead of clipping outer columns. {@link #findMemberSpot}
 * tries {@link #MEMBER_SPOT_ATTEMPTS} random candidates per member; a member whose candidates all fail
 * terrain checks is dropped -- the rolled size is the attempt, not a quota.
 *
 * <p>Anchor search (Alex, 2026-10-02): {@link #findAnchorSpot} tries the origin column, then up to
 * {@link #ANCHOR_SPOT_ATTEMPTS} random columns within the same ±{@link #MAX_ORIGIN_OFFSET} bound.
 * Judged at the origin alone, the strict terrain rules let ≈1-2% of rolls through, so a spring stood
 * about one per 50,000 chunks; the search keeps the rules and only gives each roll more columns
 * (≈4% of rolls now grow a cluster). A {@code /place feature} likewise lands on qualifying ground
 * within that bound of the player instead of failing on the column underfoot.
 *
 * <p>Surface placement ({@link #findSurfaceGround}) happens on flat land only (Alex, 2026-09-24):
 * vegetation runs before us ({@code VEGETAL_DECORATION} precedes {@code TOP_LAYER_MODIFICATION}),
 * so above every non-{@code x} column the two cells over the ground are cleared to air. Logs and
 * leaves, fluids, and any structure column whose own surface height differs from the center's
 * veto the spot ({@link #isTerrainUneven}) -- slopes, ponds and trees all move the heightmap, so
 * bumps never leave plants floating above the carve -- and stalk plants that dodge the heightmap
 * (no collision: bamboo, sugar cane) veto from the clear band.
 *
 * <p>⚠ Layout, rarity and the cluster numbers are Alex's picks (2026-09-23, 2026-09-24,
 * 2026-09-26, 2026-10-02), not silent tunables. Symbol legend: {@code x}=keep, {@code a}=air (the
 * basin well), {@code y}=cobblestone, {@code t}=mossy cobblestone, {@code f}=calcite,
 * {@code c}=cobblestone slab (bottom), {@code m}=mossy cobblestone slab (bottom).
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class SpiritSpringFeature extends Feature<NoneFeatureConfiguration> {

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
    private static final int RADIUS = 3;
    private static final int CLEAR_HEIGHT_ABOVE = 2;
    private static final List<BlockPos> BASIN_COLUMNS = collectBasinColumns();

    public SpiritSpringFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockPos anchor = findAnchorSpot(level, origin, random);
        if (anchor == null) return false;
        int clusterSize = getClusterSize(random.nextInt(100));
        placeStructure(level, anchor);
        List<BlockPos> placed = new ArrayList<>();
        placed.add(anchor);
        for (int member = 1; member < clusterSize; member++) {
            BlockPos spot = findMemberSpot(level, origin, placed, random);
            if (spot == null) continue;
            placeStructure(level, spot);
            placed.add(spot);
        }
        return true;
    }

    //region Cluster [丛生] -- the size roll, the spacing and the member search (Alex, 2026-09-26)
    static final int[] CLUSTER_SIZE_WEIGHTS = { 50, 20, 15, 10, 5 };
    static final int CLUSTER_MIN_SEPARATION = 8;
    static final int CLUSTER_MAX_SEPARATION = 16;
    private static final int MEMBER_SPOT_ATTEMPTS = 32;
    private static final int MAX_ORIGIN_OFFSET = 12;

    static int getClusterSize(int roll) {
        int cumulative = 0;
        for (int index = 0; index < CLUSTER_SIZE_WEIGHTS.length; index++) {
            cumulative += CLUSTER_SIZE_WEIGHTS[index];
            if (roll < cumulative) return index + 1;
        }
        return CLUSTER_SIZE_WEIGHTS.length;
    }

    static boolean isSeparationAcceptable(int offsetX, int offsetZ) {
        double distance = Math.sqrt((double) offsetX * offsetX + (double) offsetZ * offsetZ);
        return distance >= CLUSTER_MIN_SEPARATION && distance <= CLUSTER_MAX_SEPARATION;
    }

    private static @Nullable BlockPos findMemberSpot(WorldGenLevel level, BlockPos origin, List<BlockPos> placed,
            RandomSource random) {
        for (int attempt = 0; attempt < MEMBER_SPOT_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double reach = CLUSTER_MIN_SEPARATION
                    + random.nextDouble() * (CLUSTER_MAX_SEPARATION - CLUSTER_MIN_SEPARATION + 1.0);
            int offsetX = (int) Math.round(reach * Math.cos(angle));
            int offsetZ = (int) Math.round(reach * Math.sin(angle));
            if (Math.abs(offsetX) > MAX_ORIGIN_OFFSET || Math.abs(offsetZ) > MAX_ORIGIN_OFFSET) continue;
            int x = origin.getX() + offsetX;
            int z = origin.getZ() + offsetZ;
            boolean spaced = placed.stream()
                    .allMatch(spring -> isSeparationAcceptable(x - spring.getX(), z - spring.getZ()));
            if (!spaced) continue;
            BlockPos groundCenter = findSurfaceGround(level, x, z);
            if (groundCenter != null) return groundCenter;
        }
        return null;
    }
    //endregion

    //region Anchor search [就近找点] -- the origin column first, then random columns within the cluster bound
    private static final int ANCHOR_SPOT_ATTEMPTS = 32;

    private static @Nullable BlockPos findAnchorSpot(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos groundCenter = findSurfaceGround(level, origin.getX(), origin.getZ());
        for (int attempt = 0; groundCenter == null && attempt < ANCHOR_SPOT_ATTEMPTS; attempt++) {
            int offsetX = random.nextIntBetweenInclusive(-MAX_ORIGIN_OFFSET, MAX_ORIGIN_OFFSET);
            int offsetZ = random.nextIntBetweenInclusive(-MAX_ORIGIN_OFFSET, MAX_ORIGIN_OFFSET);
            groundCenter = findSurfaceGround(level, origin.getX() + offsetX, origin.getZ() + offsetZ);
        }
        return groundCenter;
    }
    //endregion

    //region Terrain [地形] -- flat, dry, treeless ground only (Alex, 2026-09-24)
    private static @Nullable BlockPos findSurfaceGround(WorldGenLevel level, int x, int z) {
        BlockPos groundCenter = new BlockPos(x, getGroundY(level, x, z), z);
        BlockState groundState = level.getBlockState(groundCenter);
        if (groundState.is(BlockTags.LOGS) || groundState.is(BlockTags.LEAVES)) return null;
        if (!level.getFluidState(groundCenter).isEmpty()
                || !level.getFluidState(groundCenter.above()).isEmpty()) return null;
        if (isTerrainUneven(level, groundCenter)) return null;
        return groundCenter;
    }

    private static int getGroundY(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    private static boolean isTerrainUneven(WorldGenLevel level, BlockPos groundCenter) {
        for (BlockPos offset : BASIN_COLUMNS) {
            BlockPos ground = groundCenter.offset(offset);
            if (getGroundY(level, ground.getX(), ground.getZ()) != groundCenter.getY()) return true;
            for (int offsetY = 1; offsetY <= CLEAR_HEIGHT_ABOVE; offsetY++) {
                Block above = level.getBlockState(ground.above(offsetY)).getBlock();
                if (above instanceof BambooStalkBlock || above instanceof SugarCaneBlock) return true;
            }
        }
        return false;
    }
    //endregion

    //region Placement [放置] -- the two grid layers, the clear band and the source
    public static void placeStructure(LevelAccessor level, BlockPos groundCenter) {
        placeLayer(level, groundCenter.below(), LAYER_ONE);
        placeLayer(level, groundCenter, LAYER_TWO);
        for (BlockPos offset : BASIN_COLUMNS) {
            for (int offsetY = 1; offsetY <= CLEAR_HEIGHT_ABOVE; offsetY++) {
                level.setBlock(groundCenter.offset(offset).above(offsetY), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        BlockPos source = groundCenter.above();
        BlockState sourceState = GuzhenrenBlocks.SPIRIT_SPRING.get().defaultBlockState();
        level.setBlock(source, sourceState, 2);
        level.scheduleTick(source, sourceState.getFluidState().getType(), 0);
    }

    private static void placeLayer(LevelAccessor level, BlockPos layerCenter, String[] grid) {
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length(); column++) {
                BlockState state = getState(grid[row].charAt(column));
                if (state == null) continue;
                level.setBlock(layerCenter.offset(column - RADIUS, 0, row - RADIUS), state, 2);
            }
        }
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

    private static List<BlockPos> collectBasinColumns() {
        List<BlockPos> columns = new ArrayList<>();
        for (int row = 0; row < LAYER_TWO.length; row++) {
            for (int column = 0; column < LAYER_TWO[row].length(); column++) {
                if (LAYER_TWO[row].charAt(column) == 'x') continue;
                columns.add(new BlockPos(column - RADIUS, 0, row - RADIUS));
            }
        }
        return List.copyOf(columns);
    }
    //endregion
}
