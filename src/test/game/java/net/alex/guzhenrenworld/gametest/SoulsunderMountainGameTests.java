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
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandBiomes;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandFoxImmortal;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandSite;
import net.alex.guzhenrenworld.dimension.blissfulland.BoundedNoiseGenerator;
import net.alex.guzhenrenworld.secretrealm.GentleSitePlacement;
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
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * 荡魂山与狐仙福地的世界生成运行时检查，按福地自己的生成器（不经 GameTest 自身的超平坦世界）。
 * <ul>
 *   <li>注册：结构集用缓地放置（{@code guworld:gentle_site}），结构在 {@code SURFACE_STRUCTURES} 步、地形适配
 *       {@code beard_thin}，群系是福地全部群系。</li>
 *   <li>挑址：{@link #SITE_SEEDS} 三个种子下，各建两份互不相干的福地生成器与 {@code RandomState}，挑出的区块相同（确定
 *       性），在原点 ±32 区块内、合格（无水、起伏不超过 24 格、山底不高于 y 190，山巅不超建造上限）；山心周围
 *       {@link #DRY_CHECK} 格 3×3 列的噪声地形上没有水；放置器只在该区块命中；原版主世界的结构状态处处不命中。日志
 *       {@code soulsunder mountain site} 记每次挑址毫秒数、区块、山底与起伏。</li>
 *   <li>生成：福地里挑址区块起点有效、山心为挑址给出的山心、结构片边界框最高顶是山巅（山底 + 117）、每个区块一片；山体区块并行做噪声填充两遍（无胡须、有胡须，
 *       不含地表、雕刻与地物；先空跑一遍预热），各由结构片逐区块写入并计时，日志 {@code soulsunder mountain fill} 记噪
 *       声与写入毫秒数、山石、补石与清除数；有胡须时补石不多于无胡须，写入后贴地列的最低山石下方是实心方块（无浮空），
 *       各列最高一段以上到地表只剩空气与流体，山心列顶到山底加山形顶高；行宫锚点（山心 + {@link #ANCHOR}）脚下是山石、
 *       往上 {@link #HEADROOM} 格是空的、{@link #ROOF_REACH} 格内有洞顶，山巅站立点（山心 + {@link #SUMMIT}）脚下是山石。
 *       </li>
 *   <li>坡地：在原版主世界（种子 {@link #SLOPE_SEED}）沿一条斜线找第一个山心周围 ±{@link #SLOPE_REACH} 格地面落差
 *       不小于 {@link #SLOPE_RELIEF} 格的陆地区块，把山立在那里（福地挑的是缓地，补石少，这轮补上覆盖）：无胡须时要补石，有胡须时补石不多于无胡须，没有浮空列，山上不压原地形。</li>
 *   <li>福地疆域：界内最后一列区块有方块、界外第一列区块全空；疆域内稀疏取样有水面、有地下洞穴，日志 {@code blissful land terrain} 记水面
 *       列、地面高度与洞穴占比。</li>
 *   <li>断崖：沿疆域东边线每 {@link #EDGE_STEP} 格查噪声地形，找到第一列挨着虚空的流体，填充它所在的区块后，东边
 *       线那一列从下到上没有流体。</li>
 *   <li>积雪：荡魂山石上雪层不能存活，原版石头上能。</li>
 *   <li>净空：原版主世界噪声配上原版平原与福地平原复刻两个群系的棋盘群系源（{@link #CHECKER_SIZE}），套进有疆域生成器
 *       （先经 {@code createState} 登记，荡魂山照样挑缓地），手建「荡魂山 + 村庄」的结构状态，从种子 1 起找一个原版会在
 *       山心净空圆里生成村庄的种子，对该区块走 {@code createStructures} 后，区块里没有进净空圆的结构起点；要塞与荡魂山
 *       自身不受净空约束。</li>
 *   <li>计时只含噪声与结构片写入；正式生成在 {@code FEATURES} 状态写入、多维护两张高度图，游戏内每座山的实际耗时以结构
 *       片的 INFO 日志为准。</li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoulsunderMountainGameTests {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SEED = 12345L;
    private static final BlockPos ANCHOR = new BlockPos(-2, 40, -3);
    private static final int HEADROOM = 3;
    private static final int ROOF_REACH = 20;
    private static final BlockPos SUMMIT = new BlockPos(4, 118, -2);
    private static final int SUMMIT_RISE = 117;
    private static final int DRY_CHECK = 32;
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
    private static final long[] SITE_SEEDS = {SEED, 10L, 20261120L};
    private static final int VILLAGE_SEEDS = 48;
    private static final int VILLAGE_REACH = 96;
    private static final int DUMP_REACH = 112;
    private static final LevelHeightAccessor HEIGHTS = LevelHeightAccessor.create(-64, 384);

    private SoulsunderMountainGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void registeredAsTheGentleSiteOfTheBlissfulLand(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        StructureSet set = registries.registryOrThrow(Registries.STRUCTURE_SET)
                .getOrThrow(SoulsunderMountain.STRUCTURE_SET);
        helper.assertTrue(set.placement() instanceof GentleSitePlacement, "the set is placed by " + set.placement());
        helper.assertTrue(set.structures().size() == 1
                && set.structures().getFirst().structure().is(SoulsunderMountain.STRUCTURE), "the set lists " + set);
        Structure structure = registries.registryOrThrow(Registries.STRUCTURE).getOrThrow(SoulsunderMountain.STRUCTURE);
        helper.assertTrue(structure.step() == GenerationStep.Decoration.SURFACE_STRUCTURES, "step " + structure.step());
        helper.assertTrue(structure.terrainAdaptation() == TerrainAdjustment.BEARD_THIN,
                "terrain adaptation " + structure.terrainAdaptation());
        Registry<Biome> biomes = registries.registryOrThrow(Registries.BIOME);
        helper.assertTrue(structure.biomes().size() == BlissfulLandBiomes.ROSTER.size()
                && BlissfulLandBiomes.ROSTER.stream().allMatch(key -> structure.biomes()
                .contains(biomes.getHolderOrThrow(key))), "the mountain may stand in " + structure.biomes());
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9", timeoutTicks = 1200)
    public static void siteIsGentleDeterministicAndBlissfulOnly(GameTestHelper helper) {
        StructurePlacement placement = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .getOrThrow(SoulsunderMountain.STRUCTURE_SET).placement();
        for (long seed : SITE_SEEDS) {
            BlissfulLand land = BlissfulLand.create(helper, seed);
            long started = System.nanoTime();
            BlissfulLandSite site = land.generator().getSite(land.random());
            long millis = (System.nanoTime() - started) / 1_000_000;
            BlissfulLandSite again = BlissfulLand.create(helper, seed).site();
            LOGGER.info("soulsunder mountain site (seed {}): {} ms, chunk {}, floor y {}, relief {}, wet {}", seed,
                    millis, site.chunk(), site.floor(), site.relief(), site.wet());
            helper.assertTrue(site.equals(again), "seed " + seed + ": picked " + site + " then " + again);
            helper.assertTrue(Math.abs(site.chunk().x) <= BlissfulLandSite.REACH_CHUNKS
                    && Math.abs(site.chunk().z) <= BlissfulLandSite.REACH_CHUNKS, "seed " + seed + ": " + site
                    + " lies beyond the reach");
            helper.assertTrue(site.isGentle(), "seed " + seed + ": no gentle site, picked " + site);
            helper.assertTrue(site.floor() + SUMMIT_RISE < HEIGHTS.getMaxBuildHeight(),
                    "seed " + seed + ": the summit of " + site + " tops the build limit");
            BlockPos centre = site.centre();
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int x = centre.getX() + dx * DRY_CHECK;
                    int z = centre.getZ() + dz * DRY_CHECK;
                    int surface = land.generator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, HEIGHTS,
                            land.random());
                    int floor = land.generator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, HEIGHTS,
                            land.random());
                    helper.assertTrue(surface == floor, "seed " + seed + ": water stands at " + x + ", " + z);
                }
            }
            ChunkPos chunk = site.chunk();
            helper.assertTrue(placement.isStructureChunk(land.structures(), chunk.x, chunk.z),
                    "seed " + seed + ": the placement misses " + chunk);
            for (ChunkPos other : List.of(new ChunkPos(chunk.x + 1, chunk.z), new ChunkPos(chunk.x, chunk.z - 1),
                    new ChunkPos(0, 0).equals(chunk) ? new ChunkPos(4, 4) : new ChunkPos(0, 0))) {
                helper.assertFalse(placement.isStructureChunk(land.structures(), other.x, other.z),
                        "seed " + seed + ": the placement also hits " + other);
            }
            Overworld overworld = Overworld.create(helper, seed);
            helper.assertTrue(GentleSitePlacement.find(overworld.structures()) == null
                    && !placement.isStructureChunk(overworld.structures(), chunk.x, chunk.z),
                    "seed " + seed + ": the overworld still places the mountain");
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
        BlissfulLandSite site = world.site();
        ChunkPos ring = site.chunk();
        StructureStart start = structure.generate(registries, world.generator(), world.generator().getBiomeSource(),
                world.random(), helper.getLevel().getServer().getStructureManager(), seed, ring, 0, HEIGHTS,
                biome -> structure.biomes().contains(biome));
        helper.assertTrue(start.isValid(), "no valid start at " + ring);
        List<SoulsunderMountainPiece> pieces = start.getPieces().stream()
                .map(SoulsunderMountainPiece.class::cast).toList();
        BlockPos centre = pieces.getFirst().getCentre();
        helper.assertTrue(centre.equals(site.centre()), "the centre " + centre + " is not " + site.centre());
        int boxTop = start.getPieces().stream().mapToInt(piece -> piece.getBoundingBox().maxY()).max().orElseThrow();
        helper.assertTrue(boxTop == centre.getY() + SUMMIT_RISE, "the pieces top out at y " + boxTop
                + ", not the summit");
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
        BlockPos anchor = centre.offset(ANCHOR);
        helper.assertTrue(isStone(chunks, anchor.below())
                        && BlockPos.betweenClosedStream(anchor, anchor.above(HEADROOM - 1))
                        .noneMatch(pos -> isStone(chunks, pos))
                        && BlockPos.betweenClosedStream(anchor.above(HEADROOM), anchor.above(ROOF_REACH))
                        .anyMatch(pos -> isStone(chunks, pos)),
                "the palace anchor " + anchor + " does not stand in a roofed hollow");
        BlockPos standing = centre.offset(SUMMIT);
        helper.assertTrue(isStone(chunks, standing.below()) && !isStone(chunks, standing)
                && standing.getY() - 1 == centre.getY() + SUMMIT_RISE, "the summit stone is not under " + standing);
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
    public static void blissfulLandIsVoidOutside(GameTestHelper helper) {
        BlissfulLand land = BlissfulLand.create(helper, SEED);
        BoundedNoiseGenerator generator = land.generator();
        RandomState random = land.random();
        int edge = BlissfulLandFoxImmortal.HALF_SIZE_CHUNKS;
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
        int edge = BlissfulLandFoxImmortal.HALF_SIZE_CHUNKS * 16 - 1;
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
        BoundedNoiseGenerator generator = new BoundedNoiseGenerator(new CheckerboardColumnBiomeSource(
                HolderSet.direct(biomes.getHolderOrThrow(Biomes.PLAINS),
                        biomes.getHolderOrThrow(Replicas.PLAINS)), CHECKER_SIZE),
                registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD),
                BlissfulLandFoxImmortal.HALF_SIZE_CHUNKS);
        for (long seed = 1; seed <= VILLAGE_SEEDS; seed++) {
            Overworld world = new Overworld(generator, RandomState.create(generator.generatorSettings().value(),
                    registries.lookupOrThrow(Registries.NOISE), seed), null, sets);
            generator.createState(registries.lookupOrThrow(Registries.STRUCTURE_SET), world.random(), seed);
            ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForFlat(world.random(), seed,
                    generator.getBiomeSource(), Stream.of(mountainSet, villages));
            ChunkPos ring = GentleSitePlacement.find(state);
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
            ResourceLocation file = BlissfulLandFoxImmortal.LEVEL_STEM.location()
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
            ChunkGeneratorStructureState structures = generator.createState(
                    registries.lookupOrThrow(Registries.STRUCTURE_SET), random, seed);
            structures.ensureStructuresGenerated();
            return new BlissfulLand(generator, random, structures);
        }

        BlissfulLandSite site() { return generator.getSite(random); }
    }
}
