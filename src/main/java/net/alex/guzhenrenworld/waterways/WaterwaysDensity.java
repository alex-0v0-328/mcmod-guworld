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

/**
 * 密度函数类型 {@code guworld:waterways}：刻入水系的区域最终密度。
 *
 * <ul>
 *   <li>各区域噪声设置用它包住移植的最终密度（{@link #getCarved}），写明区域名、边长、干旱、河床、摆动噪声；
 *       由 {@link WaterwaysRegistration} 注册。</li>
 *   <li>实心处取原密度与 {@link #SLOPE} ×（刻后地表 − y）的较小者（{@link WaterwaysCarver#getSurface}）：只削不填，
 *       洞穴保留；空气原样返回，地表只由实心方块来问。噪声区块内岸带经本副本包住的地形读地面，列缓存已填好。</li>
 *   <li>雕刻器由 {@link WaterwaysRuntime} 按摆动噪声实例认领：{@code RandomState} 每维度只建一个，交给区块复制的每个副
 *       本；未挂雕刻器的随机状态（如测试自建）地形原样。</li>
 *   <li>副本在首个实心方块查一次雕刻器；噪声区块每区块复制本函数，挂、摘从下一区块生效。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see WaterwaysRegistration
 * @since 1.0.0
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
            NoiseHolder.CODEC.fieldOf("meander").forGetter(density -> density.meander),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(WaterwaysDensity::argument)
    ).apply(instance, (name, size, dry, bed, meander, argument) ->
            new WaterwaysDensity(new Waterways.Region(name, size, dry, bed), meander, argument)));
    static final KeyDispatchDataCodec<WaterwaysDensity> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final Waterways.Region region;
    private final NoiseHolder meander;
    private final DensityFunction argument;
    private Optional<WaterwaysCarver> carver;

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
        while (function instanceof DensityFunctions.HolderHolder holder) {
            function = holder.function().value();
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
        Optional<WaterwaysCarver> resolved = carver;
        if (resolved == null) {
            resolved = Optional.ofNullable(WaterwaysRuntime.find(meander.noise()));
            carver = resolved;
        }
        if (resolved.isEmpty()) return density;
        double surface = resolved.get().getSurface(context.blockX(), context.blockZ(),
                context instanceof NoiseChunk ? argument : null);
        return surface == Double.POSITIVE_INFINITY ? density : Math.min(density, SLOPE * (surface - context.blockY()));
    }
}
