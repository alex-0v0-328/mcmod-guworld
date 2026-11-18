package net.alex.guzhenrenworld.biome;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * {@link XGradient} 与 {@link ZGradient} 密度函数类型的注册入口（{@code guworld:x_gradient}、{@code guworld:z_gradient}
 * ）。{@link #onRegister} 在模组总线上按各自 {@code ID} 注册 {@code MAP_CODEC}，与密度函数实现类分离；数据生成阶段同样
 * 会触发该事件，确保数据包生成器能编码引用这两种类型的域地形。
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
