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
 * 运行期活动水系雕刻器（{@link WaterwaysCarver}）的全局注册表，每个包裹了 {@link WaterwaysDensity} 的维度
 * {@link RandomState}对应一个实例。
 * <ul>
 *   <li>以 {@link Waterways#MEANDER_NOISE} 派生出的 {@link NormalNoise} 实例作为映射键：同一维度 {@link RandomState} 下
 *       的所有区块密度函数副本共享同一个噪声对象引用（{@link NormalNoise} 按对象引用判等）。</li>
 *   <li>{@link #onLevelLoad} 在服务端维度加载（生成首个区块前）时为包含 {@link WaterwaysDensity} 的维度挂载雕刻器，并在
 *       非GameTest 环境下立即调用 {@link WaterwaysCarver#prefetch} 启动后台异步建图，避免玩家进入维度时阻塞主线程。
 *       </li>
 *   <li>雕刻器构建时优先读取维度存档中的 {@link WaterwaysData}（校验 {@link Waterways#PARAMETERS}、边界尺寸、海平面、世
 *       界种子及{@link #FINGERPRINT} 采样点地面与群系指纹），校验未命中时才执行完整水文规划并写回存档。</li>
 *   <li>维度卸载或服务端停止时自动移除雕刻器以释放内存；{@link #get} 供 {@link WaterwaysFeature} 与测试获取当前维度的雕
 *       刻器，测试也可通过 {@link #attach} 与 {@link #detach} 为自建的 {@link RandomState} 手动挂载或卸载雕刻器。</li>
 * </ul>
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
                String.valueOf(generator.generatorSettings().value().seaLevel()))
                + (region.keepOut() > 0 ? "/keep" + region.keepOut() : "") + (region.walled() ? "/walled" : "");
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
