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
 * 本模组英、简、繁三语翻译表的统一数据生成器。
 * <ul>
 *   <li>每种 {@link Language} 各自实例化并运行一次，共享同一套词条注册逻辑以保证三语键集合严格一致；繁体中文由简体中文
 *       经{@link TraditionalGlyphs#convert} 自动转换字形。</li>
 *   <li>所有翻译键均通过 {@link #getLanguageKey} 由对应的 {@link ResourceKey} 动态派生，不使用硬编码字符串，避免资源改
 *       名后残留悬空翻译键。</li>
 *   <li>{@link #addDimension} 同时写出原版维度名与 Traveler's Titles 标题，{@link #addTitleColor} 按该模组约定写出不带
 *       {@code #} 的 6 位小写十六进制颜色值；{@link #addBiomes} 按顺序或按各域 {@link BiomeName} 列表写出群系译名，宝黄
 *       天（{@link TreasureYellowHeaven}）的唯一群系直接沿用维度名称。</li>
 *   <li>⚠ 英文与简体中文均为手写权威定稿，不互为机翻；若 {@link #addBiomes} 传入的译名对数与群系数量不匹配，或简体文本
 *       包含{@link TraditionalGlyphs} 未收录的新汉字，都会在 {@code runData} 阶段直接抛错失败。</li>
 * </ul>
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
