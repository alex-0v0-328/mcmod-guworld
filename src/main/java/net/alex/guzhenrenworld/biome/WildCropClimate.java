package net.alex.guzhenrenworld.biome;

import java.util.Locale;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Where Farmer's Delight's wild crops grow in Gu World's regions. That mod places its wild crop patches through its
 * own biome modifiers, which pick biomes by vanilla's and the convention tags ({@code #minecraft:is_overworld} with a
 * temperature window, {@code #minecraft:is_beach}, {@code #c:is_wet/overworld}, {@code #c:is_hot/overworld}). Gu
 * World's biomes join none of those: {@code is_overworld} is the biome list of vanilla's stronghold and
 * {@code is_beach} that of its buried treasure, and no vanilla structure generates in a region. Instead each region
 * sorts its biomes into these five climates, the tag provider writes one tag per climate,
 * {@code guworld:farmersdelight/<climate>}, and the hand-written modifiers under
 * {@code data/guworld/neoforge/biome_modifier/farmersdelight/}, loaded only while Farmer's Delight is present
 * ({@code neoforge:conditions}), add that mod's patches to the tag the way its own modifiers would:
 * <ul>
 *   <li>{@link #HOT}: tomatoes, hot and dry biomes;</li>
 *   <li>{@link #WET}: rice, wet biomes;</li>
 *   <li>{@link #BEACH}: cabbages and beetroots, shores;</li>
 *   <li>{@link #TEMPERATE}: carrots and onions, temperature 0.4 to 0.9;</li>
 *   <li>{@link #COLD}: potatoes, temperature 0.1 to 0.3.</li>
 * </ul>
 * One rule sorts every region's biomes, by the temperature and downfall in the biome's own JSON: temperate is
 * temperature 0.4 to 0.9; cold is 0.1 to 0.3; hot is temperature 1.0 and up with downfall under 0.5; wet is downfall
 * 0.8 and up, or a name with swamp, marsh, jungle, rainforest or oasis; beach is a name with beach or shore. Water
 * (oceans and rivers) and caves sit in none; a land biome may sit in several climates, or in none.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum WildCropClimate {
    HOT, WET, BEACH, TEMPERATE, COLD;

    private final TagKey<Biome> tag = TagKey.create(Registries.BIOME,
            GuWorld.id("farmersdelight/" + name().toLowerCase(Locale.ROOT)));

    public TagKey<Biome> tag() {
        return tag;
    }
}
