package net.alex.guzhenrenworld.dimension.blissfulland;

import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.XGradient;
import net.alex.guzhenrenworld.biome.ZGradient;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * 福地草原的噪声路由：山心周围一圈平地，往外是缓丘，噪声画出河与湖，地下是原版的噪声洞穴。
 * <ul>
 *   <li>地面高度 H 是只随水平位置变的函数，地形密度取 {@code (H − y) / 8}，所以地表就是 H，竖直插值不失真。H 由三张本福
 *       地专用噪声拼成：丘陵在 {@link #LAND} ± {@link #HILLS} 间起伏；河取河流噪声的零值等值线，{@code |R|} 小于
 *       {@link #RIVER_CORE} 处整段降到 {@link #RIVER_BED}，到 {@link #RIVER_EDGE} 渐变回地面；湖取湖泊噪声高于
 *       {@link #LAKE_FROM} 的斑块，到 {@link #LAKE_FULL} 降到 {@link #LAKE_BED}。河床、湖床低于海平面，水来自原版含水
 *       层。</li>
 *   <li>平台：离山心（原点区块中心 (8, 8)）{@link #PLATFORM} 格内 H 固定为 {@link #GROUND}（草方块顶 y 64），到
 *       {@link #BLEND} 格按距离平方线性过渡到草原；平台里不开洞口、不走面条洞，荡魂山和主模组的入口落点都立在平地上。
 *       </li>
 *   <li>⚠ 含水层只在「初估地表 + 8」低于海平面的位置按海平面放水；初估地表读
 *       {@code initial_density_without_jaggedness}，所以它用的 H 在河湖处再压低 {@link #WATER_FLAG} 格，否则地面 70 格
 *       的草原里刻出的河道会被判成干洞。</li>
 *   <li>洞穴沿用原版主世界路由的写法（奶酪、意面、面条、入口、柱子，引用 {@code minecraft:overworld/caves/*}），坡面奶
 *       酪参数换成本地形密度；上下滑坡与后处理同原版；矿脉同原版（y −60 至 50）。</li>
 *   <li>群系源固定为一个群系，温度、湿度等气候参数全为 0。</li>
 * </ul>
 */

public final class BlissfulLandTerrain {

    // 半格偏移：y 64 的密度 +1/16、y 65 为 −1/16，草方块顶不受浮点误差左右。
    public static final double GROUND = 64.5;
    public static final int CENTRE = 8;
    public static final int PLATFORM = 192;
    public static final int BLEND = 256;
    private static final double LAND = 70.0;
    private static final double HILLS = 10.0;
    private static final double RIVER_BED = 57.0;
    private static final double RIVER_CORE = 0.015;
    private static final double RIVER_EDGE = 0.04;
    private static final double LAKE_BED = 55.0;
    private static final double LAKE_FROM = 0.45;
    private static final double LAKE_FULL = 0.6;
    private static final double WATER_FLAG = 10.0;
    private static final double BLOCKS_PER_UNIT = 8.0;
    private static final int GRADIENT_REACH = 1024;
    private static final double SHALLOW = 1.5625;
    private static final double FAR = 1_000_000.0;
    private static final double SUPPRESSED = 1000.0;
    private static final int VEIN_MIN_Y = -60;
    private static final int VEIN_MAX_Y = 50;

    private BlissfulLandTerrain() {}

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context, String name) {
        context.register(getNoiseKey(name, "hills"), new NormalNoise.NoiseParameters(-8, 1.0, 1.0, 0.5));
        context.register(getNoiseKey(name, "rivers"), new NormalNoise.NoiseParameters(-9, 1.0, 0.5));
        context.register(getNoiseKey(name, "lakes"), new NormalNoise.NoiseParameters(-7, 1.0));
    }

    public static NoiseRouter getRouter(HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises, String name, int minY, int height) {
        DensityFunction hills = DensityFunctions.noise(noises.getOrThrow(getNoiseKey(name, "hills")), 1.0, 0.0);
        DensityFunction rivers = DensityFunctions.noise(noises.getOrThrow(getNoiseKey(name, "rivers")), 1.0, 0.0);
        DensityFunction lakes = DensityFunctions.noise(noises.getOrThrow(getNoiseKey(name, "lakes")), 1.0, 0.0);
        DensityFunction blend = DensityFunctions.cache2d(getBlend());
        DensityFunction river = DensityFunctions.add(DensityFunctions.constant(RIVER_EDGE / (RIVER_EDGE - RIVER_CORE)),
                DensityFunctions.mul(DensityFunctions.constant(-1.0 / (RIVER_EDGE - RIVER_CORE)), rivers.abs()))
                .clamp(0.0, 1.0);
        DensityFunction lake = DensityFunctions.add(DensityFunctions.constant(-LAKE_FROM / (LAKE_FULL - LAKE_FROM)),
                DensityFunctions.mul(DensityFunctions.constant(1.0 / (LAKE_FULL - LAKE_FROM)), lakes)).clamp(0.0, 1.0);
        DensityFunction land = DensityFunctions.add(DensityFunctions.constant(LAND),
                DensityFunctions.mul(DensityFunctions.constant(HILLS), hills));
        DensityFunction wet = DensityFunctions.lerp(lake,
                DensityFunctions.lerp(river, land, DensityFunctions.constant(RIVER_BED)),
                DensityFunctions.constant(LAKE_BED));
        DensityFunction ground = DensityFunctions.cache2d(DensityFunctions.lerp(blend,
                DensityFunctions.constant(GROUND), wet));
        DensityFunction wetness = DensityFunctions.mul(blend, DensityFunctions.max(river, lake));
        DensityFunction flagged = DensityFunctions.cache2d(DensityFunctions.add(ground,
                DensityFunctions.mul(DensityFunctions.constant(-WATER_FLAG), wetness)));
        DensityFunction platform = DensityFunctions.add(DensityFunctions.constant(1.0),
                DensityFunctions.mul(DensityFunctions.constant(-1.0), blend));
        DensityFunction keep = DensityFunctions.mul(DensityFunctions.constant(SUPPRESSED), platform);
        DensityFunction terrain = getTerrain(ground, minY, height);
        DensityFunction entrances = getFunction(functions, "overworld/caves/entrances");
        DensityFunction shallow = DensityFunctions.min(terrain, DensityFunctions.add(
                DensityFunctions.mul(DensityFunctions.constant(5.0), entrances), keep));
        DensityFunction caves = DensityFunctions.rangeChoice(terrain, -FAR, SHALLOW, shallow,
                getUnderground(functions, noises, terrain));
        DensityFunction finalDensity = DensityFunctions.min(postProcess(slide(caves, minY, height)),
                DensityFunctions.add(getFunction(functions, "overworld/caves/noodle"), keep));
        DensityFunction y = getFunction(functions, "y");
        DensityFunction veinToggle = limited(y, DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEININESS),
                1.5, 1.5));
        DensityFunction veinRidged = DensityFunctions.add(DensityFunctions.constant(-0.08F), DensityFunctions.max(
                limited(y, DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEIN_A), 4.0, 4.0)).abs(),
                limited(y, DensityFunctions.noise(noises.getOrThrow(Noises.ORE_VEIN_B), 4.0, 4.0)).abs()));
        return new NoiseRouter(
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_BARRIER), 0.5),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143),
                DensityFunctions.noise(noises.getOrThrow(Noises.AQUIFER_LAVA)),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                slide(getTerrain(flagged, minY, height).clamp(-64.0, 64.0), minY, height),
                finalDensity,
                veinToggle,
                veinRidged,
                DensityFunctions.noise(noises.getOrThrow(Noises.ORE_GAP)));
    }

    private static DensityFunction getBlend() {
        DensityFunction dx = new XGradient(CENTRE - GRADIENT_REACH, CENTRE + GRADIENT_REACH, -GRADIENT_REACH,
                GRADIENT_REACH);
        DensityFunction dz = new ZGradient(CENTRE - GRADIENT_REACH, CENTRE + GRADIENT_REACH, -GRADIENT_REACH,
                GRADIENT_REACH);
        double inner = (double) PLATFORM * PLATFORM;
        double span = (double) BLEND * BLEND - inner;
        return DensityFunctions.add(DensityFunctions.constant(-inner / span), DensityFunctions.mul(
                DensityFunctions.constant(1.0 / span), DensityFunctions.add(dx.square(), dz.square()))).clamp(0.0, 1.0);
    }

    private static DensityFunction getTerrain(DensityFunction ground, int minY, int height) {
        int maxY = minY + height;
        return DensityFunctions.add(DensityFunctions.mul(DensityFunctions.constant(1.0 / BLOCKS_PER_UNIT), ground),
                DensityFunctions.yClampedGradient(minY, maxY, -minY / BLOCKS_PER_UNIT, -maxY / BLOCKS_PER_UNIT));
    }

    //region Caves [洞穴] -- the vanilla overworld recipe for caves, slides and veins, fed this terrain
    private static DensityFunction getUnderground(HolderGetter<DensityFunction> functions,
            HolderGetter<NormalNoise.NoiseParameters> noises, DensityFunction terrain) {
        DensityFunction layer = DensityFunctions.mul(DensityFunctions.constant(4.0),
                DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_LAYER), 8.0).square());
        DensityFunction cheese = DensityFunctions.add(
                DensityFunctions.add(DensityFunctions.constant(0.27),
                        DensityFunctions.noise(noises.getOrThrow(Noises.CAVE_CHEESE), 0.6666666666666666))
                        .clamp(-1.0, 1.0),
                DensityFunctions.add(DensityFunctions.constant(1.5),
                        DensityFunctions.mul(DensityFunctions.constant(-0.64), terrain)).clamp(0.0, 0.5));
        DensityFunction caves = DensityFunctions.min(
                DensityFunctions.min(DensityFunctions.add(layer, cheese),
                        getFunction(functions, "overworld/caves/entrances")),
                DensityFunctions.add(getFunction(functions, "overworld/caves/spaghetti_2d"),
                        getFunction(functions, "overworld/caves/spaghetti_roughness_function")));
        DensityFunction pillars = getFunction(functions, "overworld/caves/pillars");
        return DensityFunctions.max(caves, DensityFunctions.rangeChoice(pillars, -FAR, 0.03,
                DensityFunctions.constant(-FAR), pillars));
    }

    private static DensityFunction slide(DensityFunction input, int minY, int height) {
        DensityFunction top = DensityFunctions.yClampedGradient(minY + height - 80, minY + height - 64, 1.0, 0.0);
        DensityFunction bottom = DensityFunctions.yClampedGradient(minY, minY + 24, 0.0, 1.0);
        return DensityFunctions.lerp(bottom, 0.1171875, DensityFunctions.lerp(top, -0.078125, input));
    }

    private static DensityFunction postProcess(DensityFunction input) {
        return DensityFunctions.mul(DensityFunctions.interpolated(DensityFunctions.blendDensity(input)),
                DensityFunctions.constant(0.64)).squeeze();
    }

    private static DensityFunction limited(DensityFunction y, DensityFunction inRange) {
        return DensityFunctions.interpolated(DensityFunctions.rangeChoice(y, VEIN_MIN_Y, VEIN_MAX_Y + 1, inRange,
                DensityFunctions.zero()));
    }
    //endregion

    private static ResourceKey<NormalNoise.NoiseParameters> getNoiseKey(String name, String part) {
        return GuWorld.key(Registries.NOISE, "terrain/" + name + "/" + part);
    }

    private static DensityFunction getFunction(HolderGetter<DensityFunction> functions, String path) {
        return new DensityFunctions.HolderHolder(functions.getOrThrow(ResourceKey.create(Registries.DENSITY_FUNCTION,
                ResourceLocation.withDefaultNamespace(path))));
    }
}
