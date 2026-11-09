package net.alex.guzhenrenworld.dimension.westerndesert;

import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * The Western Desert [西漠] dimension, one of the four regions beyond the Central Continent [中州], which is the
 * overworld: its {@link #REGION}. The region is hot and dry: flat sand and dunes for the most part, mesas and canyons
 * for the rest, peaks up to about y 210 ({@link WesternDesertTerrain}), and little water.
 *
 * <p>The sea level is 11 blocks under the overworld's 63 and the terrain's, which Tectonic shapes from its own
 * splines whatever the level: the rivers it cuts at about y 56 and the low ground beside them stay dry beds, and
 * water stands only where an aquifer reaches the surface (about 2% of the columns) and in the oases, whose lakes are
 * Terralith's features and fill hollows at any height. Its waterways are dry gullies but for the widest, over a sand
 * bed.
 *
 * <p>The region is finite and its keys, borders and datapack entries are the shared ones ({@link Region}); the
 * terrain hooks are {@link WesternDesertTerrain}'s and the biome table {@link WesternDesertLayout}'s.
 *
 * @author Alex
 * @version 1.0.0
 * @see WesternDesertTerrain
 * @since 1.0.0
 */

public final class WesternDesert {

    private static final String NAME = "western_desert";
    public static final Region REGION = new Region(NAME, new Region.Title("Western Desert", "西漠"), 52, true,
            Waterways.Bed.SAND,
            new Region.Biomes(WesternDesertRoster.REPLICAS, WesternDesertRoster.PORTED, WesternDesertLayout.ROSTER,
                    WesternDesertLayout.LAND, WesternDesertRoster.NAMES, WesternDesertRoster.CROPS,
                    WesternDesertLayout.TABLE::getParameters),
            WesternDesertTerrain.HOOKS);

    private WesternDesert() {}
}
