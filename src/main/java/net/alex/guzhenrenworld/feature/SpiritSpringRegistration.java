package net.alex.guzhenrenworld.feature;

import net.alex.guzhenrenworld.DatapackBootstrap;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * The loader's side of the Spirit Spring [元泉] worldgen: the feature's registration and the biome modifier that
 * places it, everything here that names the mod loader, so {@link SpiritSpring} stays the same on every loader.
 *
 * <p>Annotated {@code @Mod.EventBusSubscriber} on the mod bus: {@link #onRegister} registers
 * {@link SpiritSpringFeature} under {@code SpiritSpring.ID}, which {@link #FEATURE} names.
 * {@link #registerBiomeModifier} is the bootstrap {@code datagen.DatapackProvider} runs for the biome modifier adding
 * the placed feature to {@link SpiritSpring#BIOMES} at {@code TOP_LAYER_MODIFICATION} (after vegetation).
 *
 * @author Alex
 * @version 1.0.0
 * @see SpiritSpring
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = GuWorld.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SpiritSpringRegistration {

    public static final RegistryObject<SpiritSpringFeature> FEATURE =
            RegistryObject.create(SpiritSpring.ID, ForgeRegistries.FEATURES);
    private static final ResourceKey<BiomeModifier> BIOME_MODIFIER =
            ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS, SpiritSpring.ID);

    private SpiritSpringRegistration() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.FEATURE, SpiritSpring.ID, SpiritSpringFeature::new);
    }

    public static void registerBiomeModifier(DatapackBootstrap<BiomeModifier> context) {
        HolderSet<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(SpiritSpring.BIOMES);
        context.register(BIOME_MODIFIER, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(biomes,
                HolderSet.direct(context.lookup(Registries.PLACED_FEATURE).getOrThrow(SpiritSpring.PLACED_FEATURE)),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
    }
}
