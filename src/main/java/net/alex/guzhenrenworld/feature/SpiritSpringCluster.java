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
 * 元泉丛生地物实现：从地物原点抽取泉群规模，并逐个搜寻合格平地放置石盆。
 * <ul>
 *   <li>由 {@link SpiritSpring} 定义配置与放置规则、{@link SpiritSpringRegistration} 注册地物实例；放置时调用
 *       {@link SpiritSpringGround#findSurfaceGround} 校验地面平整度，并通过 {@link SpiritSpringBasin#placeStructure} 构
 *       筑石盆。</li>
 *   <li>首个锚点泉眼（{@link #findAnchorSpot}）优先检验地物原点列，未通过时在半径范围内随机尝试至多
 *       {@link #ANCHOR_SPOT_ATTEMPTS} 列，既提高自然生成命中率，也保证执行 {@code /place feature} 时能在附近找到合格地
 *       面。</li>
 *   <li>锚点确定后按权重表 {@link #CLUSTER_SIZE_WEIGHTS}（总权重 {@link #CLUSTER_ROLL_BOUND}）抽取泉群数量；其余成员泉
 *       眼通过{@link #findMemberSpot} 在距锚点 {@link #CLUSTER_MAX_REACH} 格以内、且与已有泉眼中心间距不小于
 *       {@link #CLUSTER_MIN_SEPARATION} 格的环带内至多尝试 {@link #MEMBER_SPOT_ATTEMPTS} 次，确保 7×7 石盆互不重叠。
 *       </li>
 *   <li>⚠ 所有候选中心点与地物原点的切比雪夫距离严禁超过 {@link #MAX_ORIGIN_OFFSET}（9 格），以保证石盆及 15×15 平整
 *       度检查完全落在原版地物生成允许读写的 3×3 区块窗口（原点所在区块外扩 1 个区块）内，越界读写会触发区块访问死锁或
 *       异常。</li>
 * </ul>
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
