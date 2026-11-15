package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSea;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlains;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesert;
import net.alex.guzhenrenworld.waterways.Waterways;
import net.alex.guzhenrenworld.waterways.WaterwaysDensity;
import net.alex.guzhenrenworld.waterways.WaterwaysPlan;
import net.alex.guzhenrenworld.waterways.WaterwaysPlanner;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.slf4j.Logger;

/**
 * 水系规划的可选俯视图。
 * <ul>
 *   <li>仅当 GameTest 服务端带 {@code -Dguworld.waterwaysMaps=<directory>} 时单独成批；读区域或写图出错才失败。</li>
 *   <li>四域各经 {@link RegionProbe}（种子 12345）建一次规划，记建图耗时，水、出口与边缘格占比。</li>
 *   <li>{@link #THRESHOLDS} 各汇流阈值记河段格数与总长、河道刻去的区域占比、每侧 {@code max(5, 2w)} 格岸带碰到的占比、
 *       湖中河段格数及最深者、溪流终止的汇点数。</li>
 *   <li>写 {@code <region>.png}（1024 像素对应 8192 格）：地面晕渲，水蓝，出口青（干旱区域土黄），
 *       {@link Waterways#MIN_CATCHMENT} 阈值的溪流画成随宽度加粗的线，穿过深于 1 格的湖处品红点，终止的汇点橙点。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@GameTestHolder(GuWorld.MOD_ID)
public final class WaterwaysPlanMaps {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SWITCH = "guworld.waterwaysMaps";
    private static final int[] THRESHOLDS = {32, 64, 128, 256};
    private static final int IMAGE = 1024;
    private static final double DEEP = 1.0;
    private static final List<ResourceKey<LevelStem>> STEMS = List.of(SouthernBorder.REGION.levelStem(),
            WesternDesert.REGION.levelStem(), EasternSea.REGION.levelStem(), NorthernPlains.REGION.levelStem());

    private WaterwaysPlanMaps() {}

    @GameTestGenerator
    public static List<TestFunction> generateMaps() {
        String directory = System.getProperty(SWITCH);
        if (directory == null || directory.isBlank()) return List.of();
        return List.of(new TestFunction("waterwaysMaps", "waterwaysplanmaps", GuWorld.MOD_ID + ":empty9x9x9", 100,
                0, true, helper -> draw(helper, Path.of(directory))));
    }

    private static void draw(GameTestHelper helper, Path directory) {
        System.setProperty("java.awt.headless", "true");
        for (ResourceKey<LevelStem> stem : STEMS) {
            RegionProbe probe = new RegionProbe(helper, stem, Region.MIN_Y, Region.HEIGHT);
            Waterways.Region region = WaterwaysDensity.find(probe.getRouter())
                    .orElseThrow(() -> new IllegalStateException(stem.location() + " has no waterways")).region();
            long started = System.nanoTime();
            WaterwaysPlan plan = WaterwaysPlanner.plan(region, probe.getGenerator(), probe.getRandomState(),
                    RegionProbe.SEED);
            double seconds = (System.nanoTime() - started) / 1e9;
            LOGGER.info("waterways plan {}: {} s, sea level {}, {}\n  {}", region.name(), "%.1f".formatted(seconds),
                    plan.seaLevel(), kinds(plan), String.join("\n  ", thresholds(plan)));
            write(plan, directory.resolve(region.name() + ".png"));
        }
        helper.succeed();
    }

    private static String kinds(WaterwaysPlan plan) {
        int[] counts = new int[WaterwaysPlan.Kind.values().length];
        for (int cell = 0; cell < plan.count(); cell++) {
            counts[plan.kind(cell).ordinal()]++;
        }
        List<String> parts = new ArrayList<>();
        for (WaterwaysPlan.Kind kind : WaterwaysPlan.Kind.values()) {
            parts.add("%s %.1f%%".formatted(kind.name().toLowerCase(), 100.0 * counts[kind.ordinal()] / plan.count()));
        }
        return String.join(", ", parts);
    }

    private static List<String> thresholds(WaterwaysPlan plan) {
        double area = plan.region().borderSize() * plan.region().borderSize();
        List<String> lines = new ArrayList<>();
        for (int threshold : THRESHOLDS) {
            int streams = 0;
            int fedSinks = 0;
            int deep = 0;
            double deepest = 0;
            double length = 0;
            double carved = 0;
            double corridor = 0;
            for (int cell = 0; cell < plan.count(); cell++) {
                if (plan.kind(cell) == WaterwaysPlan.Kind.SINK && plan.catchment(cell) >= threshold) fedSinks++;
                if (!plan.isStream(cell, threshold)) continue;
                streams++;
                double width = plan.width(cell, threshold);
                double run = plan.length(cell);
                length += run;
                carved += run * width;
                corridor += run * (width + 2 * Math.max(5, 2 * width));
                if (plan.depression(cell) > DEEP) deep++;
                deepest = Math.max(deepest, plan.depression(cell));
            }
            lines.add("catchment >= %3d: %6d stream cells, %6.1f km, carved %.2f%%, corridor %.1f%%, in lakes %d "
                    .formatted(threshold, streams, length / 1000, 100 * carved / area, 100 * corridor / area, deep)
                    + "(deepest %.1f), ending in sinks %d".formatted(deepest, fedSinks));
        }
        return lines;
    }

    private static void write(WaterwaysPlan plan, Path file) {
        BufferedImage image = new BufferedImage(IMAGE, IMAGE, BufferedImage.TYPE_INT_RGB);
        int size = plan.size();
        boolean dry = plan.region().dry();
        for (int py = 0; py < IMAGE; py++) {
            for (int px = 0; px < IMAGE; px++) {
                image.setRGB(px, py, ground(plan, py * size / IMAGE * size + px * size / IMAGE, dry));
            }
        }
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scale = IMAGE / plan.region().borderSize();
        double origin = plan.region().origin();
        graphics.setColor(dry ? new Color(150, 90, 40) : new Color(20, 50, 220));
        for (int cell = 0; cell < plan.count(); cell++) {
            if (!plan.isStream(cell, Waterways.MIN_CATCHMENT) || plan.receiver(cell) < 0) continue;
            int next = plan.receiver(cell);
            graphics.setStroke(new BasicStroke((float) (0.8 + plan.width(cell, Waterways.MIN_CATCHMENT) / 3)));
            graphics.draw(new Line2D.Double((plan.x(cell) - origin) * scale, (plan.z(cell) - origin) * scale,
                    (plan.x(next) - origin) * scale, (plan.z(next) - origin) * scale));
        }
        graphics.setColor(Color.MAGENTA);
        for (int cell = 0; cell < plan.count(); cell++) {
            if (!plan.isStream(cell, Waterways.MIN_CATCHMENT) || plan.depression(cell) <= DEEP) continue;
            graphics.fill(new Ellipse2D.Double((plan.x(cell) - origin) * scale - 1.5,
                    (plan.z(cell) - origin) * scale - 1.5, 3, 3));
        }
        graphics.setColor(Color.ORANGE);
        for (int cell = 0; cell < plan.count(); cell++) {
            if (plan.kind(cell) != WaterwaysPlan.Kind.SINK || plan.catchment(cell) < Waterways.MIN_CATCHMENT) continue;
            graphics.fill(new Ellipse2D.Double((plan.x(cell) - origin) * scale - 2.5,
                    (plan.z(cell) - origin) * scale - 2.5, 5, 5));
        }
        graphics.dispose();
        try {
            Files.createDirectories(file.getParent());
            ImageIO.write(image, "png", file.toFile());
        } catch (IOException exception) {
            throw new UncheckedIOException("cannot write " + file, exception);
        }
    }

    private static int ground(WaterwaysPlan plan, int cell, boolean dry) {
        switch (plan.kind(cell)) {
            case WATER -> {
                return new Color(40, 80, 170).getRGB();
            }
            case OUTLET -> {
                return dry ? new Color(205, 170, 110).getRGB() : new Color(70, 160, 210).getRGB();
            }
            default -> {
            }
        }
        int size = plan.size();
        int northwest = cell - size - 1;
        double fall = northwest >= 0 ? plan.ground(cell) - plan.ground(northwest) : 0;
        double shade = Math.clamp(0.8 + fall * 0.06, 0.45, 1.15);
        double rise = Math.clamp((plan.ground(cell) - plan.seaLevel()) / 200.0, 0, 1);
        int red = (int) Math.clamp((80 + 130 * rise) * shade, 0, 255);
        int green = (int) Math.clamp((125 + 80 * rise) * shade, 0, 255);
        int blue = (int) Math.clamp((65 + 125 * rise) * shade, 0, 255);
        return new Color(red, green, blue).getRGB();
    }
}
