package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.GuzhenrenBlocks;
import net.alex.guzhenrenworld.secretrealm.SoulsunderClearance;
import net.alex.guzhenrenworld.secretrealm.SoulsunderMountain;
import net.alex.guzhenrenworld.secretrealm.SoulsunderMountainPiece;
import net.alex.guzhenrenworld.secretrealm.SoulsunderShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * 荡魂山世界生成运行时检查，按原版主世界噪声生成器（不经 GameTest 自身的超平坦世界）。
 * <ul>
 *   <li>注册：结构集是 1 个点的同心环，结构在 {@code SURFACE_STRUCTURES} 步、地形适配 {@code beard_thin}、群系含深海（
 *       偏好落空也照生成），偏好标签收平原、不收海洋与锯齿山峰。</li>
 *   <li>放置：种子 12345 与 20261120 下，山心离原点落在第一环半径加偏移的范围内，且与所有要塞起点相距超过
 *       {@link #STRONGHOLD_GAP} 格（同心环与要塞同种子同 distance 会重合，见 {@link SoulsunderMountain}）。</li>
 *   <li>生成：种子 12345（山脚低洼、泡水）、{@link #HILL_SEED}（坡地）与 {@link #GENTLE_SEED}（缓丘）的环区块起点有效、山心为
 *       环区块中心、每个区块一片；山体 11×11 个区块并行做噪声填充两遍（无胡须、有胡须，不含地表、雕刻与地物；12345 先空
 *       跑一遍预热），各由结构片逐区块写入并计时，日志 {@code soulsunder mountain fill} 记噪声与写入毫秒数、山石、补石与
 *       清除数；有胡须时补石不多于无胡须，写入后贴地列的最低山石下方是实心方块（无浮空），各列最高一段以上到地表只剩空
 *       气与流体，山心列顶到山底加山形顶高。</li>
 *   <li>积雪：荡魂山石上雪层不能存活，原版石头上能。</li>
 *   <li>净空：从种子 1 起找一个原版会在山心净空圆里生成村庄的种子，对该区块走 {@code createStructures} 后，区块里没有进
 *       净空圆的结构起点；要塞与荡魂山自身不受净空约束。</li>
 *   <li>计时只含噪声与结构片写入；正式生成在 {@code FEATURES} 状态写入、多维护两张高度图，游戏内每座山的实际耗时以结构
 *       片的 INFO 日志为准。</li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoulsunderMountainGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SEED = 12345L;
    private static final long OTHER_SEED = 20261120L;
    private static final int STRONGHOLD_GAP = 96 + 64;
    private static final int BIOME_SHIFT = 112 + 16;
    private static final long HILL_SEED = 10L;
    private static final long GENTLE_SEED = 7L;
    private static final long[] MOUNTAIN_SEEDS = {SEED, HILL_SEED, GENTLE_SEED};
    private static final int VILLAGE_SEEDS = 48;
    private static final int VILLAGE_REACH = 96;
    private static final int DUMP_REACH = 112;
    private static final LevelHeightAccessor HEIGHTS = LevelHeightAccessor.create(-64, 384);

    private SoulsunderMountainGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void registeredAsOneRingInTheOverworld(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        StructureSet set = registries.registryOrThrow(Registries.STRUCTURE_SET)
                .getOrThrow(SoulsunderMountain.STRUCTURE_SET);
        helper.assertTrue(set.placement() instanceof ConcentricRingsStructurePlacement rings && rings.count() == 1
                && rings.distance() == SoulsunderMountain.RING_DISTANCE, "the set is not one concentric ring point");
        helper.assertTrue(set.structures().size() == 1
                && set.structures().getFirst().structure().is(SoulsunderMountain.STRUCTURE), "the set lists " + set);
        Structure structure = registries.registryOrThrow(Registries.STRUCTURE).getOrThrow(SoulsunderMountain.STRUCTURE);
        helper.assertTrue(structure.step() == GenerationStep.Decoration.SURFACE_STRUCTURES, "step " + structure.step());
        helper.assertTrue(structure.terrainAdaptation() == TerrainAdjustment.BEARD_THIN,
                "terrain adaptation " + structure.terrainAdaptation());
        Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        helper.assertTrue(structure.biomes().contains(biomes.getHolderOrThrow(Biomes.DEEP_OCEAN)),
                "the mountain could vanish over a deep ocean");
        helper.assertTrue(biomes.getHolderOrThrow(Biomes.PLAINS).is(SoulsunderMountain.BIASED_TO)
                && !biomes.getHolderOrThrow(Biomes.OCEAN).is(SoulsunderMountain.BIASED_TO)
                && !biomes.getHolderOrThrow(Biomes.JAGGED_PEAKS).is(SoulsunderMountain.BIASED_TO),
                "the preferred biomes take oceans or peaks, or leave out plains");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 400)
    public static void ringStaysClearOfTheStrongholds(GameTestHelper helper) {
        int innermost = SoulsunderMountain.RING_DISTANCE * 16 * 11 / 4 - BIOME_SHIFT;
        int outermost = SoulsunderMountain.RING_DISTANCE * 16 * 21 / 4 + BIOME_SHIFT;
        for (long seed : new long[]{SEED, OTHER_SEED}) {
            Overworld world = Overworld.create(helper, seed);
            ChunkPos mountain = world.getMountainChunk();
            double distance = Math.hypot(mountain.getMiddleBlockX(), mountain.getMiddleBlockZ());
            helper.assertTrue(distance >= innermost && distance <= outermost,
                    "seed " + seed + ": the mountain lies " + (int) distance + " blocks out");
            double gap = Double.MAX_VALUE;
            for (ChunkPos stronghold : world.getStrongholds()) {
                gap = Math.min(gap, Math.hypot(stronghold.getMiddleBlockX() - mountain.getMiddleBlockX(),
                        stronghold.getMiddleBlockZ() - mountain.getMiddleBlockZ()));
            }
            LOGGER.info("soulsunder mountain ring (seed {}): chunk {}, {} blocks out, nearest stronghold {} blocks",
                    seed, mountain, (int) distance, (int) gap);
            helper.assertTrue(gap > STRONGHOLD_GAP, "seed " + seed + ": a stronghold " + (int) gap + " blocks off");
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 600)
    public static void mountainStandsOnNoiseTerrain(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        Structure structure = registries.registryOrThrow(Registries.STRUCTURE).getOrThrow(SoulsunderMountain.STRUCTURE);
        SoulsunderShape shape = SoulsunderShape.get();
        Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        BlockState stone = GuzhenrenBlocks.SOULSUNDER_STONE.get().defaultBlockState();
        for (long seed : MOUNTAIN_SEEDS) {
            Overworld world = Overworld.create(helper, seed);
            ChunkPos ring = world.getMountainChunk();
            StructureStart start = structure.generate(registries, world.generator(),
                    world.generator().getBiomeSource(), world.random(),
                    helper.getLevel().getServer().getStructureManager(), seed, ring, 0, HEIGHTS,
                    biome -> structure.biomes().contains(biome));
            helper.assertTrue(start.isValid(), "seed " + seed + ": no valid start at " + ring);
            List<SoulsunderMountainPiece> pieces = start.getPieces().stream()
                    .map(SoulsunderMountainPiece.class::cast).toList();
            BlockPos centre = pieces.getFirst().getCentre();
            helper.assertTrue(centre.getX() == ring.getMiddleBlockX() && centre.getZ() == ring.getMiddleBlockZ(),
                    "the centre " + centre + " is not the middle of " + ring);
            helper.assertTrue(pieces.stream().allMatch(piece -> getChunk(piece).equals(getChunk(piece, true))),
                    "a piece spans two chunks");

            if (seed == SEED) fillMountain(world, seed, pieces, null, biomes, "warm-up", new ConcurrentHashMap<>());
            int[] bare = fillMountain(world, seed, pieces, null, biomes, "without beard", new ConcurrentHashMap<>());
            Map<ChunkPos, ProtoChunk> chunks = new ConcurrentHashMap<>();
            int[] bearded = fillMountain(world, seed, pieces, start, biomes, "with beard", chunks);
            dumpScene(world, seed, start, biomes, chunks, centre);
            helper.assertTrue(bare[0] == shape.getVolume() && bearded[0] == shape.getVolume(),
                    "placed " + bearded[0] + " of " + shape.getVolume() + " mountain blocks");
            helper.assertTrue(bearded[1] <= bare[1], "the beard adds foundation: " + bearded[1] + " > " + bare[1]);
            helper.assertTrue(countFloatingColumns(chunks, shape, centre) == 0,
                    "seed " + seed + ": some mountain columns float");
            int leftover = countLeftoversOnTop(chunks, shape, centre);
            helper.assertTrue(leftover == 0, "seed " + seed + ": " + leftover + " terrain blocks lie on the mountain");
            int summit = shape.getCentre() * shape.getWidth() + shape.getCentre();
            int top = centre.getY() + shape.getTop(summit, shape.getSpanCount(summit) - 1);
            ProtoChunk middle = chunks.get(ring);
            helper.assertTrue(middle.getBlockState(centre.atY(top)).is(stone.getBlock())
                    && !middle.getBlockState(centre.atY(top + 1)).is(stone.getBlock()),
                    "seed " + seed + ": the summit is not at y " + top);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void snowSlidesOffSoulsunderStone(GameTestHelper helper) {
        BlockPos ground = new BlockPos(1, 1, 1);
        helper.setBlock(ground, GuzhenrenBlocks.SOULSUNDER_STONE.get());
        helper.assertFalse(Blocks.SNOW.defaultBlockState().canSurvive(helper.getLevel(),
                helper.absolutePos(ground.above())), "snow settles on soulsunder stone");
        helper.setBlock(ground, Blocks.STONE);
        helper.assertTrue(Blocks.SNOW.defaultBlockState().canSurvive(helper.getLevel(),
                helper.absolutePos(ground.above())), "snow no longer settles on plain stone");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 1200)
    public static void surfaceStructuresKeepClear(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        Registry<StructureSet> sets = registries.registryOrThrow(Registries.STRUCTURE_SET);
        Registry<Structure> structures = registries.registryOrThrow(Registries.STRUCTURE);
        StructureTemplateManager templates = helper.getLevel().getServer().getStructureManager();
        Holder<StructureSet> mountainSet = sets.getHolderOrThrow(SoulsunderMountain.STRUCTURE_SET);
        Holder<StructureSet> villages = sets.getHolderOrThrow(BuiltinStructureSets.VILLAGES);
        NoiseBasedChunkGenerator generator = Overworld.create(helper, SEED).generator();
        for (long seed = 1; seed <= VILLAGE_SEEDS; seed++) {
            Overworld world = new Overworld(generator, RandomState.create(generator.generatorSettings().value(),
                    registries.lookupOrThrow(Registries.NOISE), seed), null, sets);
            ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForFlat(world.random(), seed,
                    generator.getBiomeSource(), Stream.of(mountainSet, villages));
            ChunkPos ring = state.getRingPositionsFor(
                    (ConcentricRingsStructurePlacement) mountainSet.value().placement()).getFirst();
            int x = ring.getMiddleBlockX();
            int z = ring.getMiddleBlockZ();
            for (var kept : List.of(SoulsunderMountain.STRUCTURE, BuiltinStructures.STRONGHOLD)) {
                StructureStart start = structures.getOrThrow(kept).generate(registries, generator,
                        generator.getBiomeSource(), world.random(), templates, seed, ring, 0, HEIGHTS, biome -> true);
                helper.assertFalse(SoulsunderClearance.crowds(state, registries, start), kept + " is kept out");
            }
            ChunkPos crowded = findCrowdingVillage(world, state, villages.value(), registries, templates, seed, x, z);
            if (crowded == null) continue;

            ProtoChunk chunk = new ProtoChunk(crowded, UpgradeData.EMPTY, HEIGHTS,
                    registries.registryOrThrow(Registries.BIOME), null);
            generator.createStructures(registries, state, new StructureManager(null, null, null), chunk, templates);
            for (StructureStart start : chunk.getAllStarts().values()) {
                helper.assertFalse(start.isValid() && SoulsunderClearance.crowds(start.getBoundingBox(), x, z),
                        "seed " + seed + ": " + start.getStructure() + " at " + crowded + " crowds the mountain");
            }
            LOGGER.info("soulsunder mountain clearance (seed {}): a village at {} would crowd the mountain at {}, "
                    + "{} other starts kept", seed, crowded, ring, chunk.getAllStarts().size());
            helper.succeed();
            return;
        }
        helper.fail("no seed up to " + VILLAGE_SEEDS + " puts a village next to the mountain");
    }

    private static ChunkPos findCrowdingVillage(Overworld world, ChunkGeneratorStructureState state,
            StructureSet villages, RegistryAccess registries, StructureTemplateManager templates, long seed, int x,
            int z) {
        StructurePlacement placement = villages.placement();
        int reach = (SoulsunderMountain.CLEARANCE + VILLAGE_REACH) >> 4;
        for (int chunkZ = (z >> 4) - reach; chunkZ <= (z >> 4) + reach; chunkZ++) {
            for (int chunkX = (x >> 4) - reach; chunkX <= (x >> 4) + reach; chunkX++) {
                if (!placement.isStructureChunk(state, chunkX, chunkZ)) continue;

                ChunkPos position = new ChunkPos(chunkX, chunkZ);
                for (StructureSet.StructureSelectionEntry entry : villages.structures()) {
                    Structure village = entry.structure().value();
                    StructureStart start = village.generate(registries, world.generator(),
                            world.generator().getBiomeSource(), world.random(), templates, seed, position, 0, HEIGHTS,
                            village.biomes()::contains);
                    if (start.isValid() && SoulsunderClearance.crowds(start.getBoundingBox(), x, z)) return position;
                }
            }
        }
        return null;
    }

    private static int[] fillMountain(Overworld world, long seed, List<SoulsunderMountainPiece> pieces,
            StructureStart start, Registry<Biome> biomes, String label, Map<ChunkPos, ProtoChunk> chunks) {
        StructureManager structures = getStarts(start);
        long started = System.nanoTime();
        pieces.parallelStream().forEach(piece -> {
            ChunkPos position = getChunk(piece);
            ProtoChunk chunk = new ProtoChunk(position, UpgradeData.EMPTY, HEIGHTS, biomes, null);
            world.generator().fillFromNoise(Blender.empty(), world.random(), structures, chunk).join();
            chunks.put(position, chunk);
        });
        long noise = System.nanoTime() - started;
        BlockState mountain = GuzhenrenBlocks.SOULSUNDER_STONE.get().defaultBlockState();
        BlockState foundation = Blocks.STONE.defaultBlockState();
        started = System.nanoTime();
        int[] placed = new int[3];
        for (SoulsunderMountainPiece piece : pieces) {
            int[] counts = piece.fill(chunks.get(getChunk(piece)), mountain, foundation);
            for (int i = 0; i < placed.length; i++) placed[i] += counts[i];
        }
        long fill = System.nanoTime() - started;
        LOGGER.info("soulsunder mountain fill {} (seed {}): {} chunks, noise {} ms, fill {} ms, {} mountain + {} "
                + "foundation blocks, {} cleared", label, seed, pieces.size(), noise / 1_000_000, fill / 1_000_000,
                placed[0], placed[1], placed[2]);
        return placed;
    }

    private static StructureManager getStarts(StructureStart start) {
        return new StructureManager(null, null, null) {

            @Override
            public @NotNull List<StructureStart> startsForStructure(@NotNull ChunkPos position,
                    @NotNull Predicate<Structure> predicate) {
                return start != null && predicate.test(start.getStructure()) ? List.of(start) : List.of();
            }
        };
    }

    /**
     * 只在设了环境变量 {@code SOULSUNDER_DUMP}（目录）时写出山心 ±{@link #DUMP_REACH} 格的场景，供任务目录的预览脚本画低角度
     * 侧视；每列四个 big-endian short：地形顶、山石底、山石顶（无山石为 -32768）、水面顶，按 z 行、x 列排。
     */
    private static void dumpScene(Overworld world, long seed, StructureStart start, Registry<Biome> biomes,
            Map<ChunkPos, ProtoChunk> chunks, BlockPos centre) {
        String directory = System.getenv("SOULSUNDER_DUMP");
        if (directory == null) return;

        int min = -DUMP_REACH;
        int size = DUMP_REACH * 2;
        StructureManager structures = getStarts(start);
        for (int chunkZ = (centre.getZ() + min) >> 4; chunkZ <= (centre.getZ() - min) >> 4; chunkZ++) {
            for (int chunkX = (centre.getX() + min) >> 4; chunkX <= (centre.getX() - min) >> 4; chunkX++) {
                chunks.computeIfAbsent(new ChunkPos(chunkX, chunkZ), position -> {
                    ProtoChunk chunk = new ProtoChunk(position, UpgradeData.EMPTY, HEIGHTS, biomes, null);
                    world.generator().fillFromNoise(Blender.empty(), world.random(), structures, chunk).join();
                    return chunk;
                });
            }
        }
        Block stone = GuzhenrenBlocks.SOULSUNDER_STONE.get();
        ByteBuffer out = ByteBuffer.allocate(8 + size * size * 8);
        out.putInt(size).putInt(centre.getY());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int worldX = centre.getX() + min + x;
                int worldZ = centre.getZ() + min + z;
                ProtoChunk chunk = chunks.get(new ChunkPos(worldX >> 4, worldZ >> 4));
                short terrain = Short.MIN_VALUE;
                short bottom = Short.MIN_VALUE;
                short top = Short.MIN_VALUE;
                short water = Short.MIN_VALUE;
                for (int y = HEIGHTS.getMaxBuildHeight() - 1; y >= HEIGHTS.getMinBuildHeight(); y--) {
                    BlockState state = chunk.getBlockState(cursor.set(worldX, y, worldZ));
                    if (state.is(stone)) {
                        if (top == Short.MIN_VALUE) top = (short) y;
                        bottom = (short) y;
                    } else if (!state.getFluidState().isEmpty()) {
                        if (water == Short.MIN_VALUE) water = (short) y;
                    } else if (!state.isAir()) {
                        terrain = (short) y;
                        break;
                    }
                }
                out.putShort(terrain).putShort(bottom).putShort(top).putShort(water);
            }
        }
        try {
            Files.write(Path.of(directory, "soulsunder-" + seed + ".bin"), out.array());
        } catch (IOException exception) {
            LOGGER.warn("soulsunder mountain dump failed", exception);
        }
    }

    private static ChunkPos getChunk(SoulsunderMountainPiece piece) { return getChunk(piece, false); }

    private static ChunkPos getChunk(SoulsunderMountainPiece piece, boolean far) {
        BoundingBox box = piece.getBoundingBox();
        return far ? new ChunkPos(box.maxX() >> 4, box.maxZ() >> 4) : new ChunkPos(box.minX() >> 4, box.minZ() >> 4);
    }

    private static int countLeftoversOnTop(Map<ChunkPos, ProtoChunk> chunks, SoulsunderShape shape, BlockPos centre) {
        Block stone = GuzhenrenBlocks.SOULSUNDER_STONE.get();
        int leftover = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int column = 0; column < shape.getWidth() * shape.getDepth(); column++) {
            if (shape.getSpanCount(column) == 0) continue;

            int x = centre.getX() - shape.getCentre() + column % shape.getWidth();
            int z = centre.getZ() - shape.getCentre() + column / shape.getWidth();
            ProtoChunk chunk = chunks.get(new ChunkPos(x >> 4, z >> 4));
            // 测试区块停在 EMPTY 状态，不维护 WORLD_SURFACE，从世界顶往下扫。
            for (int y = HEIGHTS.getMaxBuildHeight() - 1; y > HEIGHTS.getMinBuildHeight(); y--) {
                BlockState state = chunk.getBlockState(cursor.set(x, y, z));
                if (state.is(stone)) break;
                if (state != state.getFluidState().createLegacyBlock()) leftover++;
            }
        }
        return leftover;
    }

    private static int countFloatingColumns(Map<ChunkPos, ProtoChunk> chunks, SoulsunderShape shape, BlockPos centre) {
        Block stone = GuzhenrenBlocks.SOULSUNDER_STONE.get();
        int floating = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int column = 0; column < shape.getWidth() * shape.getDepth(); column++) {
            if (shape.getSpanCount(column) == 0 || shape.getBottom(column, 0) != 0) continue;

            int x = centre.getX() - shape.getCentre() + column % shape.getWidth();
            int z = centre.getZ() - shape.getCentre() + column / shape.getWidth();
            ProtoChunk chunk = chunks.get(new ChunkPos(x >> 4, z >> 4));
            int y = HEIGHTS.getMinBuildHeight();
            while (y < HEIGHTS.getMaxBuildHeight() && !chunk.getBlockState(cursor.set(x, y, z)).is(stone)) y++;
            if (y == HEIGHTS.getMinBuildHeight() || y == HEIGHTS.getMaxBuildHeight()) continue;

            BlockState below = chunk.getBlockState(cursor.set(x, y - 1, z));
            if (below.isAir() || !below.getFluidState().isEmpty()) floating++;
        }
        return floating;
    }

    private record Overworld(NoiseBasedChunkGenerator generator, RandomState random,
            ChunkGeneratorStructureState structures, Registry<StructureSet> sets) {

        static Overworld create(GameTestHelper helper, long seed) {
            RegistryAccess registries = helper.getLevel().registryAccess();
            NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(
                    registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                            .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)),
                    registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD));
            RandomState random = RandomState.create(generator.generatorSettings().value(),
                    registries.lookupOrThrow(Registries.NOISE), seed);
            ChunkGeneratorStructureState structures = ChunkGeneratorStructureState.createForNormal(random, seed,
                    generator.getBiomeSource(), registries.lookupOrThrow(Registries.STRUCTURE_SET));
            structures.ensureStructuresGenerated();
            return new Overworld(generator, random, structures, registries.registryOrThrow(Registries.STRUCTURE_SET));
        }

        ChunkPos getMountainChunk() {
            StructureSet mountain = sets.getOrThrow(SoulsunderMountain.STRUCTURE_SET);
            return structures.getRingPositionsFor(getRings(mountain)).getFirst();
        }

        List<ChunkPos> getStrongholds() {
            return structures.getRingPositionsFor(getRings(sets.getOrThrow(BuiltinStructureSets.STRONGHOLDS)));
        }

        private static ConcentricRingsStructurePlacement getRings(StructureSet set) {
            return (ConcentricRingsStructurePlacement) set.placement();
        }
    }
}
