package net.alex.guzhenrenworld.biome;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The loader's side of {@link ZGradient}: its registration as the density function type {@code guworld:z_gradient},
 * everything here that names the mod loader, so the function itself stays the same on every loader.
 *
 * <p>Annotated {@code @EventBusSubscriber}: {@link #onRegister} registers {@link ZGradient#MAP_CODEC} under
 * {@link ZGradient#ID}. The event also runs in the data run, so the datagen providers can encode a region's terrain
 * that names the type.
 *
 * <p>⚠ Loader glue, a version seam: Minecraft 1.21.1's density function type registry holds map codecs, 1.20.1's
 * holds codecs, so the {@code forge-1.20.1} copy registers {@code ZGradient.MAP_CODEC.codec()} instead.
 *
 * @author Alex
 * @version 1.0.0
 * @see ZGradient
 * @since 1.0.0
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
public final class ZGradientRegistration {

    private ZGradientRegistration() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.DENSITY_FUNCTION_TYPE, GuWorld.id(ZGradient.ID), () -> ZGradient.MAP_CODEC);
    }
}
