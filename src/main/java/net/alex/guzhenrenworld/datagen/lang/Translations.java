package net.alex.guzhenrenworld.datagen.lang;

import java.util.List;
import java.util.Locale;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.BiomeName;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSea;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSeaRoster;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlains;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlainsRoster;
import net.alex.guzhenrenworld.dimension.southernborder.BiomeSettings;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesert;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesertRoster;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * Every string of this mod in all three languages, written as one table: each entry names its registered key, its
 * English rendering and its Simplified Chinese one. The same provider runs once per {@link Language} and writes
 * only its own column, so no language can miss a key the others have. Traditional Chinese [繁中] converts the
 * Simplified column through {@link TraditionalGlyphs}; the two differ in glyphs only.
 *
 * <p>Every key is built from the registered key it names, never from a raw string, so a renamed dimension or biome
 * cannot leave a key behind pointing at nothing: {@link #getLanguageKey} spells the type, then the registered id.
 * {@link #addDimension} writes a dimension's name and its Traveler's Titles title; {@link #addTitleColor} adds the
 * title's color, the {@code travelerstitles.*} convention's lowercase hex without {@code #}, the same in every
 * language. {@link #addBiomes} names a registry list of biomes, one English and Simplified pair per biome in the
 * list's order, or a region's {@link BiomeName} list. The Treasure Yellow Heaven's one biome carries the dimension's
 * name.
 *
 * <p>⚠ Both renderings are the authority, not a translation of each other. ⚠ {@link #addBiomes} throws when the
 * pairs and the biomes differ in number, and {@link TraditionalGlyphs} throws on an unseen glyph, so a biome added
 * without a name, or a name with a new glyph, fails {@code runData} instead of shipping a raw key.
 *
 * @author Alex
 * @version 1.0.0
 * @see TraditionalGlyphs
 * @since 1.0.0
 */

public class Translations extends LanguageProvider {

    private final Language language;

    public enum Language { EN_US, ZH_CN, ZH_TW }

    public Translations(PackOutput packOutput, Language language) {
        super(packOutput, GuWorld.MOD_ID, language.name().toLowerCase(Locale.ROOT));
        this.language = language;
    }

    @Override
    protected void addTranslations() {
        addDimension(TreasureYellowHeaven.LEVEL_STEM, "Treasure Yellow Heaven", "宝黄天");
        addTitleColor(TreasureYellowHeaven.LEVEL_STEM, TreasureYellowHeaven.COLOR);
        addBiomes(List.of(TreasureYellowHeaven.BIOME), "Treasure Yellow Heaven", "宝黄天");
        addDimension(SouthernBorder.LEVEL_STEM, "Southern Border", "南疆");
        addBiomes(Replicas.BIOMES,
                "Plains", "平原",
                "Forest", "森林",
                "Birch Forest", "桦木森林",
                "Dark Forest", "黑森林",
                "Bamboo Jungle", "竹林",
                "Meadow", "草甸",
                "Taiga", "针叶林",
                "Windswept Hills", "风袭丘陵",
                "Stony Peaks", "裸岩山峰",
                "River", "河流",
                "Lush Caves", "繁茂洞穴",
                "Dripstone Caves", "溶洞");
        addBiomes(Ports.BIOMES,
                "Plateau", "高原",
                "Forested Plateau", "林间高原",
                "Cloud Forest", "云雾林",
                "Misty Mountains", "雾山",
                "Rainforest", "雨林",
                "Hot Springs", "热海",
                "Orchid Swamp", "兰花沼泽",
                "Peach Grove", "桃林",
                "Rocky Peaks", "裸岩峰",
                "Emerald Peaks", "翠峰",
                "Jungle Mountains", "丛林山",
                "Painted Mountains", "彩岩山",
                "Granite Cliffs", "花岗岩崖",
                "Alpine Meadow", "高山草甸",
                "Zhangshiyan Cliffs", "嶂石岩",
                "Karst Peak Forest", "喀斯特峰林");
        addBiomes(List.of(BiomeSettings.BAMBOO_SEA), "Bamboo Sea", "竹海");
        addDimension(WesternDesert.LEVEL_STEM, "Western Desert", "西漠");
        addBiomes(WesternDesertRoster.NAMES);
        addDimension(EasternSea.LEVEL_STEM, "Eastern Sea", "东海");
        addBiomes(EasternSeaRoster.NAMES);
        addDimension(NorthernPlains.LEVEL_STEM, "Northern Plains", "北原");
        addBiomes(NorthernPlainsRoster.NAMES);
    }

    private void addBiomes(List<BiomeName> names) {
        for (BiomeName name : names) {
            add(getLanguageKey("biome", name.biome()), getRendering(name.english(), name.simplified()));
        }
    }

    private void addDimension(ResourceKey<LevelStem> dimension, String english, String simplified) {
        String name = getRendering(english, simplified);
        add(getLanguageKey("dimension", dimension), name);
        add(getLanguageKey("travelerstitles", dimension), name);
    }

    private void addTitleColor(ResourceKey<LevelStem> dimension, int color) {
        add(getLanguageKey("travelerstitles", dimension) + ".color", "%06x".formatted(color));
    }

    private void addBiomes(List<ResourceKey<Biome>> biomes, String... renderings) {
        if (renderings.length != 2 * biomes.size()) {
            throw new IllegalArgumentException(renderings.length + " renderings for the " + biomes.size()
                    + " biomes " + biomes + ": each biome takes an English and a Simplified one");
        }
        for (int index = 0; index < biomes.size(); index++) {
            add(getLanguageKey("biome", biomes.get(index)),
                    getRendering(renderings[2 * index], renderings[2 * index + 1]));
        }
    }

    private String getRendering(String english, String simplified) {
        return switch (language) {
            case EN_US -> english;
            case ZH_CN -> simplified;
            case ZH_TW -> TraditionalGlyphs.convert(simplified);
        };
    }

    private static String getLanguageKey(String type, ResourceKey<?> key) {
        return key.location().toLanguageKey(type);
    }
}
