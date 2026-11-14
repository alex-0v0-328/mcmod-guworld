package net.alex.guzhenrenworld.datagen.lang;

import java.util.List;
import java.util.Locale;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.BiomeName;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.Regions;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.southernborder.BiomeSettings;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * 本模组三语全部文本，注册键、英文、简中并排成表。
 *
 * <ul>
 *   <li>provider 每种 {@link Language} 跑一次、只写本栏，键必齐；繁中由简中经 {@link TraditionalGlyphs} 只转字形。</li>
 *   <li>键一律由所指注册键生成（{@link #getLanguageKey}：类型加注册 ID），不用裸字符串，改名不留指空的键。</li>
 *   <li>{@link #addDimension} 写维度名与 Traveler's Titles 标题；{@link #addTitleColor} 写标题色，
 *       按 {@code travelerstitles.*} 惯例为小写十六进制、无 {@code #}，各语言相同；{@link #addBiomes} 按序给群系配英
 *       文、简中名，或取域的 {@link BiomeName} 名单；宝黄天[Treasure Yellow Heaven]唯一群系沿用维度名。</li>
 *   <li>⚠ 两种写法都是权威，不互为翻译；{@link #addBiomes} 名字对数与群系数不符、{@link TraditionalGlyphs} 遇新字都抛
 *       错，漏名或新字令 {@code runData} 失败，不发裸键。</li>
 * </ul>
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
        addBiomes(Replicas.BIOMES,
                "Plains", "平原",
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
        for (Region region : Regions.ALL) {
            addDimension(region.levelStem(), region.title().english(), region.title().simplified());
            addBiomes(region.biomes().names());
        }
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
