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
 * The biomes only the Eastern Sea [东海] places, which the datagen providers collect from here: {@link #PORTED},
 * the Terralith biomes the region ported ({@link Ports#biome} names each; the port's files are listed in
 * {@code src/ports/terralith/index.eastern_sea.json}), {@link #REPLICAS}, the vanilla biomes it replicates beside
 * {@link Replicas#SHARED}, {@link #NAMES}, the display names of both, and {@link #CROPS}, the land sorted into
 * the wild crop climates of Farmer's Delight ({@link WildCropClimate}). {@link EasternSeaLayout} places them and
 * derives the roster and the land list from its table.
 *
 * <p>The ports keep Terralith's names but one: Terralith's alpha islands are the isles. The region also places the
 * Southern Border's [南疆] rainforest ({@link Ports#RAINFOREST}, Terralith's tropical jungle), which that region
 * registers and names, so it is listed here only among the crop climates.
 *
 * <p>The crop climates follow each biome's own temperature and downfall ({@link WildCropClimate}): the volcanoes
 * (temperature 1, dry) hot; the jungles wet, the isles (0.7, downfall 0.8) wet and temperate; the shores beach, the
 * beach and the gravel beach (0.8) also temperate, the stony shore (0.2) also cold; the basalt and white cliffs
 * (0.5 and 0.4) temperate. The oceans sit in none.
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
