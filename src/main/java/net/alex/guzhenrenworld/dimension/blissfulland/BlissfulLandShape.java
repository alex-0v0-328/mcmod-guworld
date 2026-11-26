package net.alex.guzhenrenworld.dimension.blissfulland;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.jetbrains.annotations.NotNull;

/**
 * 福地路由的湖（{@code guworld:blissful_shape}）：一个类按 {@link Kind} 包住路由的最终密度与初估密度，把湖斑块里的地形
 * 压到只随水平位置变的湖顶面以下，数值见 {@link BlissfulLandTerrain}。
 * <ul>
 *   <li>{@link Kind#FINAL}、{@link Kind#INITIAL}：{@code min(原值, 顶面)}，顶面 = {@link BlissfulLandTerrain#SLOPE} ×
 *       (湖顶 − y)；湖外不改原值。初估密度的湖顶再低 {@link BlissfulLandTerrain#LAKE_FLAG} 格，且加上
 *       {@link BlissfulLandTerrain#PRELIMINARY}。</li>
 *   <li>为什么写成一个类：同样的式子用原版密度函数拼，二维辅助函数共用的子树在建噪声区块时会逐个展开映射， Tectonic 的
 *       偏移样条每区块要重复映射十来次，实测噪声填充从 34 慢到 84 ms/区块；这里偏移样条和原路由各只引用一次。</li>
 *   <li>列缓存：一区块 16×16 列各占一槽，存湖顶；原版按单元逐格填，相邻两格不在同一列，只记上一列命中不了。</li>
 *   <li>⚠ 列缓存只在上下文是噪声区块时用：噪声区块只求值它自己映射出的那份实例，那份只属于一个区块、一个线程；路由里的
 *       原实例会被多个线程单点求值，不缓存。</li>
 * </ul>
 */

public final class BlissfulLandShape implements DensityFunction {

    public static final String ID = "blissful_shape";
    private static final double LIMIT = 2048.0;
    private static final int COLUMN_BITS = 4;
    private static final int COLUMNS = 1 << (2 * COLUMN_BITS);
    private static final int COLUMN_MASK = (1 << COLUMN_BITS) - 1;
    public static final MapCodec<BlissfulLandShape> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Kind.CODEC.fieldOf("kind").forGetter(shape -> shape.kind),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(shape -> shape.argument),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("offset").forGetter(shape -> shape.offset),
            NoiseHolder.CODEC.fieldOf("lakes").forGetter(shape -> shape.lakes)
    ).apply(instance, BlissfulLandShape::new));
    static final KeyDispatchDataCodec<BlissfulLandShape> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final Kind kind;
    private final DensityFunction argument;
    private final DensityFunction offset;
    private final NoiseHolder lakes;
    private int[] cachedX;
    private int[] cachedZ;
    private double[] cachedCap;

    public BlissfulLandShape(Kind kind, DensityFunction argument, DensityFunction offset, NoiseHolder lakes) {
        this.kind = kind;
        this.argument = argument;
        this.offset = offset;
        this.lakes = lakes;
    }

    @Override
    public double compute(@NotNull FunctionContext context) {
        return shape(argument.compute(context), context);
    }

    @Override
    public void fillArray(double @NotNull [] array, @NotNull ContextProvider provider) {
        argument.fillArray(array, provider);
        for (int index = 0; index < array.length; index++) {
            array[index] = shape(array[index], provider.forIndex(index));
        }
    }

    @Override
    public @NotNull DensityFunction mapAll(@NotNull Visitor visitor) {
        return visitor.apply(new BlissfulLandShape(kind, argument.mapAll(visitor), offset.mapAll(visitor),
                visitor.visitNoise(lakes)));
    }

    @Override
    public double minValue() {
        return Math.min(argument.minValue(), -BlissfulLandTerrain.SLOPE * LIMIT);
    }

    @Override
    public double maxValue() {
        return argument.maxValue();
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private double shape(double value, FunctionContext context) {
        double capLevel = context instanceof NoiseChunk ? getCachedCap(context) : getCap(context);
        if (capLevel == Double.POSITIVE_INFINITY) return value;

        double threshold = kind == Kind.INITIAL ? BlissfulLandTerrain.PRELIMINARY : 0.0;
        return Math.min(value, BlissfulLandTerrain.SLOPE * (capLevel - context.blockY()) + threshold);
    }

    private double getCachedCap(FunctionContext context) {
        if (cachedX == null) {
            cachedX = new int[COLUMNS];
            cachedZ = new int[COLUMNS];
            cachedCap = new double[COLUMNS];
            Arrays.fill(cachedX, Integer.MIN_VALUE);
        }
        int x = context.blockX();
        int z = context.blockZ();
        int slot = (x & COLUMN_MASK) << COLUMN_BITS | (z & COLUMN_MASK);
        if (cachedX[slot] != x || cachedZ[slot] != z) {
            cachedCap[slot] = getCap(context);
            cachedX[slot] = x;
            cachedZ[slot] = z;
        }
        return cachedCap[slot];
    }

    private double getCap(FunctionContext context) {
        double blobs = getRamp(lakes.getValue(context.blockX(), 0, context.blockZ()), BlissfulLandTerrain.LAKE_FROM,
                BlissfulLandTerrain.LAKE_FULL);
        if (blobs <= 0) return Double.POSITIVE_INFINITY;

        double estimate = BlissfulLandTerrain.OFFSET_ZERO + BlissfulLandTerrain.BLOCKS_PER_OFFSET
                * offset.compute(context);
        double lake = blobs * getRamp(estimate, BlissfulLandTerrain.LOWLAND_FADE, BlissfulLandTerrain.LOWLAND_TOP);
        if (lake <= 0) return Double.POSITIVE_INFINITY;

        double shore = estimate + BlissfulLandTerrain.SHORE + (1 - lake) * BlissfulLandTerrain.SHORE_RISE;
        double target = shore + lake * (BlissfulLandTerrain.LAKE_BED - shore);
        return kind == Kind.INITIAL ? target - BlissfulLandTerrain.LAKE_FLAG * lake : target;
    }

    private static double getRamp(double value, double from, double to) {
        return Math.clamp((value - from) / (to - from), 0.0, 1.0);
    }

    public enum Kind implements StringRepresentable {
        FINAL("final"),
        INITIAL("initial");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
