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
 * 水系：Gu World 自己的溪流，每个有限区域按地形规划一次。
 *
 * <ul>
 *   <li>依据 Davis 板块式河流生成论文（arXiv 2610.00301；参考代码 CC BY 4.0，
 *       github.com/Davismk3/plate-local-river-generation），8192 格区域整个为一板块。</li>
 *   <li>存规划、索引、刻河共用的数值：{@link WaterwaysPlanner} 建 {@link WaterwaysPlan}，{@link WaterwaysIndex}
 *       转成线段，{@link WaterwaysDensity} 包住区域最终密度、经 {@link WaterwaysCarver} 刻出；{@link #PARAMETERS}
 *       是存档索引依赖的数值，变则重建（{@link WaterwaysData}）。</li>
 *   <li>在 {@link #CELL} 格网格上取样，样本偏移 ≤ {@link #JITTER} 格免得河道顺网格；汇流达 {@link #MIN_CATCHMENT} 格成
 *       河，全宽随汇流平方根从 {@link #WIDTH_SCALE} 格增至封顶 {@link #MAX_WIDTH} 格。</li>
 *   <li>深 ≤ {@link #LAKE_DEPTH} 格的封闭洼地在规划中成湖，更深的溪流终于洼底汇点；西漠 {@link Region#dry}：照样刻，
 *       仅最宽的有水，余为干沟。</li>
 *   <li>水面低于地面 ≥ {@link #INCISION} 格（海平面下按海平面），往下游不升（{@link WaterwaysProfile}：每段沿线及两侧
 *       {@link #KNOTS} + 1 个节点读地面，跳过深于 {@link #MAX_DIP} 格的凹陷）；规划中的浅湖刻穿。</li>
 *   <li>河底上整宽至少 1 格水，中线水深从 {@link #MIN_DEPTH} 格增至 {@link #MAX_DEPTH} 格（宽 {@link #MAX_WIDTH} 格）；
 *       岸带宽 {@code max(}{@link #BANK_MIN}{@code , }{@link #BANK_FACTOR}{@code  × width)} 格，升到扫描地面上
 *       {@link #BANK_MARGIN} 格，盖住常见 1–2 格的扫描误差。</li>
 *   <li>河道沿各轴偏离中线 ≤ {@link #MEANDER} 格，读自 {@link #MEANDER_NOISE}，其实例也标明所填维度的随机状态。</li>
 *   <li>{@link WaterwaysFeature} 放水并铺区域的 {@link Bed}；干旱区域仅宽 ≥ {@link #DRY_WIDTH} 格的河道有水。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class Waterways {

    public static final int CELL = 16;
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
    public static final ResourceKey<NormalNoise.NoiseParameters> MEANDER_NOISE =
            GuWorld.key(Registries.NOISE, "waterways/meander");
    private static final int MEANDER_OCTAVE = -6;
    private static final double MEANDER_OFFSET = 4096.0;
    private static final double CLAY = 0.4;
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
        SAND("sand");

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
            };
        }
    }

    public record Region(String name, double borderSize, boolean dry, Bed bed) {

        public int cells() {
            return (int) (borderSize / CELL);
        }

        public int origin() {
            return (int) (-borderSize / 2);
        }
    }
}
