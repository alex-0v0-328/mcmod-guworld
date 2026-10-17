package net.alex.guzhenrenworld.datagen.lang;

import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.alex.guzhenrenworld.dimension.southernborder.BiomeLayout;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * The Chinese strings of this mod, beside the English ones.
 *
 * <p>Extends {@link LanguageProvider} for {@code zh_cn}. Keys are built the same way as in
 * {@link EnUsLanguageProvider}, by {@link Translations}.
 *
 * <p>⚠ These renderings are the authority, not a translation of the English. Deriving either side
 * from the other is how a name quietly comes to mean something it never meant.
 *
 * <p>{@link ZhTwLanguageProvider} extends this table through the protected locale constructor and
 * converts every value on its way in, so a string written here reaches {@code zh_tw} with no second
 * line.
 *
 * @author Alex
 * @version 1.0.0
 * @see EnUsLanguageProvider
 * @since 1.0.0
 */

public class ZhCnLanguageProvider extends LanguageProvider {

    public ZhCnLanguageProvider(PackOutput packOutput) {
        this(packOutput, "zh_cn");
    }

    protected ZhCnLanguageProvider(PackOutput packOutput, String locale) {
        super(packOutput, GuWorld.MOD_ID, locale);
    }

    @Override
    protected void addTranslations() {
        Translations.addDimension(this, TreasureYellowHeaven.LEVEL_STEM, "宝黄天");
        Translations.addTitleColor(this, TreasureYellowHeaven.LEVEL_STEM, TreasureYellowHeaven.COLOR);
        Translations.addBiomes(this, List.of(TreasureYellowHeaven.BIOME), List.of("宝黄天"));
        Translations.addDimension(this, SouthernBorder.LEVEL_STEM, "南疆");
        Translations.addBiomes(this, Replicas.BIOMES, List.of("平原", "森林", "桦木森林", "黑森林", "竹林", "草甸",
                "针叶林", "风袭丘陵", "裸岩山峰", "河流", "繁茂洞穴", "溶洞"));
        Translations.addBiomes(this, Ports.BIOMES, List.of("高原", "林间高原", "云雾林", "雾山", "雨林", "热海", "兰花沼泽",
                "桃林"));
        Translations.addBiomes(this, BiomeLayout.LANDFORMS, List.of("喀斯特峰林", "丹霞", "砂岩峰林", "嶂石岩"));
    }
}
