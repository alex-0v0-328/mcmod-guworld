package net.alex.guzhenrenworld.datagen.lang;

import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.registry.WorldDimensions;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * The English strings of this mod.
 *
 * <p>Extends {@link net.neoforged.neoforge.common.data.LanguageProvider} for {@code en_us}. Every key
 * is built from the registered key, never written as a raw string, so a renamed dimension cannot
 * leave a key behind pointing at nothing. The {@code travelerstitles.*} pair is the Traveler's Titles
 * convention: the dimension title, and its color as lowercase hex without {@code #}.
 *
 * @author Alex
 * @version 1.0.0
 * @see ZhCnLanguageProvider
 * @since 1.0.0
 */

public class EnUsLanguageProvider extends LanguageProvider {

    public EnUsLanguageProvider(PackOutput output) {
        super(output, GuWorld.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        addDimensionKeys();
    }

    //region DIMENSION
    private void addDimensionKeys() {
        ResourceLocation dimension = WorldDimensions.TREASURE_YELLOW_HEAVEN_STEM.location();
        ResourceLocation biome = WorldDimensions.TREASURE_YELLOW_HEAVEN_BIOME.location();
        add(dimension.toLanguageKey("dimension"), "Treasure Yellow Heaven");
        add(biome.toLanguageKey("biome"), "Treasure Yellow Heaven");
        add(dimension.toLanguageKey("travelerstitles"), "Treasure Yellow Heaven");
        add(dimension.toLanguageKey("travelerstitles", "color"), "f4d35e");
    }
    //endregion
}
