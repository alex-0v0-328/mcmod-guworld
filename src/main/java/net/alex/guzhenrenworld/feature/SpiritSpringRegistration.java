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
 * 元泉[Spirit Spring] 世界生成的注册：地物与放置它的群系修改器。
 *
 * <ul>
 *   <li>{@link SpiritSpring} 因此只留键、群系与地物引导；{@link #onRegister} 以 {@code SpiritSpring.ID}
 *       注册 {@link SpiritSpringCluster}（即 {@link #FEATURE}）。</li>
 *   <li>{@link #registerBiomeModifier} 是 {@code datagen.DatapackProvider} 运行的引导：
 *       在 {@code TOP_LAYER_MODIFICATION}（植被后）把放置地物加进 {@link SpiritSpring#BIOMES}。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SpiritSpring
 * @since 1.0.0
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
