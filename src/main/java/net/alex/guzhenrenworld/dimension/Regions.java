package net.alex.guzhenrenworld.dimension;

import java.util.List;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSea;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlains;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesert;

/**
 * The four {@link Region}s in the order their entries register, the only place that names every region package:
 * {@code datagen.DatapackProvider} runs {@link RegionBootstraps} over them, the biome tag and language providers read
 * their biome lists and names, and {@link RegionBorders} applies their border. A fifth region is one more line here
 * and one new package.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class Regions {

    public static final List<Region> ALL = List.of(SouthernBorder.REGION, WesternDesert.REGION, EasternSea.REGION,
            NorthernPlains.REGION);

    private Regions() {}
}
