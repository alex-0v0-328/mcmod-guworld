package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSea;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlains;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesert;
import net.alex.guzhenrenworld.waterways.Waterways;
import net.alex.guzhenrenworld.waterways.WaterwaysCarver;
import net.alex.guzhenrenworld.waterways.WaterwaysDensity;
import net.alex.guzhenrenworld.waterways.WaterwaysFeature;
import net.alex.guzhenrenworld.waterways.WaterwaysIndex;
import net.alex.guzhenrenworld.waterways.WaterwaysRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 水系刻河前后的可选效果图。
 * <ul>
 *   <li>仅当 GameTest 服务端带 {@code -Dguworld.waterwaysCarve=<directory>} 时单独成批；四域各经 {@link RegionProbe}
 *       （种子 12345）建雕刻器索引，按 128 格扫描挑线段总长最大、与区块对齐的 256 格窗口。</li>
 *   <li>窗口 16×16 区块照 {@link SouthernBorderNoiseBenchmark} 填进 {@link ProtoChunk} 两遍：先由包装原样放过地形，
 *       再在区域 {@link RandomState} 上挂雕刻器；每遍先空跑一排区块。</li>
 *   <li>记建图耗时、全区与窗口线段数、窗口内 {@code OCEAN_FLOOR_WG} 变动列占比及降、升列数（刻河只削，升应为 0）、
 *       最大与平均下切、{@link WaterwaysCarver#getSurface(int, int)} 有限的列、前后每区块毫秒数。</li>
 *   <li>写 {@code <region>-before.png}、{@code <region>-after.png}（海底晕渲，{@code WORLD_SURFACE_WG} 高于海底处水蓝，
 *       每格 2 像素）与 {@code <region>-cut.png}（下切深度：0 灰，1 到 8 及以上黄渐红，抬高品红）。</li>
 *   <li>写 {@code <region>-section.png}：窗口最长三条线段中点的高度剖面，每侧 24 格，每格 8 像素；灰为刻前，黑为刻后，
 *       蓝为刻后水面。</li>
 *   <li>放水：窗口区块挂雕刻器重填，逐个跑 {@link WaterwaysFeature#fill(WaterwaysCarver, ChunkPos, BlockGetter, java.util.function.BiConsumer)} 并计时；不跑表层规则，故河床替换石头。</li>
 *   <li>记河道列（湿河道分有水、缺水、溢出，溢出指底低于刻河河底而留干；干沟另计）、海平面以上河道外水列、水深、
 *       窗口内海平面及以上侧面挨空格的水块（地物在此排流体刻：台阶、矮岸与溢出）、河床方块直方图。</li>
 *   <li>写 {@code <region>-water.png}：每格 1 像素，地面晕渲，水蓝且越深越暗，干沟沙色，露空水面红，河道外水品红；
 *       另记按宽度分档的线段长度及 3 到 7 格宽的累计长度；只在文件写不出时失败。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(GuWorld.MOD_ID)
public final class WaterwaysCarveMaps {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SWITCH = "guworld.waterwaysCarve";
    private static final int WINDOW = 256;
    private static final int CHUNKS = WINDOW / 16;
    private static final int BIN = 128;
    private static final int SCALE = 2;
    private static final int IMAGE = WINDOW * SCALE;
    private static final int HALF = 24;
    private static final int PIXELS = 8;
    private static final int MARGIN = 16;
    private static final int GAP = 8;
    private static final int PROFILES = 3;
    private static final int EDGE = HALF + 2;
    private static final int MAX_CUT = 8;
    private static final int BED_LIST = 5;
    private static final double[] WIDTH_EDGES = {2, 3, 4, 5, 6};
    private static final double[] WIDTH_STEPS = {3, 4, 5, 6, 7};
    private static final String[] WIDTH_BINS = {"[1.5,2)", "[2,3)", "[3,4)", "[4,5)", "[5,6)", "[6,7]"};
    private static final List<Stem> STEMS = List.of(
            new Stem(SouthernBorder.REGION.levelStem(), Region.MIN_Y, Region.HEIGHT),
            new Stem(WesternDesert.REGION.levelStem(), Region.MIN_Y, Region.HEIGHT),
            new Stem(EasternSea.REGION.levelStem(), Region.MIN_Y, Region.HEIGHT),
            new Stem(NorthernPlains.REGION.levelStem(), Region.MIN_Y, Region.HEIGHT));

    private WaterwaysCarveMaps() {}

    @GameTestGenerator
    public static List<TestFunction> generateMaps() {
        String directory = System.getProperty(SWITCH);
        if (directory == null || directory.isBlank()) return List.of();
        return List.of(new TestFunction("waterwaysCarve", "waterwayscarvemaps", GuWorld.MOD_ID + ":empty9x9x9", 1200,
                0, true, helper -> draw(helper, Path.of(directory))));
    }

    private static void draw(GameTestHelper helper, Path directory) {
        System.setProperty("java.awt.headless", "true");
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (Stem stem : STEMS) {
            RegionProbe probe = new RegionProbe(helper, stem.key(), stem.minY(), stem.height());

            //region Carver [雕刻器]
            Waterways.Region region = WaterwaysDensity.find(probe.getRouter())
                    .orElseThrow(() -> new IllegalStateException(stem.key().location() + " has no waterways density"))
                    .region();
            WaterwaysCarver carver = WaterwaysCarver.create(region, probe.getGenerator(), probe.getRandomState(),
                    RegionProbe.SEED);
            long started = System.nanoTime();
            WaterwaysIndex index = carver.getIndex();
            double seconds = (System.nanoTime() - started) / 1e9;
            Window window = pick(region, index);
            //endregion

            //region Fill [填充]
            Terrain terrain = new Terrain(probe.getGenerator(), probe.getRandomState(), biomes,
                    LevelHeightAccessor.create(stem.minY(), stem.height()), noStructures());
            Columns before = fill(terrain, window);
            Columns after;
            WaterwaysRuntime.attach(probe.getRandomState(), carver);
            try {
                after = fill(terrain, window);
            } finally {
                WaterwaysRuntime.detach(probe.getRandomState());
            }
            //endregion

            //region Statistics [统计]
            int down = 0;
            int up = 0;
            int deepest = 0;
            int finite = 0;
            long cut = 0;
            for (int z = 0; z < WINDOW; z++) {
                for (int x = 0; x < WINDOW; x++) {
                    int difference = before.floor()[z * WINDOW + x] - after.floor()[z * WINDOW + x];
                    if (difference > 0) {
                        down++;
                        cut += difference;
                        deepest = Math.max(deepest, difference);
                    } else if (difference < 0) {
                        up++;
                    }
                    if (Double.isFinite(carver.getSurface(window.x() + x, window.z() + z))) finite++;
                }
            }
            int columns = WINDOW * WINDOW;
            LOGGER.info("waterways carve {}: build {} s, {} segments, window x {} z {} ({} segments, {} blocks), "
                            + "changed {}% ({} down, {} up), cut max {} mean {}, surface columns {}, ms per chunk {} "
                            + "before, {} after, overhead {}%", region.name(), "%.1f".formatted(seconds),
                    index.segments(), window.x(), window.z(), window.segments(), "%.0f".formatted(window.length()),
                    "%.2f".formatted(100.0 * (down + up) / columns), down, up, deepest,
                    "%.2f".formatted(down > 0 ? (double) cut / down : 0.0), finite,
                    "%.1f".formatted(before.millis()), "%.1f".formatted(after.millis()),
                    "%.1f".formatted(100.0 * (after.millis() / before.millis() - 1)));
            //endregion

            //region Files [文件]
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int i = 0; i < columns; i++) {
                min = Math.min(min, Math.min(before.floor()[i], after.floor()[i]));
                max = Math.max(max, Math.max(before.floor()[i], after.floor()[i]));
            }
            write(heights(before, min, max), directory.resolve(region.name() + "-before.png"));
            write(heights(after, min, max), directory.resolve(region.name() + "-after.png"));
            write(cuts(before, after), directory.resolve(region.name() + "-cut.png"));
            List<Section> sections = sections(index, window, before, after);
            if (sections.isEmpty()) {
                LOGGER.info("waterways carve {}: no segment with room for a section in the window", region.name());
            } else {
                write(profiles(sections), directory.resolve(region.name() + "-section.png"));
                for (Section section : sections) {
                    LOGGER.info("waterways carve {} section: segment {} at x {} z {}, {} blocks long, ground {} to "
                                    + "{} before, {} to {} after", region.name(), section.segment(),
                            "%.0f".formatted(section.x()), "%.0f".formatted(section.z()),
                            "%.0f".formatted(section.length()), range(section.before(), true),
                            range(section.before(), false), range(section.after(), true),
                            range(section.after(), false));
                }
            }
            //endregion

            //region Water [水体]
            water(terrain, carver, window, directory);
            //endregion
        }
        helper.succeed();
    }

    private static void water(Terrain terrain, WaterwaysCarver carver, Window window, Path directory) {
        String name = carver.region().name();
        widths(name, carver.getIndex());
        int chunkX = window.x() >> 4;
        int chunkZ = window.z() >> 4;
        ProtoChunk[] chunks = new ProtoChunk[CHUNKS * CHUNKS];
        WaterwaysRuntime.attach(terrain.random(), carver);
        try {
            for (int cz = 0; cz < CHUNKS; cz++) {
                for (int cx = 0; cx < CHUNKS; cx++) {
                    chunks[cz * CHUNKS + cx] = chunk(terrain, chunkX + cx, chunkZ + cz);
                }
            }
        } finally {
            WaterwaysRuntime.detach(terrain.random());
        }
        WindowBlocks blocks = new WindowBlocks(chunks, chunkX, chunkZ, terrain.column());
        long started = System.nanoTime();
        for (ProtoChunk chunk : chunks) {
            WaterwaysFeature.fill(carver, chunk.getPos(), blocks, blocks::set);
        }
        double millis = (System.nanoTime() - started) / 1e6 / chunks.length;
        int sea = terrain.generator().getSeaLevel();
        int columns = WINDOW * WINDOW;
        int[] floor = new int[columns];
        int[] surface = new int[columns];
        int[] depths = new int[columns];
        boolean[] open = new boolean[columns];
        boolean[] stray = new boolean[columns];
        boolean[] dryBed = new boolean[columns];
        Map<String, Integer> beds = new HashMap<>();
        int channels = 0;
        int wet = 0;
        int missing = 0;
        int spilled = 0;
        int dry = 0;
        int strays = 0;
        int uncontained = 0;
        int deepest = 0;
        long depthSum = 0;
        int watered = 0;
        for (int z = 0; z < WINDOW; z++) {
            for (int x = 0; x < WINDOW; x++) {
                int wx = window.x() + x;
                int wz = window.z() + z;
                int column = z * WINDOW + x;
                ProtoChunk chunk = blocks.chunkAt(wx, wz);
                floor[column] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, wx & 15, wz & 15);
                surface[column] = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, wx & 15, wz & 15);
                int bed = floor[column];
                int top = surface[column];
                int depth = 0;
                for (int y = top; y > bed && isWater(blocks.getBlockState(new BlockPos(wx, y, wz))); y--) {
                    depth++;
                }
                depths[column] = depth;
                for (int y = top; y > top - depth; y--) {
                    if (y >= sea && blocks.touchesOpen(wx, y, wz)) {
                        uncontained++;
                        open[column] = true;
                    }
                }
                WaterwaysIndex.Channel channel = carver.getChannel(wx, wz);
                if (channel == null) {
                    if (depth > 0 && top >= sea) {
                        strays++;
                        stray[column] = true;
                    }
                    continue;
                }
                channels++;
                String block = BuiltInRegistries.BLOCK.getKey(blocks.getBlockState(new BlockPos(wx, bed, wz))
                        .getBlock()).getPath();
                beds.merge(block, 1, Integer::sum);
                if (!channel.wet()) {
                    dry++;
                    dryBed[column] = true;
                    continue;
                }
                wet++;
                if (depth == 0 && bed < Mth.ceil(channel.bottom()) - 2) {
                    spilled++;
                } else if (depth == 0) {
                    missing++;
                } else {
                    watered++;
                    depthSum += depth;
                    deepest = Math.max(deepest, depth);
                }
            }
        }
        String histogram = beds.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(BED_LIST).map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
        LOGGER.info("waterways water {}: window x {} z {}, fill {} ms per chunk, channel {}, wet {} (missing {}, "
                        + "spilled {}), dry {}, stray {}, depth max {} mean {}, open {}, beds {}", name, window.x(),
                window.z(), "%.2f".formatted(millis), channels, wet, missing, spilled, dry, strays, deepest,
                "%.2f".formatted(watered > 0 ? (double) depthSum / watered : 0.0), uncontained, histogram);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < columns; i++) {
            min = Math.min(min, floor[i]);
            max = Math.max(max, floor[i]);
        }
        Columns terrainColumns = new Columns(floor, surface, 0);
        BufferedImage image = new BufferedImage(WINDOW, WINDOW, BufferedImage.TYPE_INT_RGB);
        for (int z = 0; z < WINDOW; z++) {
            for (int x = 0; x < WINDOW; x++) {
                int column = z * WINDOW + x;
                int rgb;
                if (open[column]) {
                    rgb = Color.RED.getRGB();
                } else if (stray[column]) {
                    rgb = Color.MAGENTA.getRGB();
                } else if (depths[column] > 0) {
                    double factor = Math.clamp(1 - depths[column] * 0.12, 0.35, 1.0);
                    rgb = new Color((int) (40 * factor), (int) (90 * factor), (int) (200 * factor)).getRGB();
                } else if (dryBed[column]) {
                    rgb = new Color(222, 205, 140).getRGB();
                } else {
                    rgb = ground(terrainColumns, x, z, min, max);
                }
                image.setRGB(x, z, rgb);
            }
        }
        write(image, directory.resolve(name + "-water.png"));
    }

    private static boolean isWater(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    private static void widths(String name, WaterwaysIndex index) {
        double[] bins = new double[WIDTH_BINS.length];
        double[] atLeast = new double[WIDTH_STEPS.length];
        double narrow = 0;
        double total = 0;
        for (int s = 0; s < index.segments(); s++) {
            double length = length(index, s);
            double width = ((double) index.width0(s) + index.width1(s)) / 2;
            total += length;
            if (width < 1.5) {
                narrow += length;
            } else {
                int bin = 0;
                while (bin < WIDTH_EDGES.length && width >= WIDTH_EDGES[bin]) bin++;
                bins[bin] += length;
            }
            for (int i = 0; i < WIDTH_STEPS.length; i++) {
                if (width >= WIDTH_STEPS[i]) atLeast[i] += length;
            }
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < bins.length; i++) {
            text.append(WIDTH_BINS[i]).append(' ').append("%.0f".formatted(bins[i])).append(", ");
        }
        text.append("below 1.5 ").append("%.0f".formatted(narrow)).append(" blocks; cumulative");
        for (int i = 0; i < atLeast.length; i++) {
            text.append(i == 0 ? " " : ", ").append("at least ").append("%.0f".formatted(WIDTH_STEPS[i])).append(' ')
                    .append("%.2f".formatted(atLeast[i] / 1000)).append(" km (")
                    .append("%.1f".formatted(total > 0 ? 100 * atLeast[i] / total : 0.0)).append("%)");
        }
        LOGGER.info("waterways widths {}: total {} blocks ({} km), bins in blocks {}", name, "%.0f".formatted(total),
                "%.2f".formatted(total / 1000), text);
    }

    private static Window pick(Waterways.Region region, WaterwaysIndex index) {
        int bins = (int) (region.borderSize() / BIN);
        int origin = region.origin();
        double[] lengths = new double[bins * bins];
        for (int s = 0; s < index.segments(); s++) {
            int bx = Math.clamp((long) Math.floor((midX(index, s) - origin) / BIN), 0, bins - 1);
            int bz = Math.clamp((long) Math.floor((midZ(index, s) - origin) / BIN), 0, bins - 1);
            lengths[bz * bins + bx] += length(index, s);
        }
        int side = WINDOW / BIN;
        int bestX = 0;
        int bestZ = 0;
        double best = -1;
        for (int bz = 0; bz <= bins - side; bz++) {
            for (int bx = 0; bx <= bins - side; bx++) {
                double sum = 0;
                for (int dz = 0; dz < side; dz++) {
                    for (int dx = 0; dx < side; dx++) {
                        sum += lengths[(bz + dz) * bins + bx + dx];
                    }
                }
                if (sum > best) {
                    best = sum;
                    bestX = bx;
                    bestZ = bz;
                }
            }
        }
        int x = origin + bestX * BIN;
        int z = origin + bestZ * BIN;
        int inside = 0;
        double length = 0;
        for (int s = 0; s < index.segments(); s++) {
            if (!within(index, s, x, z, 0)) continue;
            inside++;
            length += length(index, s);
        }
        return new Window(x, z, inside, length);
    }

    private static boolean within(WaterwaysIndex index, int segment, int x, int z, int edge) {
        double mx = midX(index, segment);
        double mz = midZ(index, segment);
        return mx >= x + edge && mx < x + WINDOW - edge && mz >= z + edge && mz < z + WINDOW - edge;
    }

    private static double midX(WaterwaysIndex index, int segment) {
        return (index.x0(segment) + (double) index.x1(segment)) / 2;
    }

    private static double midZ(WaterwaysIndex index, int segment) {
        return (index.z0(segment) + (double) index.z1(segment)) / 2;
    }

    private static double length(WaterwaysIndex index, int segment) {
        return Math.hypot((double) index.x1(segment) - index.x0(segment),
                (double) index.z1(segment) - index.z0(segment));
    }

    private static StructureManager noStructures() {
        return new StructureManager(null, null, null) {

            @Override
            public @NotNull List<StructureStart> startsForStructure(
                    @NotNull ChunkPos position, @NotNull Predicate<Structure> predicate) {
                return List.of();
            }
        };
    }

    private static ProtoChunk chunk(Terrain terrain, int chunkX, int chunkZ) {
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(chunkX, chunkZ), UpgradeData.EMPTY, terrain.column(),
                terrain.biomes(), null);
        terrain.generator().fillFromNoise(Blender.empty(), terrain.random(), terrain.structures(), chunk).join();
        return chunk;
    }

    private static Columns fill(Terrain terrain, Window window) {
        int chunkX = window.x() >> 4;
        int chunkZ = window.z() >> 4;
        for (int i = 0; i < CHUNKS; i++) {
            chunk(terrain, chunkX + i, chunkZ);
        }
        int[] floor = new int[WINDOW * WINDOW];
        int[] surface = new int[WINDOW * WINDOW];
        long nanos = 0;
        for (int cz = 0; cz < CHUNKS; cz++) {
            for (int cx = 0; cx < CHUNKS; cx++) {
                long started = System.nanoTime();
                ProtoChunk chunk = chunk(terrain, chunkX + cx, chunkZ + cz);
                nanos += System.nanoTime() - started;
                for (int dz = 0; dz < 16; dz++) {
                    for (int dx = 0; dx < 16; dx++) {
                        int index = (cz * 16 + dz) * WINDOW + cx * 16 + dx;
                        floor[index] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, dx, dz);
                        surface[index] = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, dx, dz);
                    }
                }
            }
        }
        return new Columns(floor, surface, nanos / 1e6 / (CHUNKS * CHUNKS));
    }

    private static BufferedImage heights(Columns columns, int min, int max) {
        BufferedImage image = new BufferedImage(IMAGE, IMAGE, BufferedImage.TYPE_INT_RGB);
        for (int z = 0; z < WINDOW; z++) {
            for (int x = 0; x < WINDOW; x++) {
                int index = z * WINDOW + x;
                int rgb;
                int depth = columns.surface()[index] - columns.floor()[index];
                if (depth > 0) {
                    double factor = Math.clamp(1 - depth * 0.04, 0.5, 1.0);
                    rgb = new Color((int) (40 * factor), (int) (90 * factor), (int) (200 * factor)).getRGB();
                } else {
                    rgb = ground(columns, x, z, min, max);
                }
                for (int py = 0; py < SCALE; py++) {
                    for (int px = 0; px < SCALE; px++) {
                        image.setRGB(x * SCALE + px, z * SCALE + py, rgb);
                    }
                }
            }
        }
        return image;
    }

    private static int ground(Columns columns, int x, int z, int min, int max) {
        int index = z * WINDOW + x;
        double fall = x > 0 && z > 0 ? columns.floor()[index] - columns.floor()[index - WINDOW - 1] : 0;
        double shade = Math.clamp(0.85 + fall * 0.08, 0.5, 1.2);
        double rise = (columns.floor()[index] - min) / (double) Math.max(1, max - min);
        return new Color((int) Math.clamp((80 + 130 * rise) * shade, 0, 255),
                (int) Math.clamp((125 + 80 * rise) * shade, 0, 255),
                (int) Math.clamp((65 + 125 * rise) * shade, 0, 255)).getRGB();
    }

    private static BufferedImage cuts(Columns before, Columns after) {
        BufferedImage image = new BufferedImage(IMAGE, IMAGE, BufferedImage.TYPE_INT_RGB);
        for (int z = 0; z < WINDOW; z++) {
            for (int x = 0; x < WINDOW; x++) {
                int index = z * WINDOW + x;
                int rgb = cutColor(before.floor()[index] - after.floor()[index]);
                for (int py = 0; py < SCALE; py++) {
                    for (int px = 0; px < SCALE; px++) {
                        image.setRGB(x * SCALE + px, z * SCALE + py, rgb);
                    }
                }
            }
        }
        return image;
    }

    private static int cutColor(int depth) {
        if (depth < 0) return Color.MAGENTA.getRGB();
        if (depth == 0) return new Color(70, 70, 70).getRGB();
        double t = (Math.min(depth, MAX_CUT) - 1) / (double) (MAX_CUT - 1);
        return new Color((int) (255 - 55 * t), (int) (240 * (1 - t)), (int) (120 * (1 - t))).getRGB();
    }

    private static List<Section> sections(WaterwaysIndex index, Window window, Columns before, Columns after) {
        List<Integer> candidates = new ArrayList<>();
        for (int s = 0; s < index.segments(); s++) {
            if (within(index, s, window.x(), window.z(), EDGE) && length(index, s) > 1e-3) candidates.add(s);
        }
        candidates.sort(Comparator.<Integer>comparingDouble(s -> length(index, s)).reversed());
        List<Section> sections = new ArrayList<>();
        for (int s : candidates.subList(0, Math.min(PROFILES, candidates.size()))) {
            double length = length(index, s);
            double nx = -((double) index.z1(s) - index.z0(s)) / length;
            double nz = ((double) index.x1(s) - index.x0(s)) / length;
            double mx = midX(index, s);
            double mz = midZ(index, s);
            int[] floorBefore = new int[2 * HALF];
            int[] floorAfter = new int[2 * HALF];
            int[] water = new int[2 * HALF];
            for (int k = 0; k < 2 * HALF; k++) {
                double offset = k - HALF + 0.5;
                int x = Math.clamp((long) Math.floor(mx + nx * offset) - window.x(), 0, WINDOW - 1);
                int z = Math.clamp((long) Math.floor(mz + nz * offset) - window.z(), 0, WINDOW - 1);
                int column = z * WINDOW + x;
                floorBefore[k] = before.floor()[column] + 1;
                floorAfter[k] = after.floor()[column] + 1;
                water[k] = after.surface()[column] > after.floor()[column] ? after.surface()[column] + 1
                        : Integer.MIN_VALUE;
            }
            sections.add(new Section(s, mx, mz, length, floorBefore, floorAfter, water));
        }
        return sections;
    }

    private static String range(int[] tops, boolean low) {
        int value = low ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        for (int top : tops) {
            value = low ? Math.min(value, top) : Math.max(value, top);
        }
        return Integer.toString(value);
    }

    private static BufferedImage profiles(List<Section> sections) {
        int width = 2 * HALF * PIXELS + 2 * MARGIN;
        int[] lows = new int[sections.size()];
        int[] highs = new int[sections.size()];
        int height = GAP;
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            int low = Integer.MAX_VALUE;
            int high = Integer.MIN_VALUE;
            for (int k = 0; k < 2 * HALF; k++) {
                low = Math.min(low, Math.min(section.before()[k], section.after()[k]));
                high = Math.max(high, Math.max(section.before()[k], section.after()[k]));
                if (section.water()[k] != Integer.MIN_VALUE) high = Math.max(high, section.water()[k]);
            }
            lows[i] = low - 2;
            highs[i] = high + 2;
            height += (highs[i] - lows[i]) * PIXELS + GAP;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(245, 245, 240));
        graphics.fillRect(0, 0, width, height);
        int top = GAP;
        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            int panel = (highs[i] - lows[i]) * PIXELS;
            graphics.setColor(Color.WHITE);
            graphics.fillRect(MARGIN, top, 2 * HALF * PIXELS, panel);
            graphics.setColor(new Color(225, 225, 220));
            for (int y = Math.ceilDiv(lows[i], 8) * 8; y <= highs[i]; y += 8) {
                int py = top + (highs[i] - y) * PIXELS;
                graphics.draw(new Line2D.Float(MARGIN, py, MARGIN + 2 * HALF * PIXELS, py));
            }
            graphics.draw(new Line2D.Float(MARGIN + HALF * PIXELS, top, MARGIN + HALF * PIXELS, top + panel));
            graphics.setColor(new Color(110, 160, 235, 150));
            for (int k = 0; k < 2 * HALF; k++) {
                if (section.water()[k] == Integer.MIN_VALUE) continue;
                int py = top + (highs[i] - section.water()[k]) * PIXELS;
                graphics.fillRect(MARGIN + k * PIXELS, py, PIXELS, (section.water()[k] - section.after()[k]) * PIXELS);
            }
            graphics.setStroke(new BasicStroke(2));
            graphics.setColor(new Color(150, 150, 150));
            graphics.draw(steps(section.before(), top, highs[i]));
            graphics.setColor(Color.BLACK);
            graphics.draw(steps(section.after(), top, highs[i]));
            graphics.setColor(new Color(30, 70, 220));
            for (int k = 0; k < 2 * HALF; k++) {
                if (section.water()[k] == Integer.MIN_VALUE) continue;
                float py = top + (highs[i] - section.water()[k]) * PIXELS;
                graphics.draw(new Line2D.Float(MARGIN + k * PIXELS, py, MARGIN + (k + 1) * PIXELS, py));
            }
            graphics.setStroke(new BasicStroke(1));
            top += panel + GAP;
        }
        graphics.dispose();
        return image;
    }

    private static Path2D steps(int[] tops, int panelTop, int high) {
        Path2D path = new Path2D.Float();
        for (int k = 0; k < tops.length; k++) {
            float px = MARGIN + k * PIXELS;
            float py = panelTop + (high - tops[k]) * PIXELS;
            if (k == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
            path.lineTo(px + PIXELS, py);
        }
        return path;
    }

    private static void write(BufferedImage image, Path file) {
        try {
            Files.createDirectories(file.getParent());
            ImageIO.write(image, "png", file.toFile());
        } catch (IOException exception) {
            throw new UncheckedIOException("cannot write " + file, exception);
        }
    }

    private record Stem(ResourceKey<LevelStem> key, int minY, int height) {}

    private record WindowBlocks(ProtoChunk[] chunks, int chunkX, int chunkZ, LevelHeightAccessor column)
            implements BlockGetter {

        private ProtoChunk chunkAt(int x, int z) {
            int cx = (x >> 4) - chunkX;
            int cz = (z >> 4) - chunkZ;
            return cx < 0 || cz < 0 || cx >= CHUNKS || cz >= CHUNKS ? null : chunks[cz * CHUNKS + cx];
        }

        private void set(BlockPos pos, BlockState state) {
            ProtoChunk chunk = chunkAt(pos.getX(), pos.getZ());
            if (chunk != null) chunk.setBlockState(pos, state, false);
        }

        private boolean touchesOpen(int x, int y, int z) {
            return WaterwaysFeature.touchesOpen(this, new BlockPos(x, y, z));
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(@NotNull BlockPos pos) {
            return null;
        }

        @Override
        public @NotNull BlockState getBlockState(BlockPos pos) {
            ProtoChunk chunk = chunkAt(pos.getX(), pos.getZ());
            return chunk == null ? Blocks.STONE.defaultBlockState() : chunk.getBlockState(pos);
        }

        @Override
        public @NotNull FluidState getFluidState(@NotNull BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return column.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return column.getMinBuildHeight();
        }
    }

    private record Window(int x, int z, int segments, double length) {}

    private record Terrain(NoiseBasedChunkGenerator generator, RandomState random, Registry<Biome> biomes,
                           LevelHeightAccessor column, StructureManager structures) {}

    private record Columns(int[] floor, int[] surface, double millis) {}

    private record Section(int segment, double x, double z, double length, int[] before, int[] after, int[] water) {}
}
