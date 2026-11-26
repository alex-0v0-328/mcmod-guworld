package net.alex.guzhenrenworld.dimension.blissfulland;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.waterways.Waterways;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 有疆域的噪声生成器（{@code guworld:bounded_noise}）：以原点为中心、边长 2 × {@link #halfSizeChunks} 个区块的正方形
 * 内照常按噪声生成，界外区块整块留空（虚空），用于福地。
 * <ul>
 *   <li>疆域边线落在区块边界上，所以按区块判定就精确到格；界外区块跳过噪声填充、表层、雕刻、装饰和初始生物，不花生成
 *       时间，也不会有含水层在虚空里按海平面灌水（只靠密度函数做遮罩时，界外低于海平面的空格会被灌满）。</li>
 *   <li>装饰阶段能写相邻区块：紧挨界外的界内区块经 {@link BoundedLevel} 装饰，界外读作默认方块、写入丢弃，泉、晶洞、
 *       树冠都不会漏进虚空。</li>
 *   <li>界外区块的高度查询（{@code getBaseHeight}）仍按噪声返回，只影响结构放置判断，福地除荡魂山外没有结构。</li>
 *   <li>荡魂山落位（{@link #getSite}）：每个 {@link RandomState}（一个维度一份）记一次 {@link BlissfulLandSite#pick} 的
 *       结果，多线程首次同时取时只算一次、其余等它；{@link #createState}（建维度时原版调用）先把该 {@link RandomState}
 *       登记给本生成器（{@link #of}，结构放置器 {@code guworld:gentle_site} 只拿得到结构状态，经它找回生成器），再在后台
 *       线程开算，起服后第一次取通常已算完。两张表都按 {@link RandomState} 弱引用，换存档后随维度回收。</li>
 *   <li>水系禁区以荡魂山山心为中心（{@link Waterways.KeepOutAnchor}）。</li>
 *   <li>断面封成断崖（Alex 定）：噪声填充后，紧挨界外的区块里朝界外那一排边缘列中的流体（含水层的河、湖、洞穴水和
 *       深处岩浆）换成默认方块；装饰阶段 {@link BoundedLevel} 也把写进边缘列的含流体方块换成默认方块，所以没有流体挨着
 *       虚空，区块转正时的流体后处理也不会往外流。</li>
 * </ul>
 */

public final class BoundedNoiseGenerator extends NoiseBasedChunkGenerator implements Waterways.KeepOutAnchor {

    public static final String ID = "bounded_noise";
    private static final int CHUNK_LIMIT = 1_875_000;
    public static final MapCodec<BoundedNoiseGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings),
            Codec.intRange(1, CHUNK_LIMIT).fieldOf("half_size_chunks").forGetter(BoundedNoiseGenerator::getHalfSize)
    ).apply(instance, BoundedNoiseGenerator::new));

    private static final Map<RandomState, BoundedNoiseGenerator> GENERATORS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final int halfSizeChunks;
    private final Map<RandomState, SiteSlot> sites = Collections.synchronizedMap(new WeakHashMap<>());

    public BoundedNoiseGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings,
            int halfSizeChunks) {
        super(biomeSource, settings);
        this.halfSizeChunks = halfSizeChunks;
    }

    public int getHalfSize() { return halfSizeChunks; }

    public boolean isOutside(ChunkPos chunk) { return isOutside(chunk.x, chunk.z); }

    public boolean isOutside(int chunkX, int chunkZ) {
        return chunkX < -halfSizeChunks || chunkX >= halfSizeChunks || chunkZ < -halfSizeChunks
                || chunkZ >= halfSizeChunks;
    }

    public boolean touchesOutside(ChunkPos chunk) {
        return isOutside(chunk.x - 1, chunk.z - 1) || isOutside(chunk.x + 1, chunk.z + 1);
    }

    public boolean isInside(BlockPos pos) {
        return !isOutside(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    @Override
    protected @NotNull MapCodec<? extends ChunkGenerator> codec() { return CODEC; }

    public static @Nullable BoundedNoiseGenerator of(RandomState random) { return GENERATORS.get(random); }

    @Override
    public @NotNull ChunkGeneratorStructureState createState(@NotNull HolderLookup<StructureSet> structureSets,
            @NotNull RandomState random, long seed) {
        GENERATORS.put(random, this);
        Util.backgroundExecutor().execute(() -> getSite(random));
        return super.createState(structureSets, random, seed);
    }

    public BlissfulLandSite getSite(RandomState random) {
        return sites.computeIfAbsent(random, key -> new SiteSlot()).get(generatorSettings().value(), random,
                getBiomeSource());
    }

    @Override
    public BlockPos getKeepOutCentre(RandomState random) { return getSite(random).centre(); }

    @Override
    public @NotNull CompletableFuture<ChunkAccess> fillFromNoise(@NotNull Blender blender,
            @NotNull RandomState randomState, @NotNull StructureManager structureManager, @NotNull ChunkAccess chunk) {
        if (isOutside(chunk.getPos())) return CompletableFuture.completedFuture(chunk);

        CompletableFuture<ChunkAccess> filled = super.fillFromNoise(blender, randomState, structureManager, chunk);
        return touchesOutside(chunk.getPos()) ? filled.thenApply(this::sealEdges) : filled;
    }

    public boolean facesOutside(BlockPos pos) {
        return isInside(pos) && !(isInside(pos.east()) && isInside(pos.west()) && isInside(pos.north())
                && isInside(pos.south()));
    }

    private ChunkAccess sealEdges(ChunkAccess chunk) {
        ChunkPos position = chunk.getPos();
        BlockState solid = generatorSettings().value().defaultBlock();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int z = position.getMinBlockZ(); z <= position.getMaxBlockZ(); z++) {
            for (int x = position.getMinBlockX(); x <= position.getMaxBlockX(); x++) {
                if (!facesOutside(cursor.set(x, 0, z))) continue;

                for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                    if (!chunk.getBlockState(cursor.setY(y)).getFluidState().isEmpty()) {
                        chunk.setBlockState(cursor, solid, false);
                    }
                }
            }
        }
        return chunk;
    }

    @Override
    public void buildSurface(@NotNull WorldGenRegion level, @NotNull StructureManager structureManager,
            @NotNull RandomState random, @NotNull ChunkAccess chunk) {
        if (!isOutside(chunk.getPos())) super.buildSurface(level, structureManager, random, chunk);
    }

    @Override
    public void applyCarvers(@NotNull WorldGenRegion level, long seed, @NotNull RandomState random,
            @NotNull BiomeManager biomeManager, @NotNull StructureManager structureManager,
            @NotNull ChunkAccess chunk, GenerationStep.@NotNull Carving step) {
        if (!isOutside(chunk.getPos())) {
            super.applyCarvers(level, seed, random, biomeManager, structureManager, chunk, step);
        }
    }

    @Override
    public void applyBiomeDecoration(@NotNull WorldGenLevel level, @NotNull ChunkAccess chunk,
            @NotNull StructureManager structureManager) {
        ChunkPos position = chunk.getPos();
        if (isOutside(position)) return;

        super.applyBiomeDecoration(touchesOutside(position) ? BoundedLevel.wrap(level, this::isInside,
                this::facesOutside, generatorSettings().value().defaultBlock()) : level, chunk, structureManager);
    }

    @Override
    public void spawnOriginalMobs(@NotNull WorldGenRegion level) {
        if (!isOutside(level.getCenter())) super.spawnOriginalMobs(level);
    }

    private static final class SiteSlot {

        private volatile BlissfulLandSite site;

        private BlissfulLandSite get(NoiseGeneratorSettings settings, RandomState random, BiomeSource biomes) {
            BlissfulLandSite known = site;
            if (known != null) return known;
            synchronized (this) {
                if (site == null) site = BlissfulLandSite.pick(settings, random, biomes);
                return site;
            }
        }
    }
}
