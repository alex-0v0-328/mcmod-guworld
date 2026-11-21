package net.alex.guzhenrenworld.gametest;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.GuzhenrenBlocks;
import net.alex.guzhenrenworld.dimension.blissfulland.BoundedNoiseGenerator;
import net.alex.guzhenrenworld.dimension.blissfulland.FoxImmortalBlissfulLand;
import net.alex.guzhenrenworld.secretrealm.SoulsunderClearance;
import net.alex.guzhenrenworld.secretrealm.SoulsunderMountain;
import net.alex.guzhenrenworld.secretrealm.SoulsunderMountainPiece;
import net.alex.guzhenrenworld.secretrealm.SoulsunderShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.CheckerboardColumnBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
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
 * 荡魂山与狐仙福地的世界生成运行时检查，按福地自己的生成器（不经 GameTest 自身的超平坦世界）。
 * <ul>
 *   <li>注册：结构集是 1 个点、distance 0、偏好群系为空的同心环，结构在 {@code SURFACE_STRUCTURES} 步、地形适配
 *       {@code beard_thin}，群系只有福地一个。</li>
 *   <li>放置：三个种子下，福地结构状态的环位都恰为区块 (0, 0)；主世界结构状态算不出该结构集的环位。</li>
 *   <li>生成：福地里环区块起点有效、山心为 {@link #CENTRE}、每个区块一片；山体区块并行做噪声填充两遍（无胡须、有胡须，
 *       不含地表、雕刻与地物；先空跑一遍预热），各由结构片逐区块写入并计时，日志 {@code soulsunder mountain fill} 记噪
 *       声与写入毫秒数、山石、补石与清除数；有胡须时补石不多于无胡须，写入后贴地列的最低山石下方是实心方块（无浮空），
 *       各列最高一段以上到地表只剩空气与流体，山心列顶到山底加山形顶高；行宫锚点 {@link #ANCHOR} 脚下是山石、往上
 *       {@link #HEADROOM} 格是空的、{@link #ROOF_REACH} 格内有洞顶，山巅站立点 {@link #SUMMIT} 脚下是山石。</li>
 *   <li>坡地：在原版主世界（种子 {@link #SLOPE_SEED}）沿一条斜线找第一个山心周围 ±{@link #SLOPE_REACH} 格地面落差
 *       不小于 {@link #SLOPE_RELIEF} 格的陆地区块，把山立在那里（福地平台上山脚不贴地形、不补石，这轮补上覆盖）：无胡
 *       须时要补石，有胡须时补石不多于无胡须，没有浮空列，山上不压原地形。</li>
 *   <li>福地地形：山心 {@link #PLATFORM_CHECK} 格内地表都在 y 64（含主模组入口落点 {@link #LANDING} 周围），界内最后
 *       一列区块有方块、界外第一列区块全空；疆域内稀疏取样有水面、有地下洞穴，日志 {@code blissful land terrain} 记水面
 *       列、地面高度与洞穴占比。</li>
 *   <li>断崖：沿疆域东边线每 {@link #EDGE_STEP} 格查噪声地形，找到第一列挨着虚空的流体，填充它所在的区块后，东边
 *       线那一列从下到上没有流体。</li>
 *   <li>积雪：荡魂山石上雪层不能存活，原版石头上能。</li>
 *   <li>净空：原版主世界噪声配上平原与福地两个群系的棋盘群系源（{@link #CHECKER_SIZE}，原点所在 256 格见方是平原），手
 *       建「荡魂山 + 村庄」的结构状态（环位同样是区块 (0, 0)），从种子 1 起找一个原版会在山心净空圆里生成村庄的种子，对
 *       该区块走 {@code createStructures} 后，区块里没有进净空圆的结构起点；要塞与荡魂山自身不受净空约束。</li>
 *   <li>计时只含噪声与结构片写入；正式生成在 {@code FEATURES} 状态写入、多维护两张高度图，游戏内每座山的实际耗时以结构
 *       片的 INFO 日志为准。</li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoulsunderMountainGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SEED = 12345L;
    private static final BlockPos CENTRE = new BlockPos(8, 64, 8);
    private static final BlockPos LANDING = new BlockPos(8, 65, 168);
    private static final BlockPos ANCHOR = new BlockPos(6, 104, 5);
    private static final int HEADROOM = 3;
    private static final int ROOF_REACH = 20;
    private static final BlockPos SUMMIT = new BlockPos(12, 182, 6);
    private static final int PLATFORM_CHECK = 184;
    private static final int PLATFORM_STEP = 8;
    private static final int TERRAIN_REACH = 4096;
    private static final int TERRAIN_STEP = 128;
    private static final int CAVE_DEPTH = 8;
    private static final int CHECKER_SIZE = 4;
    private static final int EDGE_STEP = 16;
    private static final long SLOPE_SEED = 10L;
    private static final int SLOPE_TRIES = 64;
    private static final int SLOPE_STRIDE = 24;
    private static final int SLOPE_REACH = 64;
    private static final int SLOPE_RELIEF = 24;
    private static final long[] RING_SEEDS = {SEED, 10L, 20261120L};
    private static final int VILLAGE_SEEDS = 48;
    private static final int VILLAGE_REACH = 96;
    private static final int DUMP_REACH = 112;
    private static final LevelHeightAccessor HEIGHTS = LevelHeightAccessor.create(-64, 384);

    private SoulsunderMountainGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void registeredAsTheOriginRingOfTheBlissfulLand(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        StructureSet set = registries.registryOrThrow(Registries.STRUCTURE_SET)
                .getOrThrow(SoulsunderMountain.STRUCTURE_SET);
        helper.assertTrue(set.placement() instanceof ConcentricRingsStructurePlacement rings && rings.count() == 1
                && rings.distance() == 0 && rings.preferredBiomes().size() == 0,
                "the set is not one unbiased ring point at the origin");
        helper.assertTrue(set.structures().size() == 1
                && set.structures().getFirst().structure().is(SoulsunderMountain.STRUCTURE), "the set lists " + set);
        Structure structure = registries.registryOrThrow(Registries.STRUCTURE).getOrThrow(SoulsunderMountain.STRUCTURE);
        helper.assertTrue(structure.step() == GenerationStep.Decoration.SURFACE_STRUCTURES, "step " + structure.step());
        helper.assertTrue(structure.terrainAdaptation() == TerrainAdjustment.BEARD_THIN,
                "terrain adaptation " + structure.terrainAdaptation());
        Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        helper.assertTrue(structure.biomes().size() == 1
                && structure.biomes().contains(biomes.getHolderOrThrow(FoxImmortalBlissfulLand.BIOME)),
                "the mountain may stand in " + structure.biomes());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 400)
    public static void ringSitsOnTheOriginChunkOfTheBlissfulLandOnly(GameTestHelper helper) {
        for (long seed : RING_SEEDS) {
            BlissfulLand land = BlissfulLand.create(helper, seed);
            List<ChunkPos> ring = land.structures().getRingPositionsFor(getRings(helper));
            helper.assertTrue(List.of(new ChunkPos(0, 0)).equals(ring), "seed " + seed + ": the ring is " + ring);
            List<ChunkPos> overworld = Overworld.create(helper, seed).structures()
                    .getRingPositionsFor(getRings(helper));
            helper.assertTrue(overworld == null || overworld.isEmpty(),
                    "seed " + seed + ": the overworld still rings the mountain at " + overworld);
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 600)
    public static void mountainStandsOnTheBlissfulLand(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        Structure structure = registries.registryOrThrow(Registries.STRUCTURE).getOrThrow(SoulsunderMountain.STRUCTURE);
        SoulsunderShape shape = SoulsunderShape.get();
        Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        BlockState stone = GuzhenrenBlocks.SOULSUNDER_STONE.get().defaultBlockState();
        long seed = SEED;
        BlissfulLand world = BlissfulLand.create(helper, seed);
        ChunkPos ring = new ChunkPos(0, 0);
        StructureStart start = structure.generate(registries, world.generator(), world.generator().getBiomeSource(),
                world.random(), helper.getLevel().getServer().getStructureManager(), seed, ring, 0, HEIGHTS,
                biome -> structure.biomes().contains(biome));
        helper.assertTrue(start.isValid(), "no valid start at " + ring);
        List<SoulsunderMountainPiece> pieces = start.getPieces().stream()
                .map(SoulsunderMountainPiece.class::cast).toList();
        BlockPos centre = pieces.getFirst().getCentre();
        helper.assertTrue(centre.equals(CENTRE), "the centre " + centre + " is not " + CENTRE);
        helper.assertTrue(pieces.stream().allMatch(piece -> getChunk(piece).equals(getChunk(piece, true))),
                "a piece spans two chunks");
        fillMountain(world.generator(), world.random(), seed, pieces, null, biomes, "warm-up",
                new ConcurrentHashMap<>());
        int[] bare = fillMountain(world.generator(), world.random(), seed, pieces, null, biomes, "without beard",
                new ConcurrentHashMap<>());
        Map<ChunkPos, ProtoChunk> chunks = new ConcurrentHashMap<>();
        int[] bearded = fillMountain(world.generator(), world.random(), seed, pieces, start, biomes, "with beard",
                chunks);
        dumpScene(world.generator(), world.random(), seed, start, biomes, chunks, centre);
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
        helper.assertTrue(isStone(chunks, ANCHOR.below())
                        && BlockPos.betweenClosedStream(ANCHOR, ANCHOR.above(HEADROOM - 1))
                        .noneMatch(pos -> isStone(chunks, pos))
                        && BlockPos.betweenClosedStream(ANCHOR.above(HEADROOM), ANCHOR.above(ROOF_REACH))
                        .anyMatch(pos -> isStone(chunks, pos)),
                "the palace anchor " + ANCHOR + " does not stand in a roofed hollow");
        helper.assertTrue(isStone(chunks, SUMMIT.below()) && !isStone(chunks, SUMMIT),
                "the summit stone is not under " + SUMMIT);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 1200)
    public static void mountainFollowsSlopedTerrain(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        Structure structure = registries.registryOrThrow(Registries.STRUCTURE).getOrThrow(SoulsunderMountain.STRUCTURE);
        SoulsunderShape shape = SoulsunderShape.get();
        Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        Overworld world = Overworld.create(helper, SLOPE_SEED);
        ChunkPos ring = findSlope(world);
        helper.assertTrue(ring != null, "no chunk with " + SLOPE_RELIEF + " blocks of relief for seed " + SLOPE_SEED);
        StructureStart start = structure.generate(registries, world.generator(), world.generator().getBiomeSource(),
                world.random(), helper.getLevel().getServer().getStructureManager(), SLOPE_SEED, ring, 0, HEIGHTS,
                biome -> true);
        helper.assertTrue(start.isValid(), "no valid start at " + ring);
        List<SoulsunderMountainPiece> pieces = start.getPieces().stream()
                .map(SoulsunderMountainPiece.class::cast).toList();
        BlockPos centre = pieces.getFirst().getCentre();
        int[] bare = fillMountain(world.generator(), world.random(), SLOPE_SEED, pieces, null, biomes,
                "slope without beard", new ConcurrentHashMap<>());
        Map<ChunkPos, ProtoChunk> chunks = new ConcurrentHashMap<>();
        int[] bearded = fillMountain(world.generator(), world.random(), SLOPE_SEED, pieces, start, biomes,
                "slope with beard", chunks);
        helper.assertTrue(bare[0] == shape.getVolume() && bearded[0] == shape.getVolume(),
                "placed " + bearded[0] + " of " + shape.getVolume() + " mountain blocks on the slope");
        helper.assertTrue(bare[1] > 0, "the slope at " + ring + " needs no foundation");
        helper.assertTrue(bearded[1] <= bare[1], "the beard adds foundation: " + bearded[1] + " > " + bare[1]);
        helper.assertTrue(countFloatingColumns(chunks, shape, centre) == 0, "some mountain columns float on the slope");
        int leftover = countLeftoversOnTop(chunks, shape, centre);
        helper.assertTrue(leftover == 0, leftover + " terrain blocks lie on the mountain on the slope");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 1200)
    public static void blissfulLandIsFlatAroundTheMountainAndVoidOutside(GameTestHelper helper) {
        BlissfulLand land = BlissfulLand.create(helper, SEED);
        BoundedNoiseGenerator generator = land.generator();
        RandomState random = land.random();
        int uneven = 0;
        for (int dz = -PLATFORM_CHECK; dz <= PLATFORM_CHECK; dz += PLATFORM_STEP) {
            for (int dx = -PLATFORM_CHECK; dx <= PLATFORM_CHECK; dx += PLATFORM_STEP) {
                if (dx * dx + dz * dz > PLATFORM_CHECK * PLATFORM_CHECK) continue;

                int x = CENTRE.getX() + dx;
                int z = CENTRE.getZ() + dz;
                if (generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, HEIGHTS, random) != LANDING.getY()
                        || generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, HEIGHTS, random)
                        != LANDING.getY()) {
                    uneven++;
                }
            }
        }
        helper.assertTrue(uneven == 0, uneven + " platform columns are not grass-high at y 64");
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int height = generator.getBaseHeight(LANDING.getX() + dx, LANDING.getZ() + dz,
                        Heightmap.Types.WORLD_SURFACE_WG, HEIGHTS, random);
                helper.assertTrue(height == LANDING.getY(), "the landing is " + height + " high, not " + LANDING);
            }
        }
        int edge = FoxImmortalBlissfulLand.HALF_SIZE_CHUNKS;
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (ChunkPos position : List.of(new ChunkPos(edge - 1, 0), new ChunkPos(-edge, edge - 1))) {
            helper.assertFalse(generator.isOutside(position), position + " lies outside");
            helper.assertFalse(isEmpty(fill(generator, random, position, biomes)), position + " stays void");
        }
        for (ChunkPos position : List.of(new ChunkPos(edge, 0), new ChunkPos(0, -edge - 1),
                new ChunkPos(-edge - 1, edge))) {
            helper.assertTrue(generator.isOutside(position), position + " lies inside");
            helper.assertTrue(isEmpty(fill(generator, random, position, biomes)), position + " is not void");
        }
        sampleTerrain(helper, generator, random);
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 1200)
    public static void blissfulLandSealsItsEdge(GameTestHelper helper) {
        BlissfulLand land = BlissfulLand.create(helper, SEED);
        BoundedNoiseGenerator generator = land.generator();
        int edge = FoxImmortalBlissfulLand.HALF_SIZE_CHUNKS * 16 - 1;
        BlockPos found = null;
        String fluid = null;
        for (int z = -edge; z < edge && found == null; z += EDGE_STEP) {
            NoiseColumn column = generator.getBaseColumn(edge, z, HEIGHTS, land.random());
            for (int y = HEIGHTS.getMaxBuildHeight() - 1; y >= HEIGHTS.getMinBuildHeight(); y--) {
                BlockState state = column.getBlock(y);
                if (!state.getFluidState().isEmpty()) {
                    found = new BlockPos(edge, y, z);
                    fluid = state.getBlock().getDescriptionId();
                    break;
                }
            }
        }
        helper.assertTrue(found != null, "no fluid reaches the east edge of the noise terrain");
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        ProtoChunk chunk = fill(generator, land.random(), new ChunkPos(found), biomes);
        int wet = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
            for (int y = HEIGHTS.getMinBuildHeight(); y < HEIGHTS.getMaxBuildHeight(); y++) {
                if (!chunk.getBlockState(cursor.set(edge, y, z)).getFluidState().isEmpty()) wet++;
            }
        }
        LOGGER.info("blissful land edge: {} at {} in the noise, {} fluid blocks left on the rim of {}", fluid, found,
                wet, chunk.getPos());
        helper.assertTrue(wet == 0, wet + " fluid blocks still face the void in " + chunk.getPos());
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
        Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(new CheckerboardColumnBiomeSource(
                HolderSet.direct(biomes.getHolderOrThrow(Biomes.PLAINS),
                        biomes.getHolderOrThrow(FoxImmortalBlissfulLand.BIOME)), CHECKER_SIZE),
                registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD));
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

    private static int[] fillMountain(NoiseBasedChunkGenerator generator, RandomState random, long seed,
            List<SoulsunderMountainPiece> pieces, StructureStart start, Registry<Biome> biomes, String label,
            Map<ChunkPos, ProtoChunk> chunks) {
        StructureManager structures = getStarts(start);
        long started = System.nanoTime();
        pieces.parallelStream().forEach(piece -> {
            ChunkPos position = getChunk(piece);
            ProtoChunk chunk = new ProtoChunk(position, UpgradeData.EMPTY, HEIGHTS, biomes, null);
            generator.fillFromNoise(Blender.empty(), random, structures, chunk).join();
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
     * 只在设了环境变量 {@code SOULSUNDER_DUMP}（目录）时写出山心 ±{@link #DUMP_REACH} 格的场景，供任务目录的预览脚本画
     * 低角度侧视；每列四个 big-endian short：地形顶、山石底、山石顶（无山石为 -32768）、水面顶，按 z 行、x 列排。
     */
    private static void dumpScene(NoiseBasedChunkGenerator generator, RandomState random, long seed,
            StructureStart start, Registry<Biome> biomes, Map<ChunkPos, ProtoChunk> chunks, BlockPos centre) {
        String directory = System.getenv("SOULSUNDER_DUMP");
        if (directory == null) return;

        int min = -DUMP_REACH;
        int size = DUMP_REACH * 2;
        StructureManager structures = getStarts(start);
        for (int chunkZ = (centre.getZ() + min) >> 4; chunkZ <= (centre.getZ() - min) >> 4; chunkZ++) {
            for (int chunkX = (centre.getX() + min) >> 4; chunkX <= (centre.getX() - min) >> 4; chunkX++) {
                chunks.computeIfAbsent(new ChunkPos(chunkX, chunkZ), position -> {
                    ProtoChunk chunk = new ProtoChunk(position, UpgradeData.EMPTY, HEIGHTS, biomes, null);
                    generator.fillFromNoise(Blender.empty(), random, structures, chunk).join();
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

    private static void sampleTerrain(GameTestHelper helper, BoundedNoiseGenerator generator, RandomState random) {
        List<int[]> samples = new ArrayList<>();
        for (int z = -TERRAIN_REACH; z <= TERRAIN_REACH; z += TERRAIN_STEP) {
            for (int x = -TERRAIN_REACH; x <= TERRAIN_REACH; x += TERRAIN_STEP) samples.add(new int[]{x, z});
        }
        long started = System.nanoTime();
        List<int[]> columns = samples.parallelStream().map(sample -> {
            NoiseColumn column = generator.getBaseColumn(sample[0], sample[1], HEIGHTS, random);
            int surface = Integer.MIN_VALUE;
            int floor = Integer.MIN_VALUE;
            for (int y = HEIGHTS.getMaxBuildHeight() - 1; y >= HEIGHTS.getMinBuildHeight(); y--) {
                BlockState state = column.getBlock(y);
                if (state.isAir()) continue;
                if (surface == Integer.MIN_VALUE) surface = y;
                if (state.getFluidState().isEmpty()) {
                    floor = y;
                    break;
                }
            }
            int air = 0;
            for (int y = HEIGHTS.getMinBuildHeight(); y < floor - CAVE_DEPTH; y++) {
                if (column.getBlock(y).isAir()) air++;
            }
            return new int[]{surface, floor, air, floor - CAVE_DEPTH - HEIGHTS.getMinBuildHeight()};
        }).toList();
        long millis = (System.nanoTime() - started) / 1_000_000;
        int wet = 0;
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        long floors = 0;
        long air = 0;
        long underground = 0;
        for (int[] column : columns) {
            if (column[0] > column[1]) wet++;
            lowest = Math.min(lowest, column[1]);
            highest = Math.max(highest, column[1]);
            floors += column[1];
            air += column[2];
            underground += column[3];
        }
        double wetShare = (double) wet / columns.size();
        double caveShare = (double) air / underground;
        LOGGER.info("blissful land terrain: {} columns in {} ms, {} under water ({}%), floor y {} to {} (mean {}), "
                        + "cave air {}% below {} blocks under the floor", columns.size(), millis, wet,
                "%.1f".formatted(wetShare * 100), lowest, highest, "%.1f".formatted((double) floors / columns.size()),
                "%.2f".formatted(caveShare * 100), CAVE_DEPTH);
        helper.assertTrue(wetShare > 0.005 && wetShare < 0.3, "the land holds " + wet + " wet columns of "
                + columns.size());
        helper.assertTrue(caveShare > 0.001, "the land holds no caves: " + air + " air of " + underground);
    }

    private static ProtoChunk fill(NoiseBasedChunkGenerator generator, RandomState random, ChunkPos position,
            Registry<Biome> biomes) {
        ProtoChunk chunk = new ProtoChunk(position, UpgradeData.EMPTY, HEIGHTS, biomes, null);
        return (ProtoChunk) generator.fillFromNoise(Blender.empty(), random, getStarts(null),
                chunk).join();
    }

    private static boolean isEmpty(ProtoChunk chunk) {
        for (LevelChunkSection section : chunk.getSections()) {
            if (!section.hasOnlyAir()) return false;
        }
        return true;
    }

    private static ConcentricRingsStructurePlacement getRings(GameTestHelper helper) {
        return (ConcentricRingsStructurePlacement) helper.getLevel().registryAccess()
                .registryOrThrow(Registries.STRUCTURE_SET).getOrThrow(SoulsunderMountain.STRUCTURE_SET).placement();
    }

    private static ChunkPos findSlope(Overworld world) {
        for (int i = 0; i < SLOPE_TRIES; i++) {
            ChunkPos chunk = new ChunkPos(i * SLOPE_STRIDE, i * SLOPE_STRIDE / 2);
            int lowest = Integer.MAX_VALUE;
            int highest = Integer.MIN_VALUE;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int height = world.generator().getBaseHeight(chunk.getMiddleBlockX() + dx * SLOPE_REACH,
                            chunk.getMiddleBlockZ() + dz * SLOPE_REACH, Heightmap.Types.OCEAN_FLOOR_WG, HEIGHTS,
                            world.random());
                    lowest = Math.min(lowest, height);
                    highest = Math.max(highest, height);
                }
            }
            if (highest - lowest >= SLOPE_RELIEF && lowest > world.generator().getSeaLevel()) {
                LOGGER.info("soulsunder mountain slope (seed {}): chunk {}, ground y {} to {}", SLOPE_SEED, chunk,
                        lowest, highest);
                return chunk;
            }
        }
        return null;
    }

    private static boolean isStone(Map<ChunkPos, ProtoChunk> chunks, BlockPos pos) {
        return chunks.get(new ChunkPos(pos)).getBlockState(pos).is(GuzhenrenBlocks.SOULSUNDER_STONE.get());
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
    }

    private record BlissfulLand(BoundedNoiseGenerator generator, RandomState random,
            ChunkGeneratorStructureState structures) {

        static BlissfulLand create(GameTestHelper helper, long seed) {
            RegistryAccess registries = helper.getLevel().registryAccess();
            ResourceLocation file = FoxImmortalBlissfulLand.LEVEL_STEM.location()
                    .withPath(path -> "dimension/" + path + ".json");
            BoundedNoiseGenerator generator;
            try (Reader reader = helper.getLevel().getServer().getResourceManager().openAsReader(file)) {
                generator = (BoundedNoiseGenerator) LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,
                        registries), JsonParser.parseReader(reader)).getOrThrow(IllegalStateException::new).generator();
            } catch (IOException exception) {
                throw new IllegalStateException("cannot read " + file, exception);
            }
            RandomState random = RandomState.create(generator.generatorSettings().value(),
                    registries.lookupOrThrow(Registries.NOISE), seed);
            ChunkGeneratorStructureState structures = ChunkGeneratorStructureState.createForNormal(random, seed,
                    generator.getBiomeSource(), registries.lookupOrThrow(Registries.STRUCTURE_SET));
            structures.ensureStructuresGenerated();
            return new BlissfulLand(generator, random, structures);
        }
    }
}
