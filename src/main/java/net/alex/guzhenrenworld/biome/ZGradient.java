package net.alex.guzhenrenworld.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * The density function type {@code guworld:z_gradient}, vanilla's {@code y_clamped_gradient} along z: from
 * {@code from_value} at z {@code from_z} and beyond to {@code to_value} at z {@code to_z} and beyond, linear between.
 * Vanilla has no function of x or z alone, and a region that changes with latitude, such as the Northern Plains
 * [北原] with its temperature, needs one; a region names it in the JSON of its terrain copy
 * ({@link Ports#registerDensityFunctions}'s patch).
 *
 * <p>{@link #MAP_CODEC} is the type's data codec, which {@link ZGradientRegistration} registers under {@link #ID}, or
 * the codec of {@link #CODEC} where the registry holds codecs: a registry finds the type by that very instance.
 * The function reads only z, so it may sit inside a {@code cache_2d} ({@link ColumnCaches}).
 *
 * @author Alex
 * @version 1.0.0
 * @see ZGradientRegistration
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
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
