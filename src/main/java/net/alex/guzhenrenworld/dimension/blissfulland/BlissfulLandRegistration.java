package net.alex.guzhenrenworld.dimension.blissfulland;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 福地的代码注册入口：注册区块生成器类型 {@code guworld:bounded_noise}（{@link BoundedNoiseGenerator#CODEC}）。
 * <ul>
 *   <li>⚠ 生成器类型 ID 写进维度层级 JSON 与存档的维度设置；改名后福地读不回来。</li>
 * </ul>
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
public final class BlissfulLandRegistration {

    private BlissfulLandRegistration() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.CHUNK_GENERATOR, GuWorld.id(BoundedNoiseGenerator.ID),
                () -> BoundedNoiseGenerator.CODEC);
    }
}
