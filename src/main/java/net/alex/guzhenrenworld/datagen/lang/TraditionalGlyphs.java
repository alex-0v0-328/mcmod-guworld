package net.alex.guzhenrenworld.datagen.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * 本模组繁体中文（{@code zh_tw}）的简繁字形转换表。
 * <ul>
 *   <li>繁体中文与简体中文（{@code zh_cn}）保持相同词汇，仅按台湾正体字标准（OpenCC {@code s2tw}）转换字形，由
 *       {@link Translations} 对每条简体文本调用 {@link #convert} 生成；因本模组与主模组无编译期依赖，此处仅维护本模组用
 *       到的字形子集（新增字优先与主模组同名类对齐，未收录字参考 OpenCC {@code s2tw}）。</li>
 *   <li>{@link #PAIRS} 中的词元按「前一半简体、后一半繁体」成对编码（如 {@code 宝寶}），一对多的多音多义字统一按整词录
 *       入（如{@code 干草乾草}），由 {@link #convert} 从左到右按最长前缀匹配替换；简繁同形字收录在 {@link #UNCHANGED}
 *       中。</li>
 *   <li>⚠ {@link #convert} 遇到既不在 {@link #PAIRS} 也不在 {@link #UNCHANGED} 中的汉字会直接抛出异常，使新增简体字在
 *       {@code runData} 阶段显式报错，防止简体字漏入繁体语言表。</li>
 * </ul>
 */

public final class TraditionalGlyphs {

    private static final String PAIRS = "丛叢 东東 云雲 亚亞 兰蘭 冻凍 台地臺地 叶葉 宝寶 屿嶼 岗崗 岛島 岩谷岩谷 峡峽 峡谷峽谷 干草乾草 恶惡"
            + " 桦樺 泽澤 温溫 滩灘 热熱 砾礫 积積 红紅 绿綠 荆荊 蚀蝕 袭襲 裂谷裂谷 针針 间間 雾霧 风風 黄黃";
    private static final String UNCHANGED = "丘之仙伯低冰利刺北南原口古喀地壁天寒封山岩岸峰崖嶂川平彩戈斯旱暖木林"
            + "柱桃森武水沙河沼洋洞洲流海深溶漠灌火特狐玄甸疆疏白盾石砂福穴竹繁翠花茂草蜃裸西陵雨雪高黑";
    private static final Map<String, String> TOKENS = parseTokens();
    private static final int LONGEST = TOKENS.keySet().stream().mapToInt(String::length).max().orElse(1);

    private TraditionalGlyphs() {}

    public static String convert(String simplified) {
        StringBuilder traditional = new StringBuilder(simplified.length());
        int index = 0;
        while (index < simplified.length()) {
            int length = getMatchLength(simplified, index);
            if (length > 0) {
                traditional.append(TOKENS.get(simplified.substring(index, index + length)));
                index += length;
                continue;
            }

            char glyph = simplified.charAt(index);
            if (Character.UnicodeScript.of(glyph) == Character.UnicodeScript.HAN && UNCHANGED.indexOf(glyph) < 0) {
                throw new IllegalArgumentException("no Traditional glyph for '" + glyph + "' in \"" + simplified
                        + "\": add it to PAIRS or UNCHANGED, or to a phrase if it has more than one Traditional form");
            }
            traditional.append(glyph);
            index++;
        }
        return traditional.toString();
    }

    private static int getMatchLength(String simplified, int start) {
        for (int length = Math.min(LONGEST, simplified.length() - start); length > 0; length--) {
            if (TOKENS.containsKey(simplified.substring(start, start + length))) return length;
        }
        return 0;
    }

    private static Map<String, String> parseTokens() {
        Map<String, String> tokens = new HashMap<>();
        for (String token : PAIRS.split(" ")) {
            int half = token.length() / 2;
            if (token.length() % 2 != 0 || tokens.put(token.substring(0, half), token.substring(half)) != null) {
                throw new IllegalStateException("glyph token is odd or repeated: " + token);
            }
        }
        for (char glyph : UNCHANGED.toCharArray()) {
            if (tokens.containsKey(String.valueOf(glyph))) {
                throw new IllegalStateException("glyph is both mapped and unchanged: " + glyph);
            }
        }
        return Map.copyOf(tokens);
    }
}
