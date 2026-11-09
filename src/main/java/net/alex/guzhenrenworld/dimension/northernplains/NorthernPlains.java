package net.alex.guzhenrenworld.dimension.northernplains;

import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * The Northern Plains [北原] dimension, one of the four regions beyond the Central Continent [中州], which is the
 * overworld: its {@link #REGION}. The region is a cold grassland, flat and open for the most part, grass in the south
 * and snow in the north, with a few frozen peaks; its waterways run over a gravel bed.
 *
 * <p>The region is finite and its keys, borders and datapack entries are the shared ones ({@link Region}); the
 * terrain hooks are {@link NorthernPlainsTerrain}'s and the biome table {@link NorthernPlainsLayout}'s.
 *
 * @author Alex
 * @version 1.0.0
 * @see NorthernPlainsTerrain
 * @since 1.0.0
 */

public final class NorthernPlains {

    private static final String NAME = "northern_plains";
    public static final Region REGION = new Region(NAME, new Region.Title("Northern Plains", "北原"), 63, false,
            Waterways.Bed.GRAVEL,
            new Region.Biomes(NorthernPlainsRoster.REPLICAS, NorthernPlainsRoster.PORTED, NorthernPlainsLayout.ROSTER,
                    NorthernPlainsLayout.LAND, NorthernPlainsRoster.NAMES, NorthernPlainsRoster.CROPS,
                    NorthernPlainsLayout.TABLE::getParameters),
            NorthernPlainsTerrain.HOOKS);

    private NorthernPlains() {}
}
