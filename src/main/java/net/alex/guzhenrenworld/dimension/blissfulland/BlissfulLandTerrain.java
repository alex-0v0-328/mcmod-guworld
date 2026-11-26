package net.alex.guzhenrenworld.dimension.blissfulland;

import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.waterways.Waterways;
import net.alex.guzhenrenworld.waterways.WaterwaysDensity;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * 福地的噪声路由：本福地的 Tectonic 副本（原版群系那一版）外面包一层湖，最外层是水系刻河；全域都是天然地形，不围绕荡魂
 * 山整形（荡魂山自己挑缓地落位，见 {@link BlissfulLandSite}）。湖的算术在 {@link BlissfulLandShape}，本类放数值并接线。
 * <ul>
 *   <li>湖：湖泊噪声高于 {@link #LAKE_FROM} 的斑块，在低地（Tectonic 偏移估出的地面不高于 {@link #LOWLAND_TOP}）挖到
 *       {@link #LAKE_BED}，水来自原版含水层。</li>
 *   <li>湖岸的顶面随湖系数变小抬高 {@link #SHORE_RISE} 格，斑块边缘比原地形高，不削出台阶。</li>
 *   <li>Tectonic 深度为 {@code (128 − y) / 128} 加偏移，故偏移 {@code v} 处地面约在 {@code y = 128 + 128v}
 *       （{@link #OFFSET_ZERO} 与 {@link #BLOCKS_PER_OFFSET}）。</li>
 *   <li>偏干：路由的湿度整体减 {@link #DRYNESS}，原版表里偏湿的森林、针叶林、黑森林一档档让给平原与草甸（草原地貌，
 *       Alex 定）。只有群系取气候读路由的湿度，地形与刻河都不读，所以只换群系、不动地形。</li>
 *   <li>⚠ 含水层只在「初估地表 + 8」低于海平面时按海平面放水；初估地表读 {@code initial_density_without_jaggedness}
 *       高于 {@link #PRELIMINARY} 的最高处，所以湖处初估密度再压低 {@link #LAKE_FLAG} 格。</li>
 * </ul>
 */

public final class BlissfulLandTerrain {

    static final double SLOPE = 1.0 / 16;
    static final double PRELIMINARY = 0.390625;
    static final double LAKE_BED = 55.0;
    static final double LAKE_FROM = 0.45;
    static final double LAKE_FULL = 0.6;
    static final double LAKE_FLAG = 10.0;
    static final double LOWLAND_TOP = 75.0;
    static final double LOWLAND_FADE = 80.0;
    static final double SHORE = 4.0;
    static final double SHORE_RISE = 16.0;
    static final double OFFSET_ZERO = 128.0;
    static final double BLOCKS_PER_OFFSET = 128.0;
    static final double DRYNESS = 0.2;
    private static final String OFFSET = "terrain/tectonic/terrain_spline/offset/final";

    private BlissfulLandTerrain() {}

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context, String name) {
        context.register(getNoiseKey(name, "lakes"), new NormalNoise.NoiseParameters(-7, 1.0));
    }

    public static NoiseRouter getRouter(BootstrapContext<?> context, String name, Waterways.Region waterways) {
        NoiseRouter ported = Ports.getNoiseRouter(context, name);
        DensityFunction offset = new DensityFunctions.HolderHolder(context.lookup(Registries.DENSITY_FUNCTION)
                .getOrThrow(Ports.getRegionalKey(name, OFFSET)));
        DensityFunction.NoiseHolder lakes = new DensityFunction.NoiseHolder(context.lookup(Registries.NOISE)
                .getOrThrow(getNoiseKey(name, "lakes")));
        NoiseRouter shaped = new NoiseRouter(ported.barrierNoise(), ported.fluidLevelFloodednessNoise(),
                ported.fluidLevelSpreadNoise(), ported.lavaNoise(), ported.temperature(),
                DensityFunctions.add(ported.vegetation(), DensityFunctions.constant(-DRYNESS)), ported.continents(),
                ported.erosion(), ported.depth(), ported.ridges(),
                new BlissfulLandShape(BlissfulLandShape.Kind.INITIAL, ported.initialDensityWithoutJaggedness(), offset,
                        lakes),
                new BlissfulLandShape(BlissfulLandShape.Kind.FINAL, ported.finalDensity(), offset, lakes),
                ported.veinToggle(), ported.veinRidged(), ported.veinGap());
        return WaterwaysDensity.getCarved(context, shaped, waterways);
    }

    private static ResourceKey<NormalNoise.NoiseParameters> getNoiseKey(String name, String part) {
        return GuWorld.key(Registries.NOISE, "terrain/" + name + "/" + part);
    }
}
