package net.alex.guzhenrenworld.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.blissfulland.FoxImmortalBlissfulLand;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * 狐仙福地地形的可选噪声填充基准（Tectonic 副本加平地圈、湖与水系包装，不挂雕刻器）。
 * <ul>
 *   <li>仅当 GameTest 服务端带 {@code -Dguworld.noiseBenchmark=true} 时单独成批，常规检查免付这 10 秒。</li>
 *   <li>同 {@link BlissfulLandTerrainGameTests} 读模组维度 JSON 生成器，种子 12345；疆域中部 8×8 区块（间距 1024 格）
 *       {@code fillFromNoise} 进新 {@link ProtoChunk}；不混合，结构管理器无起点，beardifier 与区块存储不计时。</li>
 *   <li>空跑两轮预热 JIT（只空跑一轮时首轮慢 1/3），再计时三轮，记每区块毫秒数与全部 {@code WORLD_SURFACE_WG}
 *       高度的指纹，三轮不一致才失败；改地形前后对比此日志：指纹同即地表同，毫秒数即开销。</li>
 *   <li>同轮交替再测一份去掉福地包装的同一路由（{@link #readBareGenerator}），两者之差即包装开销。</li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
public final class BlissfulLandNoiseBenchmark {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SWITCH = "guworld.noiseBenchmark";
    private static final int GRID = 8;
    private static final int SPACING = 64;
    private static final int ORIGIN = (1 - GRID) * SPACING / 2;
    private static final int WARMUP_ROUNDS = 2;
    private static final int ROUNDS = 3;
    private static final long SEED = 12345L;
    private static final List<String> BARE_FIELDS = List.of("final_density", "initial_density_without_jaggedness",
            "continents", "fluid_level_floodedness");

    private BlissfulLandNoiseBenchmark() {}

    @GameTestGenerator
    public static List<TestFunction> generateBenchmark() {
        if (!Boolean.getBoolean(SWITCH)) return List.of();
        return List.of(new TestFunction("noiseBenchmark", "blissfullandnoisebenchmark",
                GuWorld.MOD_ID + ":empty9x9x9", 100, 0, true, BlissfulLandNoiseBenchmark::measure));
    }

    private static void measure(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        NoiseBasedChunkGenerator generator = readGenerator(helper);
        NoiseBasedChunkGenerator bare = readBareGenerator(helper, generator);
        RandomState random = RandomState.create(generator.generatorSettings().value(),
                registries.lookupOrThrow(Registries.NOISE), SEED);
        RandomState bareRandom = RandomState.create(bare.generatorSettings().value(),
                registries.lookupOrThrow(Registries.NOISE), SEED);
        Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            fill(generator, random, biomes);
            fill(bare, bareRandom, biomes);
        }
        long[] fingerprints = new long[ROUNDS];
        double[] millis = new double[ROUNDS];
        double[] bareMillis = new double[ROUNDS];
        for (int round = 0; round < ROUNDS; round++) {
            long started = System.nanoTime();
            fingerprints[round] = fill(generator, random, biomes);
            millis[round] = (System.nanoTime() - started) / 1e6 / (GRID * GRID);
            started = System.nanoTime();
            fill(bare, bareRandom, biomes);
            bareMillis[round] = (System.nanoTime() - started) / 1e6 / (GRID * GRID);
        }
        LOGGER.info("blissful land noise fill: {} chunks, ms per chunk {}, best {}, fingerprint {}; bare Tectonic copy "
                        + "{}, best {}", GRID * GRID, Arrays.stream(millis).mapToObj("%.1f"::formatted).toList(),
                "%.1f".formatted(Arrays.stream(millis).min().orElseThrow()), Long.toHexString(fingerprints[0]),
                Arrays.stream(bareMillis).mapToObj("%.1f"::formatted).toList(),
                "%.1f".formatted(Arrays.stream(bareMillis).min().orElseThrow()));

        helper.assertTrue(Arrays.stream(fingerprints).allMatch(value -> value == fingerprints[0]),
                "the rounds filled different surfaces: " + Arrays.toString(fingerprints));
        helper.succeed();
    }

    private static NoiseBasedChunkGenerator readGenerator(GameTestHelper helper) {
        ResourceLocation file = FoxImmortalBlissfulLand.LEVEL_STEM.location()
                .withPath(path -> "dimension/" + path + ".json");
        try (Reader reader = helper.getLevel().getServer().getResourceManager().openAsReader(file)) {
            return (NoiseBasedChunkGenerator) LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,
                    helper.getLevel().registryAccess()), JsonParser.parseReader(reader))
                    .getOrThrow(message -> new IllegalStateException("blissful land level stem: " + message))
                    .generator();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read " + file, exception);
        }
    }

    // 同一份噪声设置，只把路由里福地加的包装（平地圈、湖、水系、截断的大陆度与压低的淹没度）换回移植来的具名函数。
    private static NoiseBasedChunkGenerator readBareGenerator(GameTestHelper helper,
            NoiseBasedChunkGenerator generator) {
        ResourceLocation file = FoxImmortalBlissfulLand.NOISE_SETTINGS.location()
                .withPath(path -> "worldgen/noise_settings/" + path + ".json");
        try (Reader reader = helper.getLevel().getServer().getResourceManager().openAsReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject router = json.getAsJsonObject("noise_router");
            for (String field : BARE_FIELDS) {
                router.addProperty(field, GuWorld.MOD_ID + ":terrain/" + FoxImmortalBlissfulLand.LEVEL.location()
                        .getPath() + "/minecraft/overworld/noise_router/" + field);
            }
            NoiseGeneratorSettings settings = NoiseGeneratorSettings.DIRECT_CODEC.parse(RegistryOps.create(
                    JsonOps.INSTANCE, helper.getLevel().registryAccess()), json)
                    .getOrThrow(message -> new IllegalStateException("bare blissful noise settings: " + message));
            return new NoiseBasedChunkGenerator(generator.getBiomeSource(), Holder.direct(settings));
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read " + file, exception);
        }
    }

    private static long fill(NoiseBasedChunkGenerator generator, RandomState random, Registry<Biome> biomes) {
        LevelHeightAccessor column = LevelHeightAccessor.create(FoxImmortalBlissfulLand.MIN_Y,
                FoxImmortalBlissfulLand.HEIGHT);
        StructureManager structures = new StructureManager(null, null, null) {

            @Override
            public @NotNull List<StructureStart> startsForStructure(@NotNull ChunkPos position,
                    @NotNull Predicate<Structure> predicate) {
                return List.of();
            }
        };
        long fingerprint = 1;
        for (int i = 0; i < GRID * GRID; i++) {
            ChunkPos position = new ChunkPos(ORIGIN + i % GRID * SPACING, ORIGIN + i / GRID * SPACING);
            ProtoChunk chunk = new ProtoChunk(position, UpgradeData.EMPTY, column, biomes, null);
            generator.fillFromNoise(Blender.empty(), random, structures, chunk).join();
            for (int j = 0; j < 256; j++) {
                fingerprint = fingerprint * 31 + chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, j & 15, j >> 4);
            }
        }
        return fingerprint;
    }
}
