package net.alex.guzhenrenworld.waterways;

import com.mojang.serialization.Codec;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.NotNull;

/**
 * 四域水系生成系统的全局参数、河床材质与区域配置定义。
 * <ul>
 *   <li>算法基于 Davis 板块局部河流生成论文（arXiv:2610.00301，参考实现采用 CC BY 4.0 协议），将 8192×8192 格的有限域
 *       整体视作单个水文板块一次性规划水网。</li>
 *   <li>全流程串联参数：{@link WaterwaysPlanner} 在带随机抖动（≤ {@link #JITTER} 格）的 {@link #CELL} 格网格上构建水文
 *       规划{@link WaterwaysPlan}，{@link WaterwaysIndex} 将其转换为分桶线段索引，{@link WaterwaysDensity} 包裹域最终密
 *       度并委托{@link WaterwaysCarver} 在地形上刻出河槽，最后由 {@link WaterwaysFeature} 填充水体与河床方块
 *       （{@link Bed}）；所有影响规划结果的常量拼入版本串 {@link #PARAMETERS}，供 {@link WaterwaysData} 校验存档索引是
 *       否失效。</li>
 *   <li>水文几何：汇流面积达到 {@link #MIN_CATCHMENT} 格起形成溪流，河宽随汇流面积平方根从 {@link #WIDTH_SCALE} 增长至
 *       上限{@link #MAX_WIDTH}；深度不超过 {@link #LAKE_DEPTH} 格的封闭洼地填平为浅湖并刻穿，更深的洼地则在谷底设为汇点
 *       （{@code SINK}）作为内流河终点。</li>
 *   <li>剖面与横截面：水面至少低于两岸地面 {@link #INCISION} 格且向流向单调不升（{@link WaterwaysProfile} 在每段取
 *       {@link #KNOTS} + 1 个节点采样并跳过深于 {@link #MAX_DIP} 格的窄裂缝）；河底保证全宽至少 1 格水深、中线水深在
 *       {@link #MIN_DEPTH} 至 {@link #MAX_DEPTH} 间插值，两岸平滑过渡带宽 {@link #getBank} 并抬升至扫描地面上方
 *       {@link #BANK_MARGIN} 格以吸收离散扫描误差。</li>
 *   <li>河道中心线受摆动噪声 {@link #MEANDER_NOISE} 驱动，在 X/Z 轴产生至多 {@link #MEANDER} 格的蜿蜒偏移；同时该噪声实
 *       例在运行时兼作 {@link WaterwaysRuntime} 识别所属维度 {@code RandomState} 的唯一身份键。</li>
 *   <li>干旱域（{@link Region#dry()} 为 {@code true}，如西漠）同样刻出全套河槽并铺设河床，但仅在河宽达到
 *       {@link #DRY_WIDTH} 格的主干河道中注水，其余支流保持为干涸河谷。</li>
 *   <li>网格边长按区域给（{@link Region#cell()}，默认 {@link #CELL}）：抖动随之放大，成河的汇流格数按面积换算
 *       （{@link Region#minCatchment()}），使网格放粗后溪流仍从同样大的集水面积起头，每段的水面节点数不变。</li>
 *   <li>禁区与墙（狐仙福地用）：{@link Region#keepOut()} 大于 0 时，离原点不到这么多格的网格是墙；
 *       {@link Region#walled()} 时最外一圈也是墙而非出口。墙不进填洼、不收汇流，溪流绕开平地圈，也不流向疆域断面；
 *       {@link #KEEP_OUT_REACH} 为河道半宽、岸带与两轴摆动之和再留 8 格，禁区内更深处碰不到任何线段（随河宽或岸带联
 *       动）。</li>
 *   <li>数据包里的区域边长与禁区半径上限为 {@link #MAX_BORDER_SIZE}：规划按网格数平方开数组，边长再大会让 int 溢出、
 *       内存失控（现有最大的狐仙福地 19,584 格）。</li>
 * </ul>
 */

public final class Waterways {

