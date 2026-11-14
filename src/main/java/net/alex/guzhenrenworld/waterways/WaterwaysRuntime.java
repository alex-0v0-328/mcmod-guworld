package net.alex.guzhenrenworld.waterways;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.Nullable;

/**
 * 使用中的雕刻器：每个最终密度为 {@link WaterwaysDensity} 的 {@link RandomState} 一个。
 *
 * <ul>
 *   <li>以 {@link Waterways#MEANDER_NOISE} 实例为键：各副本共有的对象（{@link NormalNoise} 按同一性判等）。</li>
 *   <li>{@link #onLevelLoad} 在维度生成首个区块前给包了地形的服务端维度挂雕刻器并立即开建，玩家到时已建好，服务端线程不
 *       必等；GameTest 服务端待测试要用才建。</li>
 *   <li>建图先试维度的 {@link WaterwaysData}（键：{@link Waterways#PARAMETERS}、边长、海平面；
 *       并核对 {@link #FINGERPRINT} 各列地面与群系指纹），不符才规划并存档；规划规则一改就升
 *       {@link Waterways#PARAMETERS} 的版本号。</li>
 *   <li>维度卸载、停服时丢弃雕刻器；{@link #get} 供 {@link WaterwaysFeature} 与测试；测试可给自建随机状态挂摘雕刻
 *       器。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WaterwaysRuntime {

    private static final int[] FINGERPRINT = {-3072, -1024, 1024, 3072};
    private static final Map<NormalNoise, WaterwaysCarver> CARVERS = new ConcurrentHashMap<>();

    private WaterwaysRuntime() {}

    public static void attach(RandomState random, WaterwaysCarver carver) {
        CARVERS.put(random.getOrCreateNoise(Waterways.MEANDER_NOISE), carver);
    }

    public static void detach(RandomState random) {
        CARVERS.remove(random.getOrCreateNoise(Waterways.MEANDER_NOISE));
    }

    static @Nullable WaterwaysCarver find(@Nullable NormalNoise meander) {
        return meander == null ? null : CARVERS.get(meander);
    }

    public static @Nullable WaterwaysCarver get(ServerLevel level) {
        return CARVERS.get(level.getChunkSource().randomState().getOrCreateNoise(Waterways.MEANDER_NOISE));
    }

    static void onLevelLoad(ServerLevel level) {
        if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator)) return;
        RandomState random = level.getChunkSource().randomState();
        Optional<WaterwaysDensity> density = WaterwaysDensity.find(random.router());
        if (density.isEmpty()) return;
        Waterways.Region region = density.get().region();
        WaterwaysData data = level.getDataStorage().computeIfAbsent(WaterwaysData.factory(), WaterwaysData.NAME);
        long seed = level.getSeed();
        String key = String.join("/", Waterways.PARAMETERS, String.valueOf(region.borderSize()),
                String.valueOf(generator.generatorSettings().value().seaLevel()));
        WaterwaysCarver carver = new WaterwaysCarver(region, generator, random, ground -> {
            int[] fingerprint = getFingerprint(ground, generator, random);
            WaterwaysIndex saved = data.get(key, region, seed, fingerprint);
            if (saved != null) return saved;
            WaterwaysIndex built = WaterwaysIndex.of(WaterwaysPlanner.plan(region, generator, random, seed), ground,
                    random.getOrCreateNoise(Waterways.MEANDER_NOISE));
            data.put(key, seed, fingerprint, built);
            return built;
        });
        attach(random, carver);
        if (!(level.getServer() instanceof GameTestServer)) carver.prefetch();
    }

    static void onLevelUnload(ServerLevel level) {
        if (WaterwaysDensity.find(level.getChunkSource().randomState().router()).isPresent()) {
            detach(level.getChunkSource().randomState());
        }
    }

    static void clear() {
        CARVERS.clear();
    }

    private static int[] getFingerprint(GroundHeights ground, NoiseBasedChunkGenerator generator, RandomState random) {
        int points = FINGERPRINT.length * FINGERPRINT.length;
        int[] fingerprint = new int[2 * points];
        for (int point = 0; point < points; point++) {
            int x = FINGERPRINT[point % FINGERPRINT.length];
            int z = FINGERPRINT[point / FINGERPRINT.length];
            int height = ground.sample(x, z);
            fingerprint[point] = height;
            fingerprint[points + point] = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                    QuartPos.fromBlock(height), QuartPos.fromBlock(z), random.sampler()).unwrapKey()
                    .map(biome -> biome.location().toString().hashCode()).orElse(0);
        }
        return fingerprint;
    }
}
