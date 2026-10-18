package net.alex.guzhenrenworld.datagen;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.alex.guzhenrenworld.dimension.southernborder.BiomeSettings;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.dimension.southernborder.Terrain;
import net.alex.guzhenrenworld.feature.SpiritSpring;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The single provider for every datapack registry this mod writes.
 *
 * <p>Extends {@link DatapackBuiltinEntriesProvider}. Builds in one {@code RegistrySetBuilder} the entries
 * the function packages bootstrap: the Treasure Yellow Heaven dimension's type, biome and level stem
 * ({@link TreasureYellowHeaven}); the Southern Border [南疆] dimension's type, noise settings and level stem
 * ({@link SouthernBorder}), its density functions, Terralith's router with the region's hooks ({@link Terrain}),
 * and its bamboo sea with that biome's bamboo ({@link BiomeSettings}); Gu World's shared biomes, the vanilla replicas ({@link Replicas}) and
 * the biomes ported from Terralith with their noises and features ({@link Ports}); and the Spirit Spring
 * [元泉] configured and placed feature and biome modifier ({@link SpiritSpring}). A registry two
 * dimensions both fill gets one bootstrap that runs both. The tag provider takes {@code getRegistryProvider()}
 * from this instance, not the plain lookup, so the tag pass sees the biome this run generates.
 *
 * <p>⚠ There can only be one per data run. The builtin-entries provider reports a fixed name, so a
 * second instance fails datagen outright; add a registry to this one's builder instead.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class DatapackProvider extends DatapackBuiltinEntriesProvider {

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, context -> {
                TreasureYellowHeaven.registerDimensionType(context);
                SouthernBorder.registerDimensionType(context);
            })
            .add(Registries.BIOME, context -> {
                TreasureYellowHeaven.registerBiome(context);
                Replicas.registerBiomes(context);
                Ports.registerBiomes(context);
                BiomeSettings.registerBiomes(context);
            })
            .add(Registries.NOISE, Ports::registerNoises)
            .add(Registries.DENSITY_FUNCTION, Terrain::registerDensityFunctions)
            .add(Registries.NOISE_SETTINGS, SouthernBorder::registerNoiseSettings)
            .add(Registries.LEVEL_STEM, context -> {
                TreasureYellowHeaven.registerLevelStem(context);
                SouthernBorder.registerLevelStem(context);
            })
            .add(Registries.CONFIGURED_FEATURE, context -> {
                SpiritSpring.registerConfiguredFeature(context);
                Ports.registerConfiguredFeatures(context);
            })
            .add(Registries.PLACED_FEATURE, context -> {
                SpiritSpring.registerPlacedFeature(context);
                Ports.registerPlacedFeatures(context);
                BiomeSettings.registerPlacedFeatures(context);
            })
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, SpiritSpring::registerBiomeModifier);

    public DatapackProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider, BUILDER, Set.of(GuWorld.MOD_ID));
    }
}
