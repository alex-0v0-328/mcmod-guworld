package net.alex.guzhenrenworld.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

/**
 * 密度函数类型 {@code guworld:z_gradient}：沿 z 的原版 {@code y_clamped_gradient}。
 * <ul>
 *   <li>z 在 {@code from_z} 以外取 {@code from_value}，{@code to_z} 以外取 {@code to_value}，其间线性；原版无只依赖 x
 *       或 z 的函数，而北原温度随纬度变；由域地形副本的补丁（{@link Ports#registerDensityFunctions}）点名。</li>
 *   <li>{@link #MAP_CODEC} 为数据 codec，{@link GradientRegistration} 以 {@link #ID} 注册；{@link #CODEC} 包装同一实
 *       例，注册表凭它认类型；只读 z，可放进 {@code cache_2d}（{@link ColumnCaches}）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see GradientRegistration
 * @since 1.0.0
 */

public record ZGradient(int fromZ, int toZ, double fromValue, double toValue)
        implements DensityFunction.SimpleFunction {

    public static final String ID = "z_gradient";
    private static final int COORDINATE_LIMIT = 30_000_000;
    private static final double VALUE_LIMIT = 1_000_000.0;
    public static final MapCodec<ZGradient> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(-COORDINATE_LIMIT, COORDINATE_LIMIT).fieldOf("from_z").forGetter(ZGradient::fromZ),
            Codec.intRange(-COORDINATE_LIMIT, COORDINATE_LIMIT).fieldOf("to_z").forGetter(ZGradient::toZ),
            Codec.doubleRange(-VALUE_LIMIT, VALUE_LIMIT).fieldOf("from_value").forGetter(ZGradient::fromValue),
            Codec.doubleRange(-VALUE_LIMIT, VALUE_LIMIT).fieldOf("to_value").forGetter(ZGradient::toValue)
    ).apply(instance, ZGradient::new));
    static final KeyDispatchDataCodec<ZGradient> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return Mth.clampedMap(context.blockZ(), fromZ, toZ, fromValue, toValue);
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
