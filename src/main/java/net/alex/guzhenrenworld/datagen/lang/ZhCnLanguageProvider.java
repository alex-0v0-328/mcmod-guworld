package net.alex.guzhenrenworld.datagen.lang;

import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.registry.WorldDimensions;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * The Chinese strings of this mod, beside the English ones.
 *
 * <p>Extends {@link net.neoforged.neoforge.common.data.LanguageProvider} for {@code zh_cn}. Keys are
 * built the same way as in {@link EnUsLanguageProvider}.
 *
 * <p>⚠ These renderings are the authority, not a translation of the English. Deriving either side
 * from the other is how a name quietly comes to mean something it never meant.
 *
 * @author Alex
 * @version 1.0.0
 * @see EnUsLanguageProvider
 * @since 1.0.0
 */

public class ZhCnLanguageProvider extends LanguageProvider {

    public ZhCnLanguageProvider(PackOutput output) {
        super(output, GuWorld.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        addDimensionKeys();
    }

    //region DIMENSION
    private void addDimensionKeys() {
        ResourceLocation dimension = WorldDimensions.TREASURE_YELLOW_HEAVEN_STEM.location();
        ResourceLocation biome = WorldDimensions.TREASURE_YELLOW_HEAVEN_BIOME.location();
        add(dimension.toLanguageKey("dimension"), "宝黄天");
        add(biome.toLanguageKey("biome"), "宝黄天");
        add(dimension.toLanguageKey("travelerstitles"), "宝黄天");
        add(dimension.toLanguageKey("travelerstitles", "color"), "f4d35e");
    }
    //endregion
}
