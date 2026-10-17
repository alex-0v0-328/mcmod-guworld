package net.alex.guzhenrenworld.biome;

import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Gu World's biome keys: {@link #key} makes one from its name in this mod's namespace, and {@link #addTranslations}
 * writes the names of a list of them in one language.
 *
 * <p>Each class that holds a list of biomes, {@link Replicas}, {@link Ports} and a region's own, builds its
 * translation keys here from the registered keys, never from a raw string, so a renamed biome cannot leave a key
 * behind pointing at nothing; each language provider passes only its names, in the list's order.
 *
 * <p>⚠ {@link #addTranslations} throws when the two lists differ in length, so a biome added without a name fails
 * {@code runData} instead of shipping a raw key.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class Keys {

    private Keys() {}

    public static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, GuWorld.id(name));
    }

    public static void addTranslations(LanguageProvider languageProvider, List<ResourceKey<Biome>> biomes,
            List<String> names) {
        if (names.size() != biomes.size()) {
            throw new IllegalArgumentException(names.size() + " names for the " + biomes.size() + " biomes " + biomes);
        }
        for (int index = 0; index < biomes.size(); index++) {
            languageProvider.add(biomes.get(index).location().toLanguageKey("biome"), names.get(index));
        }
    }
}
