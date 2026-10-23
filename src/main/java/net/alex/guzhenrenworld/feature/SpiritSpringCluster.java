package net.alex.guzhenrenworld.feature;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * The Spirit Spring [元泉] feature: from one feature origin it rolls a cluster of springs, finds level ground for each
 * ({@link SpiritSpringGround#findSurfaceGround}) and lays a basin there ({@link SpiritSpringBasin#placeStructure}).
 * {@link SpiritSpring} keys it and writes its placement data; {@link SpiritSpringRegistration} registers it.
 *
 * <p>Clusters: a spot that passes the rarity roll grows 1 to 5 springs -- 50%,
 * 20%, 15%, 10%, 5% ({@link #CLUSTER_SIZE_WEIGHTS}; {@link #getClusterSize} maps a roll under
 * {@link #CLUSTER_ROLL_BOUND} through the cumulative weights). The members ring the cluster's first spring, 8 to 10
 * blocks (horizontal) from it ({@link #isWithinReach}) and at least 8 from every other
 * ({@link #isSeparationAcceptable}), so the 7x7 basins never overlap; a whole cluster spans about 25x25 blocks.
 * Every spring stays within the Chebyshev bound {@link #MAX_ORIGIN_OFFSET} of the feature origin
 * ({@link #isWithinOriginBound}): the origin sits inside the generating chunk and features may read and write one
 * chunk past its border, so ±9 keeps every 15x15 flatness check, and with it every 7x7 basin, inside the 3x3 chunk
 * window. {@link #findMemberSpot} tries {@link #MEMBER_SPOT_ATTEMPTS} random candidates per member; a member whose
 * candidates all fail terrain checks is dropped -- the rolled size is the attempt, not a quota.
 *
 * <p>Anchor search: {@link #findAnchorSpot} tries the origin column, then up to
 * {@link #ANCHOR_SPOT_ATTEMPTS} random columns within the same ±{@link #MAX_ORIGIN_OFFSET} bound.
 * The terrain rules alone would let few rolls through at the origin column; the search keeps the rules and only
 * gives each roll more columns. A {@code /place feature} likewise lands on qualifying ground within that bound of
 * the player instead of failing on the column underfoot.
 *
 * <p>⚠ The cluster numbers are Alex's picks, not silent tunables.
 *
 * @author Alex
 * @version 1.0.0
 * @see SpiritSpringBasin
 * @since 1.0.0
 */

public class SpiritSpringCluster extends Feature<NoneFeatureConfiguration> {

    public SpiritSpringCluster() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockPos anchor = findAnchorSpot(level, origin, random);
        if (anchor == null) return false;
        int clusterSize = getClusterSize(random.nextInt(CLUSTER_ROLL_BOUND));
        SpiritSpringBasin.placeStructure(level, anchor);
        List<BlockPos> placed = new ArrayList<>();
        placed.add(anchor);
        for (int member = 1; member < clusterSize; member++) {
            BlockPos spot = findMemberSpot(level, origin, anchor, placed, random);
            if (spot == null) continue;
            SpiritSpringBasin.placeStructure(level, spot);
            placed.add(spot);
        }
        return true;
    }

    //region Cluster [丛生] -- the size roll, the spacing and the member search
    static final int[] CLUSTER_SIZE_WEIGHTS = { 50, 20, 15, 10, 5 };
    static final int CLUSTER_ROLL_BOUND = 100;
    static final int CLUSTER_MIN_SEPARATION = 8;
    static final int CLUSTER_MAX_REACH = 10;
    private static final int MEMBER_SPOT_ATTEMPTS = 32;
    private static final int MAX_ORIGIN_OFFSET = 9;

    static int getClusterSize(int roll) {
        int cumulative = 0;
        for (int index = 0; index < CLUSTER_SIZE_WEIGHTS.length; index++) {
            cumulative += CLUSTER_SIZE_WEIGHTS[index];
            if (roll < cumulative) return index + 1;
        }
        return CLUSTER_SIZE_WEIGHTS.length;
    }

    static boolean isSeparationAcceptable(int offsetX, int offsetZ) {
        return getDistance(offsetX, offsetZ) >= CLUSTER_MIN_SEPARATION;
    }

    static boolean isWithinReach(int offsetX, int offsetZ) {
        return getDistance(offsetX, offsetZ) <= CLUSTER_MAX_REACH;
    }

    private static double getDistance(int offsetX, int offsetZ) {
        return Math.sqrt((double) offsetX * offsetX + (double) offsetZ * offsetZ);
    }

    private static boolean isWithinOriginBound(BlockPos origin, int x, int z) {
        return Math.abs(x - origin.getX()) <= MAX_ORIGIN_OFFSET && Math.abs(z - origin.getZ()) <= MAX_ORIGIN_OFFSET;
    }

    private static @Nullable BlockPos findMemberSpot(WorldGenLevel level, BlockPos origin, BlockPos anchor,
            List<BlockPos> placed, RandomSource random) {
        for (int attempt = 0; attempt < MEMBER_SPOT_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double reach = CLUSTER_MIN_SEPARATION
                    + random.nextDouble() * (CLUSTER_MAX_REACH - CLUSTER_MIN_SEPARATION);
            int x = anchor.getX() + (int) Math.round(reach * Math.cos(angle));
            int z = anchor.getZ() + (int) Math.round(reach * Math.sin(angle));
            if (!isWithinOriginBound(origin, x, z)) continue;
            if (!isWithinReach(x - anchor.getX(), z - anchor.getZ())) continue;
            boolean spaced = placed.stream()
                    .allMatch(spring -> isSeparationAcceptable(x - spring.getX(), z - spring.getZ()));
            if (!spaced) continue;
            BlockPos groundCenter = SpiritSpringGround.findSurfaceGround(level, x, z);
            if (groundCenter != null) return groundCenter;
        }
        return null;
    }
    //endregion

    //region Anchor search [就近找点] -- the origin column first, then random columns within the cluster bound
    private static final int ANCHOR_SPOT_ATTEMPTS = 32;

    private static @Nullable BlockPos findAnchorSpot(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos groundCenter = SpiritSpringGround.findSurfaceGround(level, origin.getX(), origin.getZ());
        for (int attempt = 0; groundCenter == null && attempt < ANCHOR_SPOT_ATTEMPTS; attempt++) {
            int offsetX = random.nextIntBetweenInclusive(-MAX_ORIGIN_OFFSET, MAX_ORIGIN_OFFSET);
            int offsetZ = random.nextIntBetweenInclusive(-MAX_ORIGIN_OFFSET, MAX_ORIGIN_OFFSET);
            groundCenter = SpiritSpringGround.findSurfaceGround(level, origin.getX() + offsetX,
                    origin.getZ() + offsetZ);
        }
        return groundCenter;
    }
    //endregion
}