    public static final int CELL = 16;
    public static final double MAX_BORDER_SIZE = 65_536.0;
    public static final int JITTER = 5;
    public static final int MIN_CATCHMENT = 32;
    public static final double WIDTH_SCALE = 1.5;
    public static final double MAX_WIDTH = 7.0;
    public static final double LAKE_DEPTH = 4.0;
    public static final double INCISION = 1.0;
    public static final double MIN_DEPTH = 1.0;
    public static final double MAX_DEPTH = 3.0;
    public static final double BANK_MIN = 5.0;
    public static final double BANK_FACTOR = 2.0;
    public static final double BANK_MARGIN = 3.0;
    public static final double MEANDER = 2.5;
    public static final int KNOTS = 3;
    public static final double MAX_DIP = 4.0;
    public static final double DRY_WIDTH = 5.0;
    public static final double KEEP_OUT_REACH = MAX_WIDTH / 2 + Math.max(BANK_MIN, BANK_FACTOR * MAX_WIDTH)
            + MEANDER * Math.sqrt(2) + 8.0;
    public static final ResourceKey<NormalNoise.NoiseParameters> MEANDER_NOISE =
            GuWorld.key(Registries.NOISE, "waterways/meander");
    private static final int MEANDER_OCTAVE = -6;
    private static final double MEANDER_OFFSET = 4096.0;
    private static final double CLAY = 0.4;
    private static final double SANDY = 0.2;
    public static final String PARAMETERS = String.join("/", "v3", String.valueOf(CELL), String.valueOf(JITTER),
            String.valueOf(MIN_CATCHMENT), String.valueOf(WIDTH_SCALE), String.valueOf(MAX_WIDTH),
            String.valueOf(LAKE_DEPTH), String.valueOf(INCISION), String.valueOf(MEANDER),
            String.valueOf(MEANDER_OCTAVE), String.valueOf(KNOTS), String.valueOf(MAX_DIP));

    private Waterways() {}

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        context.register(MEANDER_NOISE, new NormalNoise.NoiseParameters(MEANDER_OCTAVE, 1.0, 1.0));
    }

    public static double getDepth(double width) {
        double share = Math.clamp((width - WIDTH_SCALE) / (MAX_WIDTH - WIDTH_SCALE), 0.0, 1.0);
        return MIN_DEPTH + (MAX_DEPTH - MIN_DEPTH) * share;
    }

    public static double getBank(double width) {
        return Math.max(BANK_MIN, BANK_FACTOR * width);
    }

    public static double getMeanderX(NormalNoise meander, double x, double z) {
        return MEANDER * Math.clamp(meander.getValue(x, 0, z), -1.0, 1.0);
    }

    public static double getMeanderZ(NormalNoise meander, double x, double z) {
        return MEANDER * Math.clamp(meander.getValue(x, MEANDER_OFFSET, z), -1.0, 1.0);
    }

    public enum Bed implements StringRepresentable {
        GRAVEL_CLAY("gravel_clay"),
        GRAVEL("gravel"),
        SAND("sand"),
        GRAVEL_CLAY_SAND("gravel_clay_sand");

        public static final Codec<Bed> CODEC = StringRepresentable.fromEnum(Bed::values);

        private final String name;

        Bed(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public BlockState getBlock(NormalNoise meander, int x, int z) {
            return switch (this) {
                case GRAVEL_CLAY -> meander.getValue(x, 2 * MEANDER_OFFSET, z) > CLAY
                        ? Blocks.CLAY.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
                case GRAVEL -> Blocks.GRAVEL.defaultBlockState();
                case SAND -> Blocks.SAND.defaultBlockState();
                case GRAVEL_CLAY_SAND -> meander.getValue(x, 2 * MEANDER_OFFSET, z) > CLAY
                        ? Blocks.CLAY.defaultBlockState() : meander.getValue(x, 3 * MEANDER_OFFSET, z) > SANDY
                        ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            };
        }
    }

    public record Region(String name, double borderSize, boolean dry, Bed bed, double keepOut, boolean walled,
            int cell) {

        public Region(String name, double borderSize, boolean dry, Bed bed) {
            this(name, borderSize, dry, bed, 0.0, false, CELL);
        }

        public int cells() {
            return (int) (borderSize / cell);
        }

        public int jitter() {
            return JITTER * cell / CELL;
        }

        public int minCatchment() {
            return Math.max(1, MIN_CATCHMENT * CELL * CELL / (cell * cell));
        }

        public int origin() {
            return (int) (-borderSize / 2);
        }
    }
}
