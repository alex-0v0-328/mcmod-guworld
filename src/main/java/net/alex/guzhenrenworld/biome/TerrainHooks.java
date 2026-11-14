package net.alex.guzhenrenworld.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * 域填移植路由钩子（{@link Ports#getHookKey}）的方式：常量或开关。开关开时指回本域某个移植函数（全名
 * {@code terrain/...}），关时为零，关掉的功能零开销；开关够到时域才注册该函数
 * （{@link Ports#registerDensityFunctions}）。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
