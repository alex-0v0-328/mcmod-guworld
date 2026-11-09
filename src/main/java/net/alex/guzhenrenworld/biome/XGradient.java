package net.alex.guzhenrenworld.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

/**
 * The density function type {@code guworld:x_gradient}, {@link ZGradient} along x: from {@code from_value} at x
 * {@code from_x} and beyond to {@code to_value} at x {@code to_x} and beyond, linear between. A region that changes
 * from west to east, such as the Eastern Sea [东海] with its open sea toward the east, fills a hook with it.
 *
 * <p>{@link #MAP_CODEC} is the type's data codec, which {@link GradientRegistration} registers under {@link #ID},
 * and {@link #CODEC} wraps that very instance: the registry finds the type by it.
 * The function reads only x, so it may sit inside a {@code cache_2d} ({@link ColumnCaches}).
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
