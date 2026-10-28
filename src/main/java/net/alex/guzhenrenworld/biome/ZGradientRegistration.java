package net.alex.guzhenrenworld.biome;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The registration of {@link ZGradient} as the density function type {@code guworld:z_gradient}, kept apart from the
 * function itself.
 *
 * <p>Annotated {@code @EventBusSubscriber}: {@link #onRegister} registers {@link ZGradient#MAP_CODEC} under
 * {@link ZGradient#ID}. The event also runs in the data run, so the datagen providers can encode a region's terrain
 * that names the type.
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
