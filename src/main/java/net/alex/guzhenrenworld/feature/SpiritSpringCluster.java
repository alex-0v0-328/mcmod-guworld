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
 * 元泉[Spirit Spring] 地物：从地物原点掷泉群，逐个找平地放石盆。
 *
 * <ul>
 *   <li>找平地 {@link SpiritSpringGround#findSurfaceGround}，放石盆 {@link SpiritSpringBasin#placeStructure}；
 *       {@link SpiritSpring} 定键与放置数据，{@link SpiritSpringRegistration} 注册。</li>
 *   <li>泉数权重 {@link #CLUSTER_SIZE_WEIGHTS}（掷值上限 {@link #CLUSTER_ROLL_BOUND}）与间距见 wiki《元泉生成》；石盆
 *       不重叠。</li>
 *   <li>全在原点切比雪夫距离 {@link #MAX_ORIGIN_OFFSET} 内：地物可越出所在区块一个区块读写，±9 让 15×15 检查与石盆都不
 *       出 3×3 区块窗口。</li>
 *   <li>{@link #findMemberSpot} 每成员试 {@link #MEMBER_SPOT_ATTEMPTS} 个候选，全不过即跳过（数量是尝试，非定额）；
 *       首个泉眼（{@link #findAnchorSpot}）先试原点列，再随机试至多 {@link #ANCHOR_SPOT_ATTEMPTS} 列：原点列单独过检的
 *       很少，搜索只多给列、不放宽规则，{@code /place feature} 也就落在玩家附近合格地面。</li>
 *   <li>⚠ 泉群数值由 Alex 定，不得私调。</li>
 * </ul>
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
