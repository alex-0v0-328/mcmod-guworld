package net.alex.guzhenrenworld.datagen.lang;

import net.minecraft.data.PackOutput;

/**
 * The Traditional Chinese [繁中] strings of this mod, derived from the Simplified ones.
 *
 * <p>Extends {@link ZhCnLanguageProvider} for {@code zh_tw}. The two differ in glyphs only, so this
 * class adds no entry of its own: it overrides {@link #add} and passes every value through
 * {@link TraditionalGlyphs#convert}. Non-Han values such as the Traveler's Titles color pass through
 * untouched.
 *
 * <p>⚠ Never add a {@code zh_tw}-only string here; a wrong glyph is fixed in the glyph table.
 *
 * @author Alex
 * @version 1.0.0
 * @see TraditionalGlyphs
 * @since 1.0.0
 */

public class ZhTwLanguageProvider extends ZhCnLanguageProvider {

    public ZhTwLanguageProvider(PackOutput output) {
        super(output, "zh_tw");
    }

    @Override
    public void add(String key, String value) {
        super.add(key, TraditionalGlyphs.convert(value));
    }
}
