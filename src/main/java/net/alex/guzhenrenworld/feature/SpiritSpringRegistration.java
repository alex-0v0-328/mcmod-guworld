package net.alex.guzhenrenworld.feature;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 元泉世界生成的注册入口：负责注册地物类型与挂载放置地物的群系修改器。
 * <ul>
 *   <li>与纯数据定义类 {@link SpiritSpring} 分离：{@link #onRegister} 在模组总线上以 {@link SpiritSpring#ID} 注册
 *       {@link SpiritSpringCluster} 地物（导出为 {@link #FEATURE}）。</li>
 *   <li>{@link #registerBiomeModifier} 由 {@code datagen.DatapackProvider} 在数据生成阶段调用，在
 *       {@code TOP_LAYER_MODIFICATION} 阶段（植被装饰之后）将 {@link SpiritSpring#PLACED_FEATURE} 注入
 *       {@link SpiritSpring#BIOMES} 标签下的全部陆地群系。</li>
 * </ul>
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
public final class SpiritSpringRegistration {

    public static final DeferredHolder<Feature<?>, SpiritSpringCluster> FEATURE =
            DeferredHolder.create(Registries.FEATURE, SpiritSpring.ID);
    private static final ResourceKey<BiomeModifier> BIOME_MODIFIER =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, SpiritSpring.ID);

    private SpiritSpringRegistration() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.FEATURE, SpiritSpring.ID, SpiritSpringCluster::new);
    }

    public static void registerBiomeModifier(BootstrapContext<BiomeModifier> context) {
        HolderSet<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(SpiritSpring.BIOMES);
        context.register(BIOME_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(biomes,
                HolderSet.direct(context.lookup(Registries.PLACED_FEATURE).getOrThrow(SpiritSpring.PLACED_FEATURE)),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
    }
}
