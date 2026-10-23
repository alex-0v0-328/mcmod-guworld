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
 * Where a Spirit Spring [元泉] basin may stand: level, dry, treeless ground, judged by {@link #findSurfaceGround}
 * (public for GameTests) for each spot {@link SpiritSpringCluster} tries.
 *
 * <p>Surface placement happens on level, dry land only, on a mountain top, at a mountain foot or on a plain alike:
 * the ground of the 15x15 square around the center (radius {@link #FLAT_RADIUS}, about twice the basin's side) may
 * differ by at most {@link SpiritSpringBasin#MAX_RISE} blocks from its lowest to its highest column
 * ({@link #isTerrainRough}), the rise the basin's leveling absorbs. Vegetation runs before us
 * ({@code VEGETAL_DECORATION} precedes {@code TOP_LAYER_MODIFICATION}), so slopes, ponds and trees all move the
 * heightmap the check reads: a tree within the square vetoes it, and so do logs and leaves at the center. Stalk
 * plants that dodge the heightmap (no collision: bamboo, sugar cane) veto from the basin's clear band
 * ({@link SpiritSpringBasin#CLEAR_HEIGHT_ABOVE}). {@link #isTerrainWet} vetoes any structure column that is ice or
 * holds a fluid in its ground or the cell above: water level with the ground passes the height rule, and vanilla's
 * {@code freeze_top_layer} runs earlier in the same step (a biome modifier appends after the biome's own features),
 * so a frozen pond or river is flat ice with no fluid at the surface. The cluster's searches reach past the biome
 * filter, into rivers and frozen oceans as well.
 *
 * <p>⚠ The terrain checks reach up to ±16 blocks into neighbor chunks, which may not have grown their trees
 * yet, so whether a spot qualifies can depend on the order chunks generate in, as vanilla tree placement
 * does.
 *
 * <p>⚠ The flatness is Alex's pick, not a silent tunable.
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
