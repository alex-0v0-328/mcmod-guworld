package net.alex.guzhenrenworld.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

/**
 * 沿 X 轴方向做线性插值与截断的密度函数类型（{@code guworld:x_gradient}），与 {@link ZGradient} 对称。
 * <ul>
 *   <li>当方块 X 坐标超出 {@code [from_x, to_x]} 区间时分别钳制为 {@code from_value} 或 {@code to_value}，区间内按线性
 *       比例插值；供地貌沿东西方向渐变的域（如东侧过渡为开阔海面的东海）填充地形钩子。</li>
 *   <li>{@link #MAP_CODEC} 由 {@link GradientRegistration} 按 {@link #ID} 注册，{@link #CODEC} 供注册表分派类型；由于求
 *       值仅依赖水平 X 坐标，可安全包裹在 {@code cache_2d} 列缓存中（{@link ColumnCaches}）。</li>
 *   <li>两端坐标必须不同：相等时插值分母为 0，整列会求出 NaN，编解码直接报错。</li>
 * </ul>
 */

public record XGradient(int fromX, int toX, double fromValue, double toValue)
        implements DensityFunction.SimpleFunction {

    public static final String ID = "x_gradient";
    private static final int COORDINATE_LIMIT = 30_000_000;
    private static final double VALUE_LIMIT = 1_000_000.0;
    private static final MapCodec<XGradient> FIELDS = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(-COORDINATE_LIMIT, COORDINATE_LIMIT).fieldOf("from_x").forGetter(XGradient::fromX),
            Codec.intRange(-COORDINATE_LIMIT, COORDINATE_LIMIT).fieldOf("to_x").forGetter(XGradient::toX),
            Codec.doubleRange(-VALUE_LIMIT, VALUE_LIMIT).fieldOf("from_value").forGetter(XGradient::fromValue),
            Codec.doubleRange(-VALUE_LIMIT, VALUE_LIMIT).fieldOf("to_value").forGetter(XGradient::toValue)
    ).apply(instance, XGradient::new));
    public static final MapCodec<XGradient> MAP_CODEC = FIELDS.validate(XGradient::validate);
    static final KeyDispatchDataCodec<XGradient> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static DataResult<XGradient> validate(XGradient gradient) {
        return gradient.fromX() == gradient.toX()
                ? DataResult.error(() -> "x_gradient needs from_x != to_x") : DataResult.success(gradient);
    }

    @Override
    public double compute(FunctionContext context) {
        return Mth.clampedMap(context.blockX(), fromX, toX, fromValue, toValue);
    }

    @Override
    public double minValue() {
        return Math.min(fromValue, toValue);
    }

    @Override
    public double maxValue() {
        return Math.max(fromValue, toValue);
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
