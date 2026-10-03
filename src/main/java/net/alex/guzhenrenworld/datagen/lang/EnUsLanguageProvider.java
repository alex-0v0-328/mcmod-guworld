package net.alex.guzhenrenworld.datagen.lang;

import net.alex.guzhenrenworld.GuWorld;
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
    }
}
