package net.alex.guzhenrenworld.gametest;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.TreeMap;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

/**
 * 单个区域地形的 golden master。
 * <ul>
 *   <li>并入区域测试本就做的 32×32 列扫描；只看采样列的基础高度与群系，不看洞穴、表层规则、地物、水系。</li>
 *   <li>各采样列的海底高度、地表高度、地表群系按扫描顺序进 64 位 FNV-1a 哈希，两种高度另累加成和作诊断。</li>
 *   <li>{@link #verify} 拿三者比对 {@value #RESOURCE} 的 {@code <region>.hash}、{@code <region>.floorSum}、
 *       {@code <region>.surfaceSum}，重构挪动地形时失败并给出期望与实际值；有意改地形时更新该文件。</li>
 *   <li>带 {@code -Dguworld.recordGolden=true} 时按该文件格式记 {@code golden-record <key>=<value>} 行，不比对。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WorldgenGolden {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RECORD_SWITCH = "guworld.recordGolden";
    private static final String RESOURCE = "worldgen_golden.properties";
    private static final String SOURCE = "src/test/game/resources/net/alex/guzhenrenworld/gametest/" + RESOURCE;
    private static final long OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long PRIME = 0x100000001b3L;

    private final String region;
    private long hash = OFFSET_BASIS;
    private long floorSum;
    private long surfaceSum;

    public WorldgenGolden(String region) {
        this.region = region;
    }

    public void add(int floorHeight, int surfaceHeight, Holder<Biome> biome) {
        floorSum += floorHeight;
        surfaceSum += surfaceHeight;
        mix(floorHeight);
        mix(surfaceHeight);
        String name = biome.unwrapKey().orElseThrow().location().toString();
        for (int i = 0; i < name.length(); i++) {
            mix(name.charAt(i));
        }
        mix(name.length());
    }

    public void verify(GameTestHelper helper) {
        TreeMap<String, String> actual = new TreeMap<>();
        actual.put(region + ".hash", "%016x".formatted(hash));
        actual.put(region + ".floorSum", Long.toString(floorSum));
        actual.put(region + ".surfaceSum", Long.toString(surfaceSum));
        if (Boolean.getBoolean(RECORD_SWITCH)) {
            actual.forEach((key, value) -> LOGGER.info("golden-record {}={}", key, value));
            return;
        }
        Properties expected = read();
        StringBuilder mismatches = new StringBuilder();
        actual.forEach((key, value) -> {
            String wanted = expected.getProperty(key);
            if (!value.equals(wanted)) mismatches.append("\n  ").append(key).append(": expected ").append(wanted)
                    .append(", actual ").append(value);
        });
        helper.assertTrue(mismatches.isEmpty(), region + " terrain differs from the golden master:" + mismatches
                + "\nIf the change is deliberate, run with JAVA_TOOL_OPTIONS=-D" + RECORD_SWITCH
                + "=true and copy the golden-record lines of the log into " + SOURCE);
    }

    private void mix(int value) {
        for (int shift = 0; shift < 32; shift += 8) {
            hash = (hash ^ (value >>> shift & 0xff)) * PRIME;
        }
    }

    private static Properties read() {
        Properties properties = new Properties();
        try (InputStream stream = WorldgenGolden.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("missing test resource " + RESOURCE);
            properties.load(stream);
        } catch (IOException exception) {
            throw new UncheckedIOException("cannot read " + RESOURCE, exception);
        }
        return properties;
    }
}
