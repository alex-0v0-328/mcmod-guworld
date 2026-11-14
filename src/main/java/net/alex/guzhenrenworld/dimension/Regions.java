package net.alex.guzhenrenworld.dimension;

import java.util.List;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSea;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlains;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesert;

/**
 * 全部四个域（{@link Region}），按入口注册顺序；唯一点名各域包之处。
 * {@code datagen.DatapackProvider} 对其跑 {@link RegionBootstraps}，群系标签与语言 provider 读其群系名单与名字，
 * {@link RegionBorders} 应用其边界；加第五域只需此处一行加一个包。
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
