package net.alex.guzhenrenworld.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

/**
 * How a region fills the hooks of its copy of the ported router ({@link Ports#getHookKey}): a constant, or a switch
 * that points the hook back at one of the region's ported functions when on and at zero when off, so an off feature
 * costs nothing.
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

    public static void function(BootstrapContext<DensityFunction> context, String region, String hook,
            DensityFunction function) {
        context.register(Ports.getHookKey(region, hook), function);
    }

    public static void switched(BootstrapContext<DensityFunction> context, String region, String hook,
            String ported, boolean on) {
        if (!on) {
            context.register(Ports.getHookKey(region, hook), DensityFunctions.zero());
            return;
        }
        Holder<DensityFunction> function = context.lookup(Registries.DENSITY_FUNCTION)
                .getOrThrow(Ports.getRegionalKey(region, "terrain/" + ported));
        context.register(Ports.getHookKey(region, hook), new DensityFunctions.HolderHolder(function));
    }
}
