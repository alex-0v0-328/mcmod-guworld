package net.alex.guzhenrenworld.datagen.lang;

import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.SouthernBorder;
import net.alex.guzhenrenworld.dimension.SouthernBorderBiomes;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * The English strings of this mod.
 *
 * <p>Extends {@link LanguageProvider} for {@code en_us}. {@link Translations} builds every key from the registered
 * keys and lists the function packages hold; this table passes only the English renderings, in the lists' order. The
 * Treasure Yellow Heaven's one biome carries the dimension's name.
 *
 * @author Alex
 * @version 1.0.0
 * @see ZhCnLanguageProvider
 * @since 1.0.0
 */

public class EnUsLanguageProvider extends LanguageProvider {

    public EnUsLanguageProvider(PackOutput packOutput) {
        super(packOutput, GuWorld.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        Translations.addDimension(this, TreasureYellowHeaven.LEVEL_STEM, "Treasure Yellow Heaven");
        Translations.addTitleColor(this, TreasureYellowHeaven.LEVEL_STEM, TreasureYellowHeaven.COLOR);
        Translations.addBiomes(this, List.of(TreasureYellowHeaven.BIOME), List.of("Treasure Yellow Heaven"));
        Translations.addDimension(this, SouthernBorder.LEVEL_STEM, "Southern Border");
        Translations.addBiomes(this, Replicas.BIOMES, List.of("Plains", "Forest", "Birch Forest", "Dark Forest",
                "Bamboo Jungle", "Meadow", "Taiga", "Windswept Hills", "Stony Peaks", "River", "Lush Caves",
                "Dripstone Caves"));
        Translations.addBiomes(this, Ports.BIOMES, List.of("Plateau", "Forested Plateau", "Cloud Forest",
                "Misty Mountains", "Rainforest", "Hot Springs", "Orchid Swamp", "Peach Grove"));
        Translations.addBiomes(this, SouthernBorderBiomes.LANDFORMS, List.of("Karst Peak Forest", "Danxia",
                "Sandstone Pillars", "Zhangshiyan Cliffs"));
    }
}
