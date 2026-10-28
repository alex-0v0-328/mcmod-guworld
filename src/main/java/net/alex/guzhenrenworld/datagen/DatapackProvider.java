package net.alex.guzhenrenworld.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSea;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSeaRoster;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSeaTerrain;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlains;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlainsRoster;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlainsTerrain;
import net.alex.guzhenrenworld.dimension.southernborder.BiomeSettings;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.dimension.southernborder.Terrain;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesert;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesertRoster;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesertTerrain;
import net.alex.guzhenrenworld.feature.SpiritSpring;
import net.alex.guzhenrenworld.feature.SpiritSpringRegistration;
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
 * ({@link SouthernBorder}), its density functions, Tectonic's router with the region's hooks ({@link Terrain}),
 * and its bamboo sea with that biome's bamboo ({@link BiomeSettings}); the same three entries and density functions
 * of the Western Desert [西漠] ({@link WesternDesert}), the Eastern Sea [东海] ({@link EasternSea}) and the Northern
 * Plains [北原] ({@link NorthernPlains}), each region's copy of the ported terrain under its own prefix; Gu World's
 * shared biomes, the vanilla replicas ({@link Replicas#SHARED}) and each region's own ({@code Roster.REPLICAS}), the
 * biomes ported from Terralith with their features, {@link Ports#BIOMES} and each region's {@code Roster.PORTED},
 * and the noises both ports read ({@link Ports}); and the Spirit Spring [元泉] configured and placed feature
 * ({@link SpiritSpring}) and biome modifier ({@link SpiritSpringRegistration}). A registry two dimensions both fill
 * gets one bootstrap that runs both. The tag provider takes {@code getRegistryProvider()} from this instance, not the
 * plain lookup, so the tag pass sees the biome this run generates.
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
                WesternDesert.registerDimensionType(context);
                EasternSea.registerDimensionType(context);
                NorthernPlains.registerDimensionType(context);
            })
            .add(Registries.BIOME, context -> {
                TreasureYellowHeaven.registerBiome(context);
                Replicas.registerBiomes(context, getJoined(Replicas.SHARED,
                        WesternDesertRoster.REPLICAS,
                        EasternSeaRoster.REPLICAS,
                        NorthernPlainsRoster.REPLICAS));
                Ports.registerBiomes(context, getJoined(Ports.BIOMES,
                        WesternDesertRoster.PORTED,
                        EasternSeaRoster.PORTED,
                        NorthernPlainsRoster.PORTED));
                BiomeSettings.registerBiomes(context);
            })
            .add(Registries.NOISE, Ports::registerNoises)
            .add(Registries.DENSITY_FUNCTION, context -> {
                Terrain.registerDensityFunctions(context);
                WesternDesertTerrain.registerDensityFunctions(context);
                EasternSeaTerrain.registerDensityFunctions(context);
                NorthernPlainsTerrain.registerDensityFunctions(context);
            })
            .add(Registries.NOISE_SETTINGS, context -> {
                SouthernBorder.registerNoiseSettings(context);
                WesternDesert.registerNoiseSettings(context);
                EasternSea.registerNoiseSettings(context);
                NorthernPlains.registerNoiseSettings(context);
            })
            .add(Registries.LEVEL_STEM, context -> {
                TreasureYellowHeaven.registerLevelStem(context);
                SouthernBorder.registerLevelStem(context);
                WesternDesert.registerLevelStem(context);
                EasternSea.registerLevelStem(context);
                NorthernPlains.registerLevelStem(context);
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
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, SpiritSpringRegistration::registerBiomeModifier);

    public DatapackProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider, BUILDER, Set.of(GuWorld.MOD_ID));
    }

    @SafeVarargs
    private static <T> List<T> getJoined(List<T>... parts) {
        List<T> joined = new ArrayList<>();
        for (List<T> part : parts) joined.addAll(part);
        return List.copyOf(joined);
    }
}
