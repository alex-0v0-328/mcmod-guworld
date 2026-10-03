package net.alex.guzhenrenworld.datagen;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
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
 * ({@link TreasureYellowHeaven}), and the Spirit Spring [元泉] configured and placed feature and biome
 * modifier ({@link SpiritSpring}). The tag provider takes {@code getRegistryProvider()} from this
 * instance, not the plain lookup, so the tag pass sees the biome this run generates.
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
            .add(Registries.DIMENSION_TYPE, TreasureYellowHeaven::registerDimensionType)
            .add(Registries.BIOME, TreasureYellowHeaven::registerBiome)
            .add(Registries.LEVEL_STEM, TreasureYellowHeaven::registerLevelStem)
            .add(Registries.CONFIGURED_FEATURE, SpiritSpring::registerConfiguredFeature)
            .add(Registries.PLACED_FEATURE, SpiritSpring::registerPlacedFeature)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, SpiritSpring::registerBiomeModifier);

    public DatapackProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider, BUILDER, Set.of(GuWorld.MOD_ID));
    }
}
