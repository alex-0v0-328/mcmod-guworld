package net.alex.guzhenrenworld.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

/**
 * 密度函数类型 {@code guworld:x_gradient}：沿 x 的 {@link ZGradient}。
 * <ul>
 *   <li>x 在 {@code from_x} 以外取 {@code from_value}，{@code to_x} 以外取 {@code to_value}，其间线性；自西向东变化的域
 *       用它填钩子，如东边是开阔海面的东海。</li>
 *   <li>{@link #MAP_CODEC} 为数据 codec，{@link GradientRegistration} 以 {@link #ID} 注册；{@link #CODEC} 包装同一实
 *       例，注册表凭它认类型；只读 x，可放进 {@code cache_2d}（{@link ColumnCaches}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see GradientRegistration
 * @since 1.0.0
 */

public record XGradient(int fromX, int toX, double fromValue, double toValue)
        implements DensityFunction.SimpleFunction {

    public static final String ID = "x_gradient";
    private static final int COORDINATE_LIMIT = 30_000_000;
    private static final double VALUE_LIMIT = 1_000_000.0;
    public static final MapCodec<XGradient> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(-COORDINATE_LIMIT, COORDINATE_LIMIT).fieldOf("from_x").forGetter(XGradient::fromX),
            Codec.intRange(-COORDINATE_LIMIT, COORDINATE_LIMIT).fieldOf("to_x").forGetter(XGradient::toX),
            Codec.doubleRange(-VALUE_LIMIT, VALUE_LIMIT).fieldOf("from_value").forGetter(XGradient::fromValue),
            Codec.doubleRange(-VALUE_LIMIT, VALUE_LIMIT).fieldOf("to_value").forGetter(XGradient::toValue)
    ).apply(instance, XGradient::new));
    static final KeyDispatchDataCodec<XGradient> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

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
