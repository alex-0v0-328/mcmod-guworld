package net.alex.guzhenrenworld.dimension.easternsea;

import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * The Eastern Sea [东海] dimension, one of the four regions beyond the Central Continent [中州], which is the
 * overworld: its {@link #REGION}. The region is a warm archipelago, about four fifths water, islands from two
 * kilometers across down to a few dozen blocks; its waterways run over a sand bed. {@code /guworld enter
 * guworld:eastern_sea} lands players on the surface above 0, 0, the sea when the origin is water.
 *
 * <p>The region is finite and its keys, borders and datapack entries are the shared ones ({@link Region}); the
 * terrain hooks are {@link EasternSeaTerrain}'s and the biome table {@link EasternSeaLayout}'s.
 *
 * @author Alex
 * @version 1.0.0
 * @see EasternSeaTerrain
 * @since 1.0.0
 */

public final class EasternSea {

    private static final String NAME = "eastern_sea";
    public static final Region REGION = new Region(NAME, new Region.Title("Eastern Sea", "东海"), 63, false,
            Waterways.Bed.SAND,
            new Region.Biomes(EasternSeaRoster.REPLICAS, EasternSeaRoster.PORTED, EasternSeaLayout.ROSTER,
                    EasternSeaLayout.LAND, EasternSeaRoster.NAMES, EasternSeaRoster.CROPS,
                    EasternSeaLayout.TABLE::getParameters),
            EasternSeaTerrain.HOOKS);

    private EasternSea() {}
}
