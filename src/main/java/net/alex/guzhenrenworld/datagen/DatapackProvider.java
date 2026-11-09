package net.alex.guzhenrenworld.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.Regions;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.alex.guzhenrenworld.dimension.southernborder.BiomeSettings;
import net.alex.guzhenrenworld.feature.SpiritSpring;
import net.alex.guzhenrenworld.feature.SpiritSpringRegistration;
import net.alex.guzhenrenworld.waterways.Waterways;
import net.alex.guzhenrenworld.waterways.WaterwaysRegistration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The single provider for every datapack registry this mod writes.
 *
 * <p>Extends {@link DatapackBuiltinEntriesProvider}. Builds in one {@code RegistrySetBuilder} the entries the function
 * packages bootstrap: the Treasure Yellow Heaven dimension's type, biome and level stem ({@link TreasureYellowHeaven});
 * for each of the four regions ({@link Regions#ALL}) the dimension's type, density functions (Tectonic's router with
 * the region's hooks), noise settings and level stem ({@link RegionBootstraps}), each region's copy of the ported
 * terrain under its own prefix, and the Southern Border's [南疆] bamboo sea with that biome's bamboo ({@link
 * BiomeSettings}); Gu World's shared biomes, the vanilla replicas ({@link Replicas#SHARED}) and each region's own, the
 * biomes ported from Terralith with their features, {@link Ports#BIOMES} and each region's own, and the noises both
 * ports read ({@link Ports}); and the Spirit Spring [元泉] configured and placed feature ({@link SpiritSpring}) and biome
 * modifier ({@link SpiritSpringRegistration}). A registry two dimensions both fill gets one bootstrap that runs both.
 * The tag provider takes {@code getRegistryProvider()} from this instance, not the plain lookup, so the tag pass sees
 * the biome this run generates.
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
                Regions.ALL.forEach(region -> RegionBootstraps.registerDimensionType(context, region));
            })
            .add(Registries.BIOME, context -> {
                TreasureYellowHeaven.registerBiome(context);
                Replicas.registerBiomes(context, getJoined(Replicas.SHARED, Region.Biomes::replicas));
                Ports.registerBiomes(context, getJoined(Ports.BIOMES, Region.Biomes::ported));
                BiomeSettings.registerBiomes(context);
            })
            .add(Registries.NOISE, context -> {
                Ports.registerNoises(context);
                Regions.ALL.forEach(region -> Ports.registerNoises(context, region.name()));
                Waterways.registerNoises(context);
            })
            .add(Registries.DENSITY_FUNCTION, context -> {
                Regions.ALL.forEach(region -> RegionBootstraps.registerDensityFunctions(context, region));
            })
            .add(Registries.NOISE_SETTINGS, context -> {
                Regions.ALL.forEach(region -> RegionBootstraps.registerNoiseSettings(context, region));
            })
            .add(Registries.LEVEL_STEM, context -> {
                TreasureYellowHeaven.registerLevelStem(context);
                Regions.ALL.forEach(region -> RegionBootstraps.registerLevelStem(context, region));
            })
            .add(Registries.CONFIGURED_FEATURE, context -> {
                SpiritSpring.registerConfiguredFeature(context);
                WaterwaysRegistration.registerConfiguredFeature(context);
                Ports.registerConfiguredFeatures(context);
            })
            .add(Registries.PLACED_FEATURE, context -> {
                SpiritSpring.registerPlacedFeature(context);
                WaterwaysRegistration.registerPlacedFeature(context);
                Ports.registerPlacedFeatures(context);
                BiomeSettings.registerPlacedFeatures(context);
            })
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                SpiritSpringRegistration.registerBiomeModifier(context);
                WaterwaysRegistration.registerBiomeModifier(context, Regions.ALL.stream()
                        .flatMap(region -> region.biomes().roster().stream()).toList());
            });

    public DatapackProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider, BUILDER, Set.of(GuWorld.MOD_ID));
    }

    private static <T> List<T> getJoined(List<T> shared, Function<Region.Biomes, List<T>> part) {
        List<T> joined = new ArrayList<>(shared);
        Regions.ALL.forEach(region -> joined.addAll(part.apply(region.biomes())));
        return List.copyOf(joined);
    }
}
