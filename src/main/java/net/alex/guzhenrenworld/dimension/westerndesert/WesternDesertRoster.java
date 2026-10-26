package net.alex.guzhenrenworld.dimension.westerndesert;

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
 * The biomes only the Western Desert [西漠] places, which the datagen providers collect from here: {@link #PORTED},
 * the Terralith biomes the region ported ({@link Ports#biome} names each; the port's files are listed in
 * {@code src/ports/terralith/index.western_desert.json}), {@link #REPLICAS}, the vanilla biomes it replicates beside
 * {@link Replicas#SHARED}, {@link #NAMES}, the display names of both, and {@link #CROPS}, the land sorted into
 * the wild crop climates of Farmer's Delight ({@link WildCropClimate}). {@link WesternDesertLayout} places them and
 * derives the roster and the land list from its table.
 *
 * <p>The climates follow each biome's own temperature and downfall ({@link WildCropClimate}): the deserts, badlands,
 * canyons, mesas and the dry highlands and brushland, all temperature 1 and up and dry, hot, the two oases also wet by
 * name, the hot shrubland (0.8) temperate, and the gravel desert (0.14) cold.
 *
 * <p>⚠ A re-port that adds a biome gives it its key in {@link #PORTED}, its name in {@link #NAMES} and its climates
 * in {@link #CROPS} in the same change; {@link Ports#registerBiomes} fails while the index and the keys differ.
 *
 * @author Alex
 * @version 1.0.0
 * @see WesternDesertLayout
 * @since 1.0.0
 */

public final class WesternDesertRoster {

    public static final ResourceKey<Biome> ANCIENT_SANDS = Ports.biome("ancient_sands");
    public static final ResourceKey<Biome> DESERT_CANYON = Ports.biome("desert_canyon");
    public static final ResourceKey<Biome> DESERT_OASIS = Ports.biome("desert_oasis");
    public static final ResourceKey<Biome> RED_OASIS = Ports.biome("red_oasis");
    public static final ResourceKey<Biome> DESERT_SPIRES = Ports.biome("desert_spires");
    public static final ResourceKey<Biome> GRAVEL_DESERT = Ports.biome("gravel_desert");
    public static final ResourceKey<Biome> LUSH_DESERT = Ports.biome("lush_desert");
    public static final ResourceKey<Biome> SANDSTONE_VALLEY = Ports.biome("sandstone_valley");
    public static final ResourceKey<Biome> ARID_HIGHLANDS = Ports.biome("arid_highlands");
    public static final ResourceKey<Biome> WHITE_MESA = Ports.biome("white_mesa");
    public static final ResourceKey<Biome> BRYCE_CANYON = Ports.biome("bryce_canyon");
    public static final ResourceKey<Biome> HOT_SHRUBLAND = Ports.biome("hot_shrubland");
    public static final ResourceKey<Biome> BRUSHLAND = Ports.biome("brushland");
    public static final ResourceKey<Biome> DESERT = Replicas.keyOf(Biomes.DESERT);
    public static final ResourceKey<Biome> BADLANDS = Replicas.keyOf(Biomes.BADLANDS);
    public static final ResourceKey<Biome> ERODED_BADLANDS = Replicas.keyOf(Biomes.ERODED_BADLANDS);
    public static final ResourceKey<Biome> WOODED_BADLANDS = Replicas.keyOf(Biomes.WOODED_BADLANDS);
    public static final List<ResourceKey<Biome>> PORTED = List.of(ANCIENT_SANDS, DESERT_CANYON, DESERT_OASIS, RED_OASIS,
            DESERT_SPIRES, GRAVEL_DESERT, LUSH_DESERT, SANDSTONE_VALLEY, ARID_HIGHLANDS, WHITE_MESA, BRYCE_CANYON,
            HOT_SHRUBLAND, BRUSHLAND);
    public static final List<Replicas.Template> REPLICAS = List.of(
            Replicas.of(Biomes.DESERT, OverworldBiomes::desert),
            Replicas.of(Biomes.BADLANDS, (placed, carvers) -> OverworldBiomes.badlands(placed, carvers, false)),
            Replicas.of(Biomes.ERODED_BADLANDS, (placed, carvers) -> OverworldBiomes.badlands(placed, carvers, false)),
            Replicas.of(Biomes.WOODED_BADLANDS, (placed, carvers) -> OverworldBiomes.badlands(placed, carvers, true)));
    public static final List<BiomeName> NAMES = List.of(
            new BiomeName(ANCIENT_SANDS, "Ancient Sands", "古沙海"),
            new BiomeName(DESERT_CANYON, "Desert Canyon", "沙漠峡谷"),
            new BiomeName(DESERT_OASIS, "Desert Oasis", "绿洲"),
            new BiomeName(RED_OASIS, "Red Oasis", "红绿洲"),
            new BiomeName(DESERT_SPIRES, "Desert Spires", "沙漠石柱"),
            new BiomeName(GRAVEL_DESERT, "Gravel Desert", "戈壁"),
            new BiomeName(LUSH_DESERT, "Lush Desert", "疏林沙漠"),
            new BiomeName(SANDSTONE_VALLEY, "Sandstone Valley", "砂岩谷"),
            new BiomeName(ARID_HIGHLANDS, "Arid Highlands", "旱原高地"),
            new BiomeName(WHITE_MESA, "White Mesa", "白台地"),
            new BiomeName(BRYCE_CANYON, "Bryce Canyon", "石林峡谷"),
            new BiomeName(HOT_SHRUBLAND, "Hot Shrubland", "热灌丛"),
            new BiomeName(BRUSHLAND, "Brushland", "荆丛地"),
            new BiomeName(DESERT, "Desert", "沙漠"),
            new BiomeName(BADLANDS, "Badlands", "恶地"),
            new BiomeName(ERODED_BADLANDS, "Eroded Badlands", "风蚀恶地"),
            new BiomeName(WOODED_BADLANDS, "Wooded Badlands", "疏林恶地"));
    public static final Map<WildCropClimate, List<ResourceKey<Biome>>> CROPS = Map.of(
            WildCropClimate.HOT, List.of(ANCIENT_SANDS, DESERT_CANYON, DESERT_OASIS, RED_OASIS, DESERT_SPIRES,
                    LUSH_DESERT, SANDSTONE_VALLEY, ARID_HIGHLANDS, WHITE_MESA, BRYCE_CANYON, BRUSHLAND, DESERT,
                    BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS),
            WildCropClimate.WET, List.of(DESERT_OASIS, RED_OASIS),
            WildCropClimate.BEACH, List.of(),
            WildCropClimate.TEMPERATE, List.of(HOT_SHRUBLAND),
            WildCropClimate.COLD, List.of(GRAVEL_DESERT));

    private WesternDesertRoster() {}
}
