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
 * The carvers in use, one per {@link RandomState} whose final density is a {@link WaterwaysDensity}, keyed by that
 * random state's instance of {@link Waterways#MEANDER_NOISE}, the one object every copy of the density function
 * carries ({@link NormalNoise} keeps identity equality).
 *
 * <p>{@link #onLevelLoad} attaches a carver to each loaded server level whose generator wraps its terrain, before the
 * level generates a chunk, and starts its build at once, so a player arriving later finds it ready instead of the
 * server thread waiting out a plan; a GameTest server only builds when a test asks. The build tries the dimension's
 * {@link WaterwaysData} first, keyed by {@link Waterways#PARAMETERS}, the border size and the sea level and checked
 * against a fingerprint of the ground and the biome at {@link #FINGERPRINT} columns, before planning the region and
 * saving the result. A change to the planner's own rules bumps the version in {@link Waterways#PARAMETERS}.
 * {@link #onLevelUnload} and {@link #clear} drop carvers with their levels; {@link #get} is a level's carver, for
 * {@link WaterwaysFeature} and tests. Tests attach their own carvers to random states they built ({@link #attach},
 * {@link #detach}).
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
