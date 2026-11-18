package net.alex.guzhenrenworld.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

/**
 * 沿 Z 轴方向做线性插值与截断的密度函数类型（{@code guworld:z_gradient}），相当于原版 {@code y_clamped_gradient} 的水平
 * Z 轴版本。
 * <ul>
 *   <li>当方块 Z 坐标超出 {@code [from_z, to_z]} 区间时分别钳制为 {@code from_value} 或 {@code to_value}，区间内按线性
 *       比例插值；用于弥补原版缺少单水平轴渐变函数的空白，供温度随纬度变化的域（如北原）在地形补丁（
 *       {@link Ports#registerDensityFunctions}）中引用。</li>
 *   <li>{@link #MAP_CODEC} 由 {@link GradientRegistration} 按 {@link #ID} 注册，{@link #CODEC} 供注册表分派类型；由于求
 *       值仅依赖水平 Z 坐标，可安全包裹在 {@code cache_2d} 列缓存中（{@link ColumnCaches}）。</li>
 * </ul>
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
