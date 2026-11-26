package net.alex.guzhenrenworld.dimension.blissfulland;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;
import net.alex.guzhenrenworld.waterways.GroundHeights;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * 狐仙福地荡魂山的落位：在原点 ±{@link #REACH_CHUNKS} 区块内挑一处缓地作山心区块，并给出山底高度；只依赖世界种子与生成
 * 器，由 {@link BoundedNoiseGenerator#getSite} 按维度记忆。
 * <ul>
 *   <li>候选：每 {@link #CANDIDATE_STEP} 区块一个区块（17 × 17 = 289 个），山心是区块中心方块；地面取刻河前最终密度自顶向
 *       下扫描的高度（{@link GroundHeights}，实测约 160 µs/列，{@code getBaseHeight} 约 20 ms/列，后者一次挑址要几十
 *       秒）。全部取样落在间距 {@link #LATTICE} 格、对准区块中心的格点上，相邻候选共用，共
 *       {@code (2 × (512 + 96) / 32 + 1)²} = 1,521 列。</li>
 *   <li>每个候选看山心 {@link #FOOT_RADIUS} 格内的 29 个格点：地面低于海平面的格点算有水（含水层与湖都只在那里放水，
 *       溪流另由水系禁区绕开山心）；起伏 = 最高减最低；山底 = 山心周围 3 × 3 个格点的中位数。</li>
 *   <li>合格：无水、起伏不超过 {@link #MAX_RELIEF} 格、山底不高于 {@link #MAX_FLOOR}（山高 117，山巅不超建造上限）。合格
 *       的按起伏分档（每 {@link #RELIEF_BAND} 格一档）取最缓的一档，同档取离原点近的，再同距按先北后西；全不合格时依次比
 *       有水格点数、山底超限格数、起伏、离原点距离，取最不差的。</li>
 *   <li>⚠ 地面不经水系雕刻：雕刻要等水系建图（新档几十秒），而水系禁区反过来以本落位为心，见
 *       {@link BoundedNoiseGenerator}。</li>
 * </ul>
 */

public record BlissfulLandSite(ChunkPos chunk, int floor, int relief, int wet) {

    public static final int REACH_CHUNKS = 32;
    public static final int CANDIDATE_STEP = 4;
    public static final int LATTICE = 32;
    public static final int FOOT_RADIUS = 96;
    public static final int MAX_RELIEF = 24;
    public static final int RELIEF_BAND = 8;
    public static final int MAX_FLOOR = 190;
    private static final int OFFSET = 8;
    private static final int REACH = REACH_CHUNKS * 16 + FOOT_RADIUS;
    private static final int SIDE = 2 * REACH / LATTICE + 1;
    private static final int FOOT = FOOT_RADIUS / LATTICE;

    public BlockPos centre() { return new BlockPos(chunk.getMiddleBlockX(), floor, chunk.getMiddleBlockZ()); }

    public boolean isGentle() { return wet == 0 && relief <= MAX_RELIEF && floor <= MAX_FLOOR; }

    public static BlissfulLandSite pick(NoiseGeneratorSettings settings, RandomState random) {
        NoiseSettings noise = settings.noiseSettings();
        GroundHeights heights = new GroundHeights(random.router(), noise.minY(), noise.minY() + noise.height());
        int[] ground = IntStream.range(0, SIDE * SIDE)
                .map(index -> heights.sample(toBlock(index % SIDE), toBlock(index / SIDE))).toArray();
        int seaLevel = settings.seaLevel();
        return IntStream.rangeClosed(-REACH_CHUNKS / CANDIDATE_STEP, REACH_CHUNKS / CANDIDATE_STEP).boxed()
                .flatMap(row -> IntStream.rangeClosed(-REACH_CHUNKS / CANDIDATE_STEP, REACH_CHUNKS / CANDIDATE_STEP)
                        .mapToObj(column -> rate(ground, seaLevel, new ChunkPos(column * CANDIDATE_STEP,
                                row * CANDIDATE_STEP))))
                .min(ORDER).orElseThrow();
    }

    private static BlissfulLandSite rate(int[] ground, int seaLevel, ChunkPos chunk) {
        int column = (chunk.getMiddleBlockX() + REACH - OFFSET) / LATTICE;
        int row = (chunk.getMiddleBlockZ() + REACH - OFFSET) / LATTICE;
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        int wet = 0;
        for (int dz = -FOOT; dz <= FOOT; dz++) {
            for (int dx = -FOOT; dx <= FOOT; dx++) {
                if (dx * dx + dz * dz > FOOT * FOOT) continue;

                int height = ground[(row + dz) * SIDE + column + dx];
                lowest = Math.min(lowest, height);
                highest = Math.max(highest, height);
                if (height < seaLevel) wet++;
            }
        }
        int[] middle = new int[9];
        for (int i = 0; i < middle.length; i++) {
            middle[i] = ground[(row + i / 3 - 1) * SIDE + column + i % 3 - 1];
        }
        Arrays.sort(middle);
        return new BlissfulLandSite(chunk, middle[4], highest - lowest, wet);
    }

    private static int toBlock(int index) { return index * LATTICE - REACH + OFFSET; }

    private static long getDistance(BlissfulLandSite site) {
        long x = site.chunk.getMiddleBlockX();
        long z = site.chunk.getMiddleBlockZ();
        return x * x + z * z;
    }

    private static final Comparator<BlissfulLandSite> ORDER = Comparator
            .comparing((BlissfulLandSite site) -> !site.isGentle())
            .thenComparingInt(site -> site.isGentle() ? site.relief / RELIEF_BAND : 0)
            .thenComparingInt(site -> site.wet)
            .thenComparingInt(site -> Math.max(0, site.floor - MAX_FLOOR))
            .thenComparingInt(site -> site.isGentle() ? 0 : site.relief)
            .thenComparingLong(BlissfulLandSite::getDistance)
            .thenComparingInt(site -> site.chunk.z)
            .thenComparingInt(site -> site.chunk.x);
}
