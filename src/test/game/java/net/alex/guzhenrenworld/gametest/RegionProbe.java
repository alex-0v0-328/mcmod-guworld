package net.alex.guzhenrenworld.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.stream.IntStream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * 区域测试取样列、高度、群系与路由值的生成器。读模组维度 JSON（{@code data/guworld/dimension}；GameTest
 * 服务端不加载模组维度），固定种子。每列耗一个噪声区块，即为该列重建整套路由，故一次 {@link #getColumn}
 * 扫描读出两种高度图，不按高度图各建一次。
 * <ul>
 *   <li>区域测试共用以原点为中心的 32×32 网格（间距 256 格，{@link #gridX}、{@link #gridZ}）；
 *       {@link #getGridColumns} 在公共 ForkJoin 池并行取列，一个区域从约 17 秒降到 2 秒左右。</li>
 *   <li>并行安全的依据：原版区块生成本就在多个工作线程上共用同一 {@link RandomState} 与密度函数；
 *       顺序敏感的统计（{@link WorldgenGolden}）仍按网格下标串行累加。</li>
 * </ul>
 */

public final class RegionProbe {

    public static final long SEED = 12345L;
    public static final int SAMPLES = 32;
    public static final int SPACING = 256;
    public static final int TOTAL = SAMPLES * SAMPLES;
    private static final int ORIGIN = -SAMPLES * SPACING / 2;

    private final ChunkGenerator generator;
    private final RandomState random;
    private final LevelHeightAccessor column;
    private final int minY;
    private final int height;

    public RegionProbe(GameTestHelper helper, ResourceKey<LevelStem> levelStem, int minY, int height) {
        this.minY = minY;
        this.height = height;
        RegistryAccess registries = helper.getLevel().registryAccess();
        ResourceLocation file = levelStem.location().withPath(path -> "dimension/" + path + ".json");
        try (Reader reader = helper.getLevel().getServer().getResourceManager().openAsReader(file)) {
            generator = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registries),
                    JsonParser.parseReader(reader))
                    .getOrThrow(message -> new IllegalStateException(levelStem.location() + " level stem: " + message))
                    .generator();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read " + file, exception);
        }
        NoiseGeneratorSettings settings = ((NoiseBasedChunkGenerator) generator).generatorSettings().value();
        random = RandomState.create(settings, registries.lookupOrThrow(Registries.NOISE), SEED);
        column = LevelHeightAccessor.create(minY, height);
    }

    public static int gridX(int index) {
        return ORIGIN + index % SAMPLES * SPACING;
    }

    public static int gridZ(int index) {
        return ORIGIN + index / SAMPLES * SPACING;
    }

    public NoiseColumn getColumn(int x, int z) {
        return generator.getBaseColumn(x, z, column, random);
    }

    public NoiseColumn[] getGridColumns() {
        return IntStream.range(0, TOTAL).parallel().mapToObj(index -> getColumn(gridX(index), gridZ(index)))
                .toArray(NoiseColumn[]::new);
    }

    public int getHeight(NoiseColumn noise, Heightmap.Types type) {
        for (int y = minY + height - 1; y >= minY; y--) {
            if (type.isOpaque().test(noise.getBlock(y))) return y + 1;
        }
        return minY;
    }

    public Holder<Biome> getBiome(int x, int y, int z) {
        return generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y),
                QuartPos.fromBlock(z), random.sampler());
    }

    public double getErosion(int x, int z) {
        return random.router().erosion().compute(new DensityFunction.SinglePointContext(x, 0, z));
    }

    public double getContinentalness(int x, int z) {
        return random.router().continents().compute(new DensityFunction.SinglePointContext(x, 0, z));
    }

    public double getTemperature(int x, int z) {
        return random.router().temperature().compute(new DensityFunction.SinglePointContext(x, 0, z));
    }

    public NoiseRouter getRouter() {
        return random.router();
    }

    public NoiseBasedChunkGenerator getGenerator() {
        return (NoiseBasedChunkGenerator) generator;
    }

    public RandomState getRandomState() {
        return random;
    }

    public int getSeaLevel() {
        return ((NoiseBasedChunkGenerator) generator).generatorSettings().value().seaLevel();
    }
}
