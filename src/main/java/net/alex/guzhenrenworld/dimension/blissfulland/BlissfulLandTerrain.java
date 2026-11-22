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
 * 福地的噪声路由：本福地的 Tectonic 副本（原版群系那一版）外面包一层平地圈和湖，最外层是水系刻河；整形的算术在
 * {@link BlissfulLandShape}，本类放数值并接线。
 * <ul>
 *   <li>平地圈：最终密度夹在只随水平位置变的地板和顶面之间，地面就是原地形夹在两者之间。离山心（原点区块中心 (8, 8)）
 *       {@link #FLAT} 格内地板、顶面都在 y 64，是一块平地；顶面到 {@link #RING} 格按距离平方的比例的平方升高
 *       {@link #CEILING_RISE} 格，把山削成往外升的碗沿；地板在 {@link #RING} 格内钉在 y 64，圈内没有河、湖和水，到
 *       {@link #MARGIN} 格降下 {@link #FLOOR_DROP} 格，低处回到原地形。荡魂山、主模组的入口落点和田区都在这块平地上。</li>
 *   <li>平地圈的群系：圈内把路由的大陆度压到 {@link #RING_CONTINENTS}，群系表里只有平地圈那一格（
 *       {@link BlissfulLandBiomes#RING_CONTINENTALNESS} 以上）够得着；圈外截到 {@link #CONTINENTS_HIGH}，因为 Tectonic 的
 *       大陆度会超过原版的上限 1，不截就有远处的格子落进平地圈群系。大陆度只喂群系，地形读 Tectonic 自己的大陆噪声。</li>
 *   <li>湖：湖泊噪声高于 {@link #LAKE_FROM} 的斑块，在低地（Tectonic 偏移估出的地面不高于 {@link #LOWLAND_TOP}）挖到
 *       {@link #LAKE_BED}，水来自原版含水层；离山心 {@link #MARGIN} 到 {@link #LAKE_CLEAR} 格渐出，圈内没有湖。</li>
 *   <li>圈内含水层的淹没度压到 {@link #DRY_FLOODEDNESS}：碗沿里仍有高过 y 64 的洼地，原版含水层会在那里放局部水位，压到
 *       −1 时不放。</li>
 *   <li>地板只是地板面以下 {@link #FLOOR_SLAB} 格厚的一层：填平地表的洼地和洞口，更深的洞穴照原地形保留。</li>
 *   <li>湖岸的顶面随湖系数变小抬高 {@link #SHORE_RISE} 格，斑块边缘比原地形高，不削出台阶。</li>
 *   <li>⚠ 含水层只在「初估地表 + 8」低于海平面时按海平面放水；初估地表读 {@code initial_density_without_jaggedness}
 *       高于 {@link #PRELIMINARY} 的最高处，所以湖处的初估密度再压低 {@link #LAKE_FLAG} 格，平地圈的初估地表钉在 y 64。
 *       </li>
 * </ul>
 */

public final class BlissfulLandTerrain {

    // 半格偏移：y 64 的密度为正、y 65 为负，草方块顶不受浮点误差左右。
    public static final double GROUND = 64.5;
    public static final int CENTRE = 8;
    public static final int FLAT = 384;
    public static final int RING = 512;
    public static final int MARGIN = 576;
    static final int LAKE_CLEAR = 704;
    static final double SLOPE = 1.0 / 16;
    static final double CEILING_RISE = 400.0;
    static final double FLOOR_DROP = 300.0;
    static final double FLOOR_SLAB = 24.0;
    static final double PRELIMINARY = 0.390625;
    static final double RING_CONTINENTS = 2.0;
    static final double CONTINENTS_LOW = -2.0;
    static final double CONTINENTS_HIGH = 1.0;
    static final double DRY_FLOODEDNESS = -1.0;
    static final double LAKE_BED = 55.0;
    static final double LAKE_FROM = 0.45;
    static final double LAKE_FULL = 0.6;
    static final double LAKE_FLAG = 10.0;
    static final double LOWLAND_TOP = 75.0;
    static final double LOWLAND_FADE = 80.0;
    static final double SHORE = 4.0;
    static final double SHORE_RISE = 16.0;
    // Tectonic 的深度是 (128 − y) / 128 加偏移，所以偏移为 v 处的地面约在 y 128 + 128 v。
    static final double OFFSET_ZERO = 128.0;
    static final double BLOCKS_PER_OFFSET = 128.0;
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
        DensityFunction none = DensityFunctions.zero();
        NoiseRouter shaped = new NoiseRouter(ported.barrierNoise(),
                new BlissfulLandShape(BlissfulLandShape.Kind.FLOODEDNESS, ported.fluidLevelFloodednessNoise(), none,
                        lakes),
                ported.fluidLevelSpreadNoise(), ported.lavaNoise(), ported.temperature(), ported.vegetation(),
                new BlissfulLandShape(BlissfulLandShape.Kind.CONTINENTS, ported.continents(), none, lakes),
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
