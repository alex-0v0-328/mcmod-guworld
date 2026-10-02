package net.alex.guzhenrenworld.feature;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenrenworld.registry.GuzhenrenBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BambooStalkBlock;
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
 * The Spirit Spring [元泉] structure, in two {@link Placement}s: on the surface and underground in
 * open caves. Each structure is a 7x7 basin laid out as three layers around the center column's
 * ground height g (surface: {@code MOTION_BLOCKING_NO_LEAVES} - 1; underground: the cave floor
 * found by the scan below); {@link #groundAt} dispatches between the two rules.
 *
 * <p>Layer one at g-1 buries the calcite basin; layer two at g paves the ground with slabs and
 * raises the calcite pillar under the spring; layer three at g+1 is only the source block. The
 * layer-two ring around the pillar is cleared to air ({@code a}), so the four streams fall into
 * the calcite basin and the rim holds the water instead of letting it sheet outward (Alex,
 * 2026-09-23). Outer {@code x} cells keep the original terrain (dirt stays dirt).
 * {@link #placeStructure} lays both grid layers and the source around the ground block at layer
 * two's level, first clearing {@link #CLEAR_HEIGHT_ABOVE} cells over every non-{@code x} column
 * (enough for double plants and snow layers); it is public for GameTests, which run on a
 * {@code ServerLevel}.
 *
 * <p>The spring itself is Guzhenren's block, reached by id through {@link GuzhenrenBlocks#SPIRIT_SPRING}
 * because this mod never compiles against Guzhenren; the first flow tick goes to the fluid the
 * block's default state carries, its source. Guzhenren keeps the block, fluid, item and rendering;
 * this structure moved here on 2026-10-02 (Alex).
 *
 * <p>Clusters (Alex, 2026-09-26): a spot that passes the rarity roll grows 1..5 springs -- 50%,
 * 20%, 15%, 10%, 5% ({@link #CLUSTER_SIZE_WEIGHTS}; {@link #clusterSizeForRoll} maps a [0,100)
 * roll through the cumulative weights) -- the same roll for surface and underground spots. Every
 * spring of one cluster sits 8..16 blocks (horizontal) from every other
 * ({@link #separationAcceptable}), so the 7x7 basins never overlap. Members are scattered around
 * the feature origin within the Chebyshev bound {@link #MAX_ORIGIN_OFFSET}: the origin sits inside
 * the generating chunk and features may write one chunk past its border, so ±12 keeps every 7x7
 * inside the 3x3 chunk window instead of clipping outer columns. {@link #memberSpot} tries
 * {@link #MEMBER_SPOT_ATTEMPTS} random candidates per member; a member whose candidates all fail
 * terrain checks is dropped -- the rolled size is the attempt, not a quota.
 *
 * <p>Surface placement ({@link #surfaceGround}) happens on flat land only (Alex, 2026-09-24):
 * vegetation runs before us ({@code VEGETAL_DECORATION} precedes {@code TOP_LAYER_MODIFICATION}),
 * so above every non-{@code x} column the two cells over the ground are cleared to air. Logs and
 * leaves, fluids, and any structure column whose own surface height differs from the center's
 * veto the spot ({@link #unevenTerrain}) -- slopes, ponds and trees all move the heightmap, so
 * bumps never leave plants floating above the carve -- and stalk plants that dodge the heightmap
 * (no collision: bamboo, sugar cane) veto from the clear band.
 *
 * <p>Underground placement ({@link #caveGround}, 2026-09-26) runs at
 * {@code UNDERGROUND_DECORATION} with a uniform height sample; the column scan starts just below
 * that sample (never nearer than {@link #UNDERGROUND_SURFACE_GUARD} under the surface, so open air
 * never qualifies) and walks down {@link #UNDERGROUND_SCAN_DEPTH} cells for an enclosed cave floor
 * ({@link #caveFloorAt}): sturdy ground under the whole footprint, the two-cell clear band holding
 * only air or replaceable plants ({@link #clearable}) -- never fluids, so cave lakes and lava veto
 * the floor -- and solid ceiling within {@link #UNDERGROUND_CEILING_SCAN} cells over the clear
 * band ({@link #enclosed}), so ravines open to the sky fail. Its rarity sits strictly above the
 * surface roll (DatapackProvider), per Alex's "lower than the surface, still rare" constraint.
 *
 * <p>⚠ Layout, rarity and the cluster numbers are Alex's picks (2026-09-23, 2026-09-24,
 * 2026-09-26), not silent tunables. Symbol legend: {@code x}=keep, {@code a}=air (the basin well),
 * {@code y}=cobblestone, {@code t}=mossy cobblestone, {@code f}=calcite, {@code c}=cobblestone
 * slab (bottom), {@code m}=mossy cobblestone slab (bottom).
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class SpiritSpringFeature extends Feature<NoneFeatureConfiguration> {

    public enum Placement { SURFACE, UNDERGROUND }

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

    //region Cluster [丛生] -- the size roll and spacing (Alex, 2026-09-26)
    static final int[] CLUSTER_SIZE_WEIGHTS = { 50, 20, 15, 10, 5 };
    static final int CLUSTER_MIN_SEPARATION = 8;
    static final int CLUSTER_MAX_SEPARATION = 16;
    private static final int MEMBER_SPOT_ATTEMPTS = 32;
    private static final int MAX_ORIGIN_OFFSET = 12;
    //endregion

    //region Underground scan [地下扫描]
    private static final int UNDERGROUND_SCAN_DEPTH = 24;
    private static final int UNDERGROUND_CEILING_SCAN = 16;
    private static final int UNDERGROUND_SURFACE_GUARD = 4;
    //endregion

    private final Placement placement;

    public SpiritSpringFeature(Placement placement) {
        super(NoneFeatureConfiguration.CODEC);
        this.placement = placement;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockPos anchor = groundAt(level, origin.getX(), origin.getZ(), origin.getY());
        if (anchor == null) return false;
        int clusterSize = clusterSizeForRoll(random.nextInt(100));
        placeStructure(level, anchor);
        List<BlockPos> placed = new ArrayList<>();
        placed.add(anchor);
        for (int member = 1; member < clusterSize; member++) {
            BlockPos spot = memberSpot(level, origin, anchor, placed, random);
            if (spot == null) continue;
            placeStructure(level, spot);
            placed.add(spot);
        }
        return true;
    }

    static int clusterSizeForRoll(int roll) {
        int cumulative = 0;
        for (int size = 0; size < CLUSTER_SIZE_WEIGHTS.length; size++) {
            cumulative += CLUSTER_SIZE_WEIGHTS[size];
            if (roll < cumulative) return size + 1;
        }
        return CLUSTER_SIZE_WEIGHTS.length;
    }

    static boolean separationAcceptable(int dx, int dz) {
        double distance = Math.sqrt((double) dx * dx + (double) dz * dz);
        return distance >= CLUSTER_MIN_SEPARATION && distance <= CLUSTER_MAX_SEPARATION;
    }

    private @Nullable BlockPos memberSpot(WorldGenLevel level, BlockPos origin, BlockPos anchor,
                                          List<BlockPos> placed, RandomSource random) {
        for (int attempt = 0; attempt < MEMBER_SPOT_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double reach = CLUSTER_MIN_SEPARATION
                    + random.nextDouble() * (CLUSTER_MAX_SEPARATION - CLUSTER_MIN_SEPARATION + 1.0);
            int dx = (int) Math.round(reach * Math.cos(angle));
            int dz = (int) Math.round(reach * Math.sin(angle));
            if (Math.abs(dx) > MAX_ORIGIN_OFFSET || Math.abs(dz) > MAX_ORIGIN_OFFSET) continue;
            boolean spaced = true;
            for (BlockPos spring : placed) {
                if (!separationAcceptable(origin.getX() + dx - spring.getX(),
                        origin.getZ() + dz - spring.getZ())) {
                    spaced = false;
                    break;
                }
            }
            if (!spaced) continue;
            BlockPos ground = groundAt(level, origin.getX() + dx, origin.getZ() + dz, anchor.getY());
            if (ground != null) return ground;
        }
        return null;
    }

    private @Nullable BlockPos groundAt(WorldGenLevel level, int x, int z, int hintY) {
        return switch (placement) {
            case SURFACE -> surfaceGround(level, x, z);
            case UNDERGROUND -> caveGround(level, x, z, hintY);
        };
    }

    private static @Nullable BlockPos surfaceGround(WorldGenLevel level, int x, int z) {
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        BlockPos groundCenter = new BlockPos(x, groundY, z);
        BlockState ground = level.getBlockState(groundCenter);
        if (ground.is(BlockTags.LOGS) || ground.is(BlockTags.LEAVES)) return null;
        if (!level.getFluidState(groundCenter).isEmpty()
                || !level.getFluidState(groundCenter.above()).isEmpty()) return null;
        if (unevenTerrain(level, groundCenter)) return null;
        return groundCenter;
    }

    private static @Nullable BlockPos caveGround(WorldGenLevel level, int x, int z, int hintY) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        int top = Math.min(hintY, surfaceY - UNDERGROUND_SURFACE_GUARD);
        int bottom = Math.max(level.getMinBuildHeight() + 1, top - UNDERGROUND_SCAN_DEPTH);
        for (int groundY = top; groundY >= bottom; groundY--) {
            if (caveFloorAt(level, x, groundY, z)) return new BlockPos(x, groundY, z);
        }
        return null;
    }

    private static boolean caveFloorAt(WorldGenLevel level, int x, int groundY, int z) {
        if (!sturdyFloor(level, x, groundY, z)) return false;
        if (!enclosed(level, x, groundY, z)) return false;
        for (int row = 0; row < LAYER_TWO.length; row++) {
            for (int column = 0; column < LAYER_TWO[row].length(); column++) {
                if (LAYER_TWO[row].charAt(column) == 'x') continue;
                int cellX = x + column - RADIUS;
                int cellZ = z + row - RADIUS;
                if (!sturdyFloor(level, cellX, groundY, cellZ)) return false;
                for (int dy = 1; dy <= CLEAR_HEIGHT_ABOVE; dy++) {
                    if (!clearable(level, cellX, groundY + dy, cellZ)) return false;
                }
            }
        }
        return true;
    }

    private static boolean sturdyFloor(WorldGenLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
    }

    private static boolean clearable(WorldGenLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && (state.isAir() || state.canBeReplaced());
    }

    private static boolean enclosed(WorldGenLevel level, int x, int groundY, int z) {
        for (int dy = CLEAR_HEIGHT_ABOVE + 1; dy <= CLEAR_HEIGHT_ABOVE + UNDERGROUND_CEILING_SCAN; dy++) {
            BlockPos pos = new BlockPos(x, groundY + dy, z);
            if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.DOWN)) return true;
        }
        return false;
    }

    private static boolean unevenTerrain(WorldGenLevel level, BlockPos groundCenter) {
        for (int row = 0; row < LAYER_TWO.length; row++) {
            for (int column = 0; column < LAYER_TWO[row].length(); column++) {
                if (LAYER_TWO[row].charAt(column) == 'x') continue;
                int x = groundCenter.getX() + column - RADIUS;
                int z = groundCenter.getZ() + row - RADIUS;
                if (level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1
                        != groundCenter.getY()) return true;
                for (int dy = 1; dy <= CLEAR_HEIGHT_ABOVE; dy++) {
                    BlockState above = level.getBlockState(
                            groundCenter.offset(column - RADIUS, dy, row - RADIUS));
                    if (above.getBlock() instanceof BambooStalkBlock
                            || above.getBlock() instanceof SugarCaneBlock) return true;
                }
            }
        }
        return false;
    }

    public static void placeStructure(LevelAccessor level, BlockPos groundCenter) {
        placeLayer(level, groundCenter.below(), LAYER_ONE);
        placeLayer(level, groundCenter, LAYER_TWO);
        for (int row = 0; row < LAYER_TWO.length; row++) {
            for (int column = 0; column < LAYER_TWO[row].length(); column++) {
                if (LAYER_TWO[row].charAt(column) == 'x') continue;
                for (int dy = 1; dy <= CLEAR_HEIGHT_ABOVE; dy++) {
                    level.setBlock(groundCenter.offset(column - RADIUS, dy, row - RADIUS),
                            Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        BlockPos spring = groundCenter.above();
        BlockState source = GuzhenrenBlocks.SPIRIT_SPRING.get().defaultBlockState();
        level.setBlock(spring, source, 2);
        level.scheduleTick(spring, source.getFluidState().getType(), 0);
    }

    private static void placeLayer(LevelAccessor level, BlockPos layerCenter, String[] grid) {
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length(); column++) {
                BlockState state = stateFor(grid[row].charAt(column));
                if (state == null) continue;
                level.setBlock(layerCenter.offset(column - RADIUS, 0, row - RADIUS), state, 2);
            }
        }
    }

    static @Nullable BlockState stateFor(char symbol) {
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
}
