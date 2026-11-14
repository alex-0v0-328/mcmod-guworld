package net.alex.guzhenrenworld.dimension.easternsea;

import java.util.List;
import java.util.Map;
import net.alex.guzhenrenworld.biome.BiomeName;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.biome.WildCropClimate;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * 只有东海放置的群系，供数据生成 provider 收集。
 *
 * <ul>
 *   <li>{@link #PORTED}：移植的 Terralith 群系，键由 {@link Ports#biome} 生成；{@link #REPLICAS}：
 *       {@link Replicas#SHARED} 外复刻的原版群系；{@link #NAMES}：二者显示名；{@link #CROPS}：陆地按农夫乐事野生作物气
 *       候（{@link WildCropClimate}）分；移植文件列在 {@code src/ports/terralith/index.eastern_sea.json}。</li>
 *   <li>{@link EasternSeaLayout} 摆放它们，据表推出名单与陆地表；移植沿用 Terralith 名，唯 alpha 群岛改名岛屿。</li>
 *   <li>也放南疆雨林（{@link Ports#RAINFOREST}，即 Terralith 热带丛林），由南疆注册命名，此处只列在作物气候里。</li>
 *   <li>作物气候按群系自身温度与降水：火山（温度 1，干）热；丛林类湿；岛屿与蜃岛（0.7，降水 0.8）湿兼温带；海岸类滩，
 *       沙滩与砾滩（0.8）兼温带，石岸（0.2）兼寒；玄武岩崖与白崖（0.5、0.4）温带；海洋不归任何气候。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see EasternSeaLayout
 * @since 1.0.0
 */

public final class EasternSeaRoster {

    public static final ResourceKey<Biome> WARM_OCEAN = Replicas.keyOf(Biomes.WARM_OCEAN);
    public static final ResourceKey<Biome> LUKEWARM_OCEAN = Replicas.keyOf(Biomes.LUKEWARM_OCEAN);
    public static final ResourceKey<Biome> OCEAN = Replicas.keyOf(Biomes.OCEAN);
    public static final ResourceKey<Biome> DEEP_OCEAN = Replicas.keyOf(Biomes.DEEP_OCEAN);
    public static final ResourceKey<Biome> BEACH = Replicas.keyOf(Biomes.BEACH);
    public static final ResourceKey<Biome> STONY_SHORE = Replicas.keyOf(Biomes.STONY_SHORE);
    public static final ResourceKey<Biome> JUNGLE = Replicas.keyOf(Biomes.JUNGLE);
    public static final ResourceKey<Biome> ISLES = Ports.biome("isles");
    public static final ResourceKey<Biome> MIRAGE_ISLES = Ports.biome("mirage_isles");
    public static final ResourceKey<Biome> VOLCANIC_PEAKS = Ports.biome("volcanic_peaks");
    public static final ResourceKey<Biome> VOLCANIC_CRATER = Ports.biome("volcanic_crater");
    public static final ResourceKey<Biome> BASALT_CLIFFS = Ports.biome("basalt_cliffs");
    public static final ResourceKey<Biome> WHITE_CLIFFS = Ports.biome("white_cliffs");
    public static final ResourceKey<Biome> DEEP_WARM_OCEAN = Ports.biome("deep_warm_ocean");
    public static final ResourceKey<Biome> GRAVEL_BEACH = Ports.biome("gravel_beach");
    public static final List<ResourceKey<Biome>> PORTED = List.of(ISLES, MIRAGE_ISLES, VOLCANIC_PEAKS,
            VOLCANIC_CRATER, BASALT_CLIFFS, WHITE_CLIFFS, DEEP_WARM_OCEAN, GRAVEL_BEACH);
    public static final List<Replicas.Template> REPLICAS = List.of(
            Replicas.of(Biomes.WARM_OCEAN, OverworldBiomes::warmOcean),
            Replicas.of(Biomes.LUKEWARM_OCEAN, (placed, carvers) -> OverworldBiomes.lukeWarmOcean(placed, carvers,
                    false)),
            Replicas.of(Biomes.OCEAN, (placed, carvers) -> OverworldBiomes.ocean(placed, carvers, false)),
            Replicas.of(Biomes.DEEP_OCEAN, (placed, carvers) -> OverworldBiomes.ocean(placed, carvers, true)),
            Replicas.of(Biomes.BEACH, (placed, carvers) -> OverworldBiomes.beach(placed, carvers, false, false)),
            Replicas.of(Biomes.STONY_SHORE, (placed, carvers) -> OverworldBiomes.beach(placed, carvers, false, true)),
            Replicas.of(Biomes.JUNGLE, OverworldBiomes::jungle));
    public static final List<BiomeName> NAMES = List.of(
            new BiomeName(WARM_OCEAN, "Warm Ocean", "暖水海洋"),
            new BiomeName(LUKEWARM_OCEAN, "Lukewarm Ocean", "温水海洋"),
            new BiomeName(OCEAN, "Ocean", "海洋"),
            new BiomeName(DEEP_OCEAN, "Deep Ocean", "深海"),
            new BiomeName(BEACH, "Beach", "沙滩"),
            new BiomeName(STONY_SHORE, "Stony Shore", "石岸"),
            new BiomeName(JUNGLE, "Jungle", "丛林"),
            new BiomeName(ISLES, "Isles", "岛屿"),
            new BiomeName(MIRAGE_ISLES, "Mirage Isles", "蜃岛"),
            new BiomeName(VOLCANIC_PEAKS, "Volcanic Peaks", "火山峰"),
            new BiomeName(VOLCANIC_CRATER, "Volcanic Crater", "火山口"),
            new BiomeName(BASALT_CLIFFS, "Basalt Cliffs", "玄武岩崖"),
            new BiomeName(WHITE_CLIFFS, "White Cliffs", "白崖"),
            new BiomeName(DEEP_WARM_OCEAN, "Deep Warm Ocean", "深暖海"),
            new BiomeName(GRAVEL_BEACH, "Gravel Beach", "砾滩"));
    public static final Map<WildCropClimate, List<ResourceKey<Biome>>> CROPS = Map.of(
            WildCropClimate.HOT, List.of(VOLCANIC_PEAKS, VOLCANIC_CRATER),
            WildCropClimate.WET, List.of(JUNGLE, Ports.RAINFOREST, ISLES, MIRAGE_ISLES),
            WildCropClimate.BEACH, List.of(BEACH, STONY_SHORE, GRAVEL_BEACH),
            WildCropClimate.TEMPERATE, List.of(BEACH, GRAVEL_BEACH, ISLES, MIRAGE_ISLES, BASALT_CLIFFS, WHITE_CLIFFS),
            WildCropClimate.COLD, List.of(STONY_SHORE));

    private EasternSeaRoster() {}
}
