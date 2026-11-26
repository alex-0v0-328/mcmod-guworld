package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.IntBinaryOperator;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.blissfulland.FoxImmortalBlissfulLand;
import net.alex.guzhenrenworld.waterways.GroundHeights;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.slf4j.Logger;

/**
 * 狐仙福地挑址的单列取高基准（种子 {@link RegionProbe#SEED}，不挂水系雕刻器）。
 * <ul>
 *   <li>仅当 GameTest 服务端带 {@code -Dguworld.siteBenchmark=true} 时单独成批，常规检查免付这段时间。</li>
 *   <li>原点 ±{@link #REACH} 格、间距 {@link #STEP} 格的网格（每 4 区块一列，同挑址候选网格），单线程依次测
 *       {@code getBaseHeight}（{@code WORLD_SURFACE_WG}、{@code OCEAN_FLOOR_WG}）、{@code getFirstOccupiedHeight} 与
 *       {@link GroundHeights#sample}（刻河前最终密度自顶向下扫描）；先在错开半格的网格上空跑一轮预热，再计时。</li>
 *   <li>日志 {@code blissful land site heights} 记各法每列微秒数与按挑址取样数折算的一次挑址秒数；只在读取出错时失败。
 *       </li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
public final class BlissfulLandSiteBenchmark {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SWITCH = "guworld.siteBenchmark";
    private static final int REACH = 512;
    private static final int STEP = 64;

    private BlissfulLandSiteBenchmark() {}

    @GameTestGenerator
    public static List<TestFunction> generateBenchmark() {
        if (!Boolean.getBoolean(SWITCH)) return List.of();
        return List.of(new TestFunction("siteBenchmark", "blissfullandsitebenchmark",
                GuWorld.MOD_ID + ":empty9x9x9", 6000, 0, true, BlissfulLandSiteBenchmark::measure));
    }

    private static void measure(GameTestHelper helper) {
        RegionProbe probe = new RegionProbe(helper, FoxImmortalBlissfulLand.LEVEL_STEM, FoxImmortalBlissfulLand.MIN_Y,
                FoxImmortalBlissfulLand.HEIGHT);
        NoiseBasedChunkGenerator generator = probe.getGenerator();
        RandomState random = probe.getRandomState();
        LevelHeightAccessor heights = LevelHeightAccessor.create(FoxImmortalBlissfulLand.MIN_Y,
                FoxImmortalBlissfulLand.HEIGHT);
        GroundHeights ground = new GroundHeights(random.router(), FoxImmortalBlissfulLand.MIN_Y,
                FoxImmortalBlissfulLand.MIN_Y + FoxImmortalBlissfulLand.HEIGHT);
        IntBinaryOperator surface = (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                heights, random);
        IntBinaryOperator floor = (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights,
                random);
        IntBinaryOperator occupied = (x, z) -> generator.getFirstOccupiedHeight(x, z,
                Heightmap.Types.WORLD_SURFACE_WG, heights, random);
        IntBinaryOperator scan = ground::sample;
        for (IntBinaryOperator method : List.of(surface, floor, occupied, scan)) time(method, STEP / 2);
        double surfaceMicros = time(surface, 0);
        double floorMicros = time(floor, 0);
        double occupiedMicros = time(occupied, 0);
        double scanMicros = time(scan, 0);
        int columns = (2 * REACH / STEP + 1) * (2 * REACH / STEP + 1);
        LOGGER.info("blissful land site heights: {} columns, µs per column: getBaseHeight WORLD_SURFACE_WG {}, "
                        + "OCEAN_FLOOR_WG {}, getFirstOccupiedHeight {}, GroundHeights.sample {}", columns,
                "%.0f".formatted(surfaceMicros), "%.0f".formatted(floorMicros), "%.0f".formatted(occupiedMicros),
                "%.0f".formatted(scanMicros));
        helper.succeed();
    }

    private static double time(IntBinaryOperator method, int offset) {
        long started = System.nanoTime();
        int count = 0;
        long sum = 0;
        for (int z = -REACH; z <= REACH; z += STEP) {
            for (int x = -REACH; x <= REACH; x += STEP) {
                sum += method.applyAsInt(x + offset, z + offset);
                count++;
            }
        }
        if (sum == Long.MIN_VALUE) LOGGER.debug("unreachable {}", sum);
        return (System.nanoTime() - started) / 1e3 / count;
    }
}
