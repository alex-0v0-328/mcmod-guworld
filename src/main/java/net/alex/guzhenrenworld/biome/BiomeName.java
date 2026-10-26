package net.alex.guzhenrenworld.biome;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * A biome's display name in the two authored languages, English and Simplified Chinese, which the language provider
 * ({@code datagen.lang.Translations}) writes; Traditional Chinese is converted from the Simplified column. Each region
 * lists the names of the biomes only it places, so a region's package names its own biomes and the provider only
 * collects the lists.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public record BiomeName(ResourceKey<Biome> biome, String english, String simplified) {}
