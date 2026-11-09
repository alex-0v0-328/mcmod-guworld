package net.alex.guzhenrenworld.dimension.southernborder;

import java.util.List;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.waterways.Waterways;

/**
 * The Southern Border [南疆] dimension, the first of the four regions beyond the Central Continent [中州], which is
 * the overworld: its {@link #REGION}. Peaks reach about y 300 ({@link Terrain}), about 20 blocks under the build
 * limit; sea level 63, and its waterways run over gravel with clay patches. The region places the shared replicas
 * only, no roster of its own.
 *
 * <p>The region is finite and its keys, borders and datapack entries are the shared ones ({@link Region}); the
 * terrain hooks are {@link Terrain}'s and the biome table {@link BiomeLayout}'s.
 *
 * @author Alex
 * @version 1.0.0
 * @see Terrain
 * @since 1.0.0
 */

public final class SouthernBorder {

    private static final String NAME = "southern_border";
    public static final Region REGION = new Region(NAME, new Region.Title("Southern Border", "南疆"), 63, false,
            Waterways.Bed.GRAVEL_CLAY,
            new Region.Biomes(List.of(), List.of(), BiomeLayout.ROSTER, BiomeLayout.LAND, List.of(),
                    BiomeLayout.CROPS, BiomeLayout.TABLE::getParameters),
            Terrain.HOOKS);

    private SouthernBorder() {}
}
