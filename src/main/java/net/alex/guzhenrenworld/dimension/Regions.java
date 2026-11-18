package net.alex.guzhenrenworld.dimension;

import java.util.List;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSea;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlains;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesert;

/**
 * 全部四个域（{@link Region}）的有序汇总表，也是全仓唯一直接引用各域子包入口的聚合点。{@code datagen.DatapackProvider}
 * 遍历{@link #ALL} 执行 {@link RegionBootstraps} 注册，群系标签与语言表生成器从中提取群系名单与译名，
 * {@link RegionBorders} 据此施加世界边界；未来扩展新域时仅需新建域包并在此列表追加一项。
 */

public final class Regions {

    public static final List<Region> ALL = List.of(SouthernBorder.REGION, WesternDesert.REGION, EasternSea.REGION,
            NorthernPlains.REGION);

    private Regions() {}
}
