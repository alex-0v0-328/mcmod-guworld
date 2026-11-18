package net.alex.guzhenrenworld.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * 各域填充 Tectonic 移植路由钩子（{@link Ports#getHookKey}）的注册辅助类，提供常量（{@link #constant}）与布尔开关（
 * {@link #switched}）两种装配方式。开关开启时将钩子转发至本域对应的移植密度函数（{@code terrain/...}），关闭时直接绑定
 * 为常量 {@code 0} 以消除运行时求值开销；仅当开关开启且钩子可达时，{@link Ports#registerDensityFunctions} 才会将目标函
 * 数注册到数据包中。
 */

public final class TerrainHooks {

    private TerrainHooks() {}

    public static void constant(BootstrapContext<DensityFunction> context, String region, String hook,
            double value) {
        context.register(Ports.getHookKey(region, hook), DensityFunctions.constant(value));
    }

    public static void switched(BootstrapContext<DensityFunction> context, String region, String hook,
            String ported, boolean on) {
        if (!on) {
            context.register(Ports.getHookKey(region, hook), DensityFunctions.zero());
            return;
        }
        Holder<DensityFunction> function = context.lookup(Registries.DENSITY_FUNCTION)
                .getOrThrow(Ports.getRegionalKey(region, ported));
        context.register(Ports.getHookKey(region, hook), new DensityFunctions.HolderHolder(function));
    }
}
