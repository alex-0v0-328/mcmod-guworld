package net.alex.guzhenrenworld.waterways;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 在域最终密度上刻入水系河槽的包裹型密度函数（{@code guworld:waterways}）。
 * <ul>
 *   <li>各域噪声生成设置通过 {@link #getCarved} 将本函数包裹在移植噪声路由的 {@code finalDensity} 外层，编码域名称、边
 *       界尺寸、干旱标志、河床类型与蜿蜒噪声引用；类型 Codec 由 {@link WaterwaysRegistration} 注册。</li>
 *   <li>仅对原本为实心（{@code density > 0}）的坐标生效，取原密度与 {@code SLOPE × (surface - y)}（
 *       {@link WaterwaysCarver#getSurface}）的较小值：只向下切削河槽与岸坡而不填充空气，从而完整保留下方的天然洞穴；在
 *       {@link NoiseChunk} 内求值时会将当前区块已挂载列缓存的内部 {@link #argument} 传给雕刻器以加速岸带地面采样。</li>
 *   <li>通过 {@link WaterwaysRuntime} 以实例化后的蜿蜒噪声对象（{@link NormalNoise}）为键查找当前维度的
 *       {@link WaterwaysCarver}；未挂载雕刻器的独立 {@code RandomState}（如纯单元测试）直接原样返回底层地形密度。</li>
 *   <li>每个区块通过 {@link #mapAll} 克隆出独立的密度函数副本，并在首次遇到实心方块时解析一次雕刻器缓存到不可变包装记录
 *       {@link Lookup} 中，利用单引用写入实现无锁线程安全。</li>
 * </ul>
 */

public final class WaterwaysDensity implements DensityFunction {

    public static final String ID = "waterways";
    private static final double SLOPE = 0.1;
    private static final double LOWEST = -SLOPE * 4096;
    public static final MapCodec<WaterwaysDensity> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("region").forGetter(density -> density.region.name()),
            Codec.doubleRange(Waterways.CELL, 60_000_000.0).fieldOf("size")
                    .forGetter(density -> density.region.borderSize()),
            Codec.BOOL.fieldOf("dry").forGetter(density -> density.region.dry()),
            Waterways.Bed.CODEC.fieldOf("bed").forGetter(density -> density.region.bed()),
            Codec.doubleRange(0.0, 60_000_000.0).optionalFieldOf("keep_out", 0.0)
                    .forGetter(density -> density.region.keepOut()),
            Codec.BOOL.optionalFieldOf("walled", false).forGetter(density -> density.region.walled()),
            Codec.intRange(Waterways.CELL / 2, Waterways.CELL * 4).optionalFieldOf("cell", Waterways.CELL)
                    .forGetter(density -> density.region.cell()),
            NoiseHolder.CODEC.fieldOf("meander").forGetter(density -> density.meander),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(WaterwaysDensity::argument)
    ).apply(instance, (name, size, dry, bed, keepOut, walled, cell, meander, argument) -> new WaterwaysDensity(
            new Waterways.Region(name, size, dry, bed, keepOut, walled, cell), meander, argument)));
    static final KeyDispatchDataCodec<WaterwaysDensity> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final Waterways.Region region;
    private final NoiseHolder meander;
    private final DensityFunction argument;
    private @Nullable Lookup lookup;

    private WaterwaysDensity(Waterways.Region region, NoiseHolder meander, DensityFunction argument) {
        this.region = region;
        this.meander = meander;
        this.argument = argument;
    }

    public static NoiseRouter getCarved(BootstrapContext<?> context, NoiseRouter router, Waterways.Region region) {
        Holder<NormalNoise.NoiseParameters> meander = context.lookup(Registries.NOISE)
                .getOrThrow(Waterways.MEANDER_NOISE);
        return new NoiseRouter(router.barrierNoise(), router.fluidLevelFloodednessNoise(),
                router.fluidLevelSpreadNoise(), router.lavaNoise(), router.temperature(), router.vegetation(),
                router.continents(), router.erosion(), router.depth(), router.ridges(),
                router.initialDensityWithoutJaggedness(),
                new WaterwaysDensity(region, new NoiseHolder(meander), router.finalDensity()),
                router.veinToggle(), router.veinRidged(), router.veinGap());
    }

    public static Optional<WaterwaysDensity> find(NoiseRouter router) {
        DensityFunction function = router.finalDensity();
        while (function instanceof DensityFunctions.HolderHolder(var held)) {
            function = held.value();
        }
        return function instanceof WaterwaysDensity waterways ? Optional.of(waterways) : Optional.empty();
    }

    static DensityFunction getUncarved(NoiseRouter router) {
        return find(router).map(WaterwaysDensity::argument).orElse(router.finalDensity());
    }

    public Waterways.Region region() {
        return region;
    }

    public DensityFunction argument() {
        return argument;
    }

    @Override
    public double compute(@NotNull FunctionContext context) {
        double density = argument.compute(context);
        return density > 0 ? carve(density, context) : density;
    }

    @Override
    public void fillArray(double @NotNull [] array, @NotNull ContextProvider provider) {
        argument.fillArray(array, provider);
        for (int index = 0; index < array.length; index++) {
            if (array[index] > 0) array[index] = carve(array[index], provider.forIndex(index));
        }
    }

    @Override
    public @NotNull DensityFunction mapAll(@NotNull Visitor visitor) {
        return visitor.apply(new WaterwaysDensity(region, visitor.visitNoise(meander), argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return Math.min(argument.minValue(), LOWEST);
    }

    @Override
    public double maxValue() {
        return argument.maxValue();
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private double carve(double density, FunctionContext context) {
        Lookup resolved = lookup;
        if (resolved == null) {
            resolved = new Lookup(WaterwaysRuntime.find(meander.noise()));
            lookup = resolved;
        }
        WaterwaysCarver carver = resolved.carver();
        if (carver == null) return density;
        double surface = carver.getSurface(context.blockX(), context.blockZ(),
                context instanceof NoiseChunk ? argument : null);
        return surface == Double.POSITIVE_INFINITY ? density : Math.min(density, SLOPE * (surface - context.blockY()));
    }

    private record Lookup(@Nullable WaterwaysCarver carver) {}
}
