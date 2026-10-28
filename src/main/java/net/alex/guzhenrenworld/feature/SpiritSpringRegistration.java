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
 * The registrations of the Spirit Spring [元泉] worldgen: the feature itself and the biome modifier that places it,
 * so {@link SpiritSpring} keeps only keys, biomes and the feature bootstraps.
 *
 * <p>Annotated {@code @EventBusSubscriber}: {@link #onRegister} registers {@link SpiritSpringCluster} under
 * {@code SpiritSpring.ID}, which {@link #FEATURE} names. {@link #registerBiomeModifier} is the bootstrap
 * {@code datagen.DatapackProvider} runs for the biome modifier adding the placed feature to {@link SpiritSpring#BIOMES}
 * at {@code TOP_LAYER_MODIFICATION} (after vegetation).
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
