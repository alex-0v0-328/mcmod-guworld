package net.alex.guzhenrenworld.biome;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * {@link XGradient}、{@link ZGradient} 的密度函数类型注册（{@code guworld:x_gradient}、{@code guworld:z_gradient}），
 * 与函数分开放。{@link #onRegister} 按 {@code ID} 注册 {@code MAP_CODEC}；数据生成时也触发，provider 才能编码点名这些类
 * 型的域地形。
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
