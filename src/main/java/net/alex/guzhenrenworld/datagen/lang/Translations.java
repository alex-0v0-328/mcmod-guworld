package net.alex.guzhenrenworld.datagen.lang;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Builds every translation key of this mod from the registered key it names, never from a raw string, so a renamed
 * dimension or biome cannot leave a key behind pointing at nothing; the language providers pass only their
 * renderings, and the function packages only their keys and lists.
 *
 * <p>{@link #addDimension} writes a dimension's name and its Traveler's Titles title; {@link #addTitleColor} adds the
 * title's color, the {@code travelerstitles.*} convention's lowercase hex without {@code #}, which is no rendering, so
 * every language writes the same value. {@link #addBiomes} names a list of biomes in the list's order.
 *
 * <p>⚠ {@link #addBiomes} throws when the two lists differ in length, so a biome added without a name fails
 * {@code runData} instead of shipping a raw key.
 *
 * @author Alex
 * @version 1.0.0
 * @see EnUsLanguageProvider
 * @since 1.0.0
 */

final class Translations {

    private Translations() {}

    static void addDimension(LanguageProvider languageProvider, ResourceKey<LevelStem> dimension, String name) {
        languageProvider.add(dimension.location().toLanguageKey("dimension"), name);
        languageProvider.add(dimension.location().toLanguageKey("travelerstitles"), name);
    }

    static void addTitleColor(LanguageProvider languageProvider, ResourceKey<LevelStem> dimension, int color) {
        languageProvider.add(dimension.location().toLanguageKey("travelerstitles", "color"), "%06x".formatted(color));
    }

    static void addBiomes(LanguageProvider languageProvider, List<ResourceKey<Biome>> biomes, List<String> names) {
        if (names.size() != biomes.size()) {
            throw new IllegalArgumentException(names.size() + " names for the " + biomes.size() + " biomes " + biomes);
        }
        for (int index = 0; index < biomes.size(); index++) {
            languageProvider.add(biomes.get(index).location().toLanguageKey("biome"), names.get(index));
        }
    }
}
