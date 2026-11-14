package net.alex.guzhenrenworld.datagen.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * 本模组 {@code zh_tw} 的简转繁字形表。
 *
 * <ul>
 *   <li>繁中与 {@code zh_cn} 只差字形不差用词，{@link Translations} 每个 {@code zh_tw} 值都经 {@link #convert} 得出；
 *       字形按台湾标准（OpenCC {@code s2tw}），用词照原文。主模组 {@code TraditionalGlyphs} 的副本，只留本模组用字：
 *       两模组互不编译依赖，借不到对方的表；新字取主模组表的繁体，表中没有则取 OpenCC {@code s2tw}。</li>
 *   <li>{@link #PAIRS} 词元为简体半段接繁体半段（宝寶）；一简多繁的字只以整词录入（冲刷沖刷）；{@link #convert}
 *       逐位取最长匹配；{@link #UNCHANGED} 收简繁同形字。</li>
 *   <li>⚠ {@link #convert} 遇未收录汉字即抛错：{@code zh_cn} 含新字则 {@code runData} 失败，不把简体字混入繁体。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Translations
 * @since 1.0.0
 */

public final class TraditionalGlyphs {

    private static final String PAIRS = "丛叢 东東 云雲 亚亞 兰蘭 冻凍 台地臺地 叶葉 宝寶 屿嶼 岗崗 岛島 岩谷岩谷 峡峽 峡谷峽谷 干草乾草 恶惡"
            + " 桦樺 泽澤 温溫 滩灘 热熱 砾礫 积積 红紅 绿綠 荆荊 蚀蝕 袭襲 裂谷裂谷 针針 间間 雾霧 风風 黄黃";
    private static final String UNCHANGED = "丘之伯低冰利刺北南原口古喀地壁天寒封山岩岸峰崖嶂川平彩戈斯旱暖木林柱桃森武水沙"
            + "河沼洋洞洲流海深溶漠灌火特玄甸疆疏白盾石砂穴竹繁翠花茂草蜃裸西陵雨雪高黑";
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
