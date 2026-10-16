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
 * <p>Extends {@link LanguageProvider} for {@code en_us}. Each function package builds its own keys
 * ({@link TreasureYellowHeaven#addTranslations}); this table passes only the English renderings.
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
        TreasureYellowHeaven.addTranslations(this, "Treasure Yellow Heaven");
        SouthernBorder.addTranslations(this, "Southern Border");
        Replicas.addTranslations(this, List.of("Plains", "Forest", "Birch Forest", "Dark Forest", "Bamboo Jungle",
                "Meadow", "Taiga", "Windswept Hills", "Stony Peaks", "River", "Lush Caves", "Dripstone Caves"));
        Ports.addTranslations(this, List.of("Plateau", "Forested Plateau", "Cloud Forest", "Misty Mountains",
                "Rainforest", "Hot Springs", "Orchid Swamp", "Peach Grove"));
        SouthernBorderBiomes.addTranslations(this, List.of("Karst Peak Forest", "Danxia", "Sandstone Pillars",
                "Zhangshiyan Cliffs"));
    }
}
