package net.alex.guzhenrenworld.biome;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The registration of {@link XGradient} and {@link ZGradient} as the density function types
 * {@code guworld:x_gradient} and {@code guworld:z_gradient}, kept apart from the functions themselves.
 *
 * <p>Annotated {@code @EventBusSubscriber}: {@link #onRegister} registers each type's {@code MAP_CODEC} under its
 * {@code ID}. The event also runs in the data run, so the datagen providers can encode a region's terrain that names
 * the types.
 *
 * @author Alex
 * @version 1.0.0
 * @see XGradient
 * @see ZGradient
 * @since 1.0.0
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
public final class GradientRegistration {

    private GradientRegistration() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.DENSITY_FUNCTION_TYPE, GuWorld.id(XGradient.ID), () -> XGradient.MAP_CODEC);
        event.register(Registries.DENSITY_FUNCTION_TYPE, GuWorld.id(ZGradient.ID), () -> ZGradient.MAP_CODEC);
    }
}
