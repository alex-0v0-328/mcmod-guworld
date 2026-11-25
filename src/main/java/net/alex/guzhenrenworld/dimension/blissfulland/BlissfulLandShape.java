package net.alex.guzhenrenworld.dimension.blissfulland;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.jetbrains.annotations.NotNull;

/**
 * 福地路由的整形函数（{@code guworld:blissful_shape}）：一个类按 {@link Kind} 包住路由的四个字段，平地圈、湖与平地圈群
 * 系的算术都在这里，数值见 {@link BlissfulLandTerrain}。
 * <ul>
 *   <li>{@link Kind#FINAL}、{@link Kind#INITIAL}：原密度夹在地板和顶面两个平坦场之间，
 *       {@code min(max(原值, 地板), 顶面)}；地板只是地板面以下 {@link BlissfulLandTerrain#FLOOR_SLAB} 格厚的一层，地板
 *       面和顶面只随水平位置变，湖并进顶面，初估密度的顶面在湖处再低 {@link BlissfulLandTerrain#LAKE_FLAG} 格，两个场都
 *       加上 {@link BlissfulLandTerrain#PRELIMINARY}。</li>
 *   <li>{@link Kind#CONTINENTS}：截到原版上限，平地圈内压到 {@link BlissfulLandTerrain#RING_CONTINENTS}；
 *       {@link Kind#FLOODEDNESS}：平地圈内压到 {@link BlissfulLandTerrain#DRY_FLOODEDNESS}。</li>
 *   <li>为什么写成一个类：同样的式子用原版密度函数拼，二维辅助函数共用的子树在建噪声区块时会逐个展开映射， Tectonic 的
 *       偏移样条每区块要重复映射十来次，实测噪声填充从 34 慢到 84 ms/区块；这里偏移样条和原路由各只引用一次。</li>
 *   <li>列缓存：一区块 16×16 列各占一槽，存地板面与顶面；原版按单元逐格填，相邻两格不在同一列，只记上一列命中不了。
 *       </li>
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
    private double[] cachedFloor;
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
        return switch (kind) {
            case FINAL, INITIAL -> Math.min(argument.minValue(), -BlissfulLandTerrain.SLOPE * LIMIT);
            case CONTINENTS -> BlissfulLandTerrain.CONTINENTS_LOW;
            case FLOODEDNESS -> Math.min(argument.minValue(), BlissfulLandTerrain.DRY_FLOODEDNESS);
        };
    }

    @Override
    public double maxValue() {
        return switch (kind) {
            case FINAL, INITIAL -> Math.max(argument.maxValue(), BlissfulLandTerrain.SLOPE * LIMIT + 1.0);
            case CONTINENTS -> BlissfulLandTerrain.RING_CONTINENTS;
            case FLOODEDNESS -> Math.max(argument.maxValue(), BlissfulLandTerrain.DRY_FLOODEDNESS);
        };
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private double shape(double value, FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();
        double dx = x - BlissfulLandTerrain.CENTRE;
        double dz = z - BlissfulLandTerrain.CENTRE;
        double squared = dx * dx + dz * dz;
        return switch (kind) {
            case FINAL, INITIAL -> {
                double floorLevel;
                double capLevel;
                if (context instanceof NoiseChunk) {
                    if (cachedX == null) {
                        cachedX = new int[COLUMNS];
                        cachedZ = new int[COLUMNS];
                        cachedFloor = new double[COLUMNS];
                        cachedCap = new double[COLUMNS];
                        Arrays.fill(cachedX, Integer.MIN_VALUE);
                    }
                    int slot = (x & COLUMN_MASK) << COLUMN_BITS | (z & COLUMN_MASK);
                    if (cachedX[slot] != x || cachedZ[slot] != z) {
                        cachedFloor[slot] = getFloor(squared);
                        cachedCap[slot] = getCap(x, z, squared, context);
                        cachedX[slot] = x;
                        cachedZ[slot] = z;
                    }
                    floorLevel = cachedFloor[slot];
                    capLevel = cachedCap[slot];
                } else {
                    floorLevel = getFloor(squared);
                    capLevel = getCap(x, z, squared, context);
                }
                double threshold = kind == Kind.INITIAL ? BlissfulLandTerrain.PRELIMINARY : 0.0;
                int y = context.blockY();
                double floor = BlissfulLandTerrain.SLOPE * Math.min(floorLevel - y,
                        y - floorLevel + BlissfulLandTerrain.FLOOR_SLAB) + threshold;
                double cap = BlissfulLandTerrain.SLOPE * (capLevel - y) + threshold;
                yield Math.min(Math.max(value, floor), cap);
            }
            case CONTINENTS -> {
                double inland = Math.clamp(value, BlissfulLandTerrain.CONTINENTS_LOW,
                        BlissfulLandTerrain.CONTINENTS_HIGH);
                double share = getShare(squared, BlissfulLandTerrain.RING, BlissfulLandTerrain.MARGIN);
                yield BlissfulLandTerrain.RING_CONTINENTS + share * (inland - BlissfulLandTerrain.RING_CONTINENTS);
            }
            case FLOODEDNESS -> {
                double share = getShare(squared, BlissfulLandTerrain.RING, BlissfulLandTerrain.MARGIN);
                yield BlissfulLandTerrain.DRY_FLOODEDNESS + share * (value - BlissfulLandTerrain.DRY_FLOODEDNESS);
            }
        };
    }

    private static double getFloor(double squared) {
        double drop = getShare(squared, BlissfulLandTerrain.RING, BlissfulLandTerrain.MARGIN);
        return BlissfulLandTerrain.GROUND - BlissfulLandTerrain.FLOOR_DROP * drop * drop;
    }

    private double getCap(int x, int z, double squared, FunctionContext context) {
        double rise = getShare(squared, BlissfulLandTerrain.FLAT, BlissfulLandTerrain.RING);
        double cap = BlissfulLandTerrain.GROUND + BlissfulLandTerrain.CEILING_RISE * rise * rise;
        double clear = getShare(squared, BlissfulLandTerrain.MARGIN, BlissfulLandTerrain.LAKE_CLEAR);
        if (clear > 0) {
            double blobs = getRamp(lakes.getValue(x, 0, z), BlissfulLandTerrain.LAKE_FROM,
                    BlissfulLandTerrain.LAKE_FULL);
            if (blobs > 0) {
                double estimate = BlissfulLandTerrain.OFFSET_ZERO
                        + BlissfulLandTerrain.BLOCKS_PER_OFFSET * offset.compute(context);
                double lake = blobs * clear * getRamp(estimate, BlissfulLandTerrain.LOWLAND_FADE,
                        BlissfulLandTerrain.LOWLAND_TOP);
                if (lake > 0) {
                    double shore = estimate + BlissfulLandTerrain.SHORE + (1 - lake) * BlissfulLandTerrain.SHORE_RISE;
                    double target = shore + lake * (BlissfulLandTerrain.LAKE_BED - shore);
                    if (kind == Kind.INITIAL) target -= BlissfulLandTerrain.LAKE_FLAG * lake;
                    cap = Math.min(cap, target);
                }
            }
        }
        return cap;
    }

    private static double getShare(double squared, int inner, int outer) {
        return getRamp(squared, (double) inner * inner, (double) outer * outer);
    }

    private static double getRamp(double value, double from, double to) {
        return Math.clamp((value - from) / (to - from), 0.0, 1.0);
    }

    public enum Kind implements StringRepresentable {
        FINAL("final"),
        INITIAL("initial"),
        CONTINENTS("continents"),
        FLOODEDNESS("floodedness");

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
