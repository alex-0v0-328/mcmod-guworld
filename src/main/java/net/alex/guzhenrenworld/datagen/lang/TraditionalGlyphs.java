package net.alex.guzhenrenworld.datagen.lang;

import java.util.HashMap;
import java.util.Map;

/**
 * The Simplified-to-Traditional glyph table behind this mod's {@code zh_tw}.
 *
 * <p>Traditional Chinese [繁中] differs from {@code zh_cn} in glyphs only, never in wording, so
 * {@link ZhTwLanguageProvider} derives every value through {@link #convert}. The glyphs follow the
 * Taiwan standard (OpenCC {@code s2tw}); vocabulary stays as written.
 *
 * <p>This is a copy of Guzhenren's {@code TraditionalGlyphs}, cut down to the glyphs this mod uses:
 * the two mods never compile against each other, so neither can borrow the other's table. A glyph
 * added here takes the Traditional form Guzhenren's table already gives it.
 *
 * <p>A token in {@link #PAIRS} is its Simplified half followed by its Traditional half (宝寶). A
 * glyph with more than one Traditional form goes in as a whole phrase token (冲刷沖刷) and never on
 * its own; {@link #convert} takes the longest token that matches at each position. {@link #UNCHANGED}
 * holds the glyphs both scripts share.
 *
 * <p>⚠ {@link #convert} throws on a Han glyph it cannot place, so a new {@code zh_cn} string with an
 * unseen glyph fails {@code runData} instead of shipping a Simplified glyph inside the Traditional
 * file.
 *
 * @author Alex
 * @version 1.0.0
 * @see ZhTwLanguageProvider
 * @since 1.0.0
 */

public final class TraditionalGlyphs {

    private static final String PAIRS = "宝寶 黄黃";
    private static final String UNCHANGED = "天";
    private static final Map<String, String> TOKENS = tokens();
    private static final int LONGEST = TOKENS.keySet().stream().mapToInt(String::length).max().orElse(1);

    private TraditionalGlyphs() {}

    public static String convert(String simplified) {
        StringBuilder traditional = new StringBuilder(simplified.length());
        int index = 0;
        while (index < simplified.length()) {
            int length = matchLength(simplified, index);
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

    private static int matchLength(String text, int start) {
        for (int length = Math.min(LONGEST, text.length() - start); length > 0; length--) {
            if (TOKENS.containsKey(text.substring(start, start + length))) return length;
        }
        return 0;
    }

    private static Map<String, String> tokens() {
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
