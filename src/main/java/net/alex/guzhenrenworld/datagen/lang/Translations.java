package net.alex.guzhenrenworld.datagen.lang;

import java.util.List;
import java.util.Locale;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.BiomeName;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.Regions;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandBiomes;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandFoxImmortal;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.southernborder.BiomeSettings;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * 本模组简、繁中文翻译表的统一数据生成器；游戏内文本只有中文，英文只留在翻译键里。
 * <ul>
 *   <li>每种 {@link Language} 各自实例化并运行一次，共享同一套词条注册逻辑以保证各语言键集合严格一致：{@code en_us} 与
 *       {@code zh_cn} 写出同一份简体文本（原版只回退到 {@code en_us}，非中文客户端由此显示简体），{@code zh_tw} 与
 *       {@code zh_hk} 写出简体经 {@link TraditionalGlyphs#convert} 自动转换字形的繁体文本。</li>
 *   <li>所有翻译键均通过 {@link #getLanguageKey} 由对应的 {@link ResourceKey} 动态派生，不使用硬编码字符串，避免资源改
 *       名后残留悬空翻译键。</li>
 *   <li>{@link #addDimensionName} 同时写出原版维度名与 Traveler's Titles 标题，{@link #addTitleColor} 按该模组约定写出
 *       不带 {@code #} 的 6 位小写十六进制颜色值；{@link #addBiomes} 按顺序或按各域 {@link BiomeName} 列表写出群系译
 *       名，宝黄天（{@link TreasureYellowHeaven}）的唯一群系直接沿用维度名称。</li>
 *   <li>⚠ 简体中文为手写权威定稿；若 {@link #addBiomes} 传入的译名数与群系数量不匹配，或简体文本包含
 *       {@link TraditionalGlyphs} 未收录的新汉字，都会在 {@code runData} 阶段直接抛错失败。</li>
 * </ul>
 */

public class Translations extends LanguageProvider {

    private final Language language;

    public enum Language { EN_US, ZH_CN, ZH_TW, ZH_HK }

    public Translations(PackOutput packOutput, Language language) {
        super(packOutput, GuWorld.MOD_ID, language.name().toLowerCase(Locale.ROOT));
        this.language = language;
    }

    @Override
    protected void addTranslations() {
        addDimensionName(TreasureYellowHeaven.LEVEL_STEM, "宝黄天");
        addTitleColor(TreasureYellowHeaven.LEVEL_STEM, TreasureYellowHeaven.COLOR);
        addBiomes(List.of(TreasureYellowHeaven.BIOME), "宝黄天");
        addDimensionName(BlissfulLandFoxImmortal.LEVEL_STEM, BlissfulLandFoxImmortal.TITLE);
        addTitleColor(BlissfulLandFoxImmortal.LEVEL_STEM, BlissfulLandFoxImmortal.COLOR);
        addBiomes(Replicas.BIOMES,
                "平原",
                "河流",
                "繁茂洞穴",
                "溶洞",
                "雪林",
                "冰封山峰");
        addBiomes(BlissfulLandBiomes.NAMES);
        addBiomes(Ports.BIOMES,
                "高原",
                "林间高原",
                "云雾林",
                "雾山",
                "雨林",
                "热海",
                "兰花沼泽",
                "桃林",
                "裸岩峰",
                "翠峰",
                "丛林山",
                "彩岩山",
                "花岗岩崖",
                "高山草甸",
                "嶂石岩",
                "喀斯特峰林");
        addBiomes(List.of(BiomeSettings.BAMBOO_SEA), "竹海");
        for (Region region : Regions.ALL) {
            addDimensionName(region.levelStem(), region.title());
            addBiomes(region.biomes().names());
        }
    }

    private void addBiomes(List<BiomeName> names) {
        for (BiomeName name : names) {
            add(getLanguageKey("biome", name.biome()), getRendering(name.simplified()));
        }
    }

    private void addDimensionName(ResourceKey<LevelStem> dimension, String simplified) {
        String name = getRendering(simplified);
        add(getLanguageKey("dimension", dimension), name);
        add(getLanguageKey("travelerstitles", dimension), name);
    }

    private void addTitleColor(ResourceKey<LevelStem> dimension, int color) {
        add(getLanguageKey("travelerstitles", dimension) + ".color", "%06x".formatted(color));
    }

    private void addBiomes(List<ResourceKey<Biome>> biomes, String... simplified) {
        if (simplified.length != biomes.size()) {
            throw new IllegalArgumentException(simplified.length + " names for the " + biomes.size()
                    + " biomes " + biomes + ": each biome takes one Simplified name");
        }
        for (int index = 0; index < biomes.size(); index++) {
            add(getLanguageKey("biome", biomes.get(index)), getRendering(simplified[index]));
        }
    }

    private String getRendering(String simplified) {
        return switch (language) {
            case EN_US, ZH_CN -> simplified;
            case ZH_TW, ZH_HK -> TraditionalGlyphs.convert(simplified);
        };
    }

    private static String getLanguageKey(String type, ResourceKey<?> key) {
        return key.location().toLanguageKey(type);
    }
}
