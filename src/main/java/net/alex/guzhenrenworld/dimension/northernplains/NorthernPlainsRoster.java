package net.alex.guzhenrenworld.dimension.northernplains;

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
 * 只有北原放置的群系，供数据生成 provider 收集。
 *
 * <ul>
 *   <li>{@link #PORTED}：移植的 Terralith 群系，键由 {@link Ports#biome} 生成；{@link #REPLICAS}：
 *       {@link Replicas#SHARED} 外复刻的原版群系；{@link #NAMES}：二者显示名；{@link #CROPS}：陆地按农夫乐事野生作物气
 *       候（{@link WildCropClimate}）分；移植文件列在 {@code src/ports/terralith/index.northern_plains.json}。</li>
 *   <li>{@link NorthernPlainsLayout} 摆放它们，由群系表推出名单与陆地列表。</li>
 *   <li>气候按自身温度与降水：温带（0.4–0.45）干草原、地盾、高寒高地，地盾兼湿（降水 0.8）；寒（0.1–0.22）寒灌丛、
 *       西伯利亚针叶林、桦木针叶林、雪地盾、冰沼，西伯利亚针叶林、雪地盾、冰沼兼湿（降水 ≥ 0.8）；低于 0.1 不归任何气
 *       候：寒原低地、寒林、冰崖、冰川裂谷与复刻的雪原、冰刺之地、积雪针叶林、雪林、冰封山峰；冻河是水体。</li>
 *   <li>⚠ 重新移植多出的群系须在同一改动里补 {@link #PORTED} 键、{@link #NAMES} 名、{@link #CROPS} 气候；清单与键不符
 *       时 {@link Ports#registerBiomes} 失败。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see NorthernPlainsLayout
 * @since 1.0.0
 */

public final class NorthernPlainsRoster {

    public static final ResourceKey<Biome> STEPPE = Ports.biome("steppe");
    public static final ResourceKey<Biome> COLD_SHRUBLAND = Ports.biome("cold_shrubland");
    public static final ResourceKey<Biome> WINTRY_LOWLANDS = Ports.biome("wintry_lowlands");
    public static final ResourceKey<Biome> WINTRY_FOREST = Ports.biome("wintry_forest");
    public static final ResourceKey<Biome> SIBERIAN_TAIGA = Ports.biome("siberian_taiga");
    public static final ResourceKey<Biome> BIRCH_TAIGA = Ports.biome("birch_taiga");
    public static final ResourceKey<Biome> SHIELD = Ports.biome("shield");
    public static final ResourceKey<Biome> SNOWY_SHIELD = Ports.biome("snowy_shield");
    public static final ResourceKey<Biome> ICE_MARSH = Ports.biome("ice_marsh");
    public static final ResourceKey<Biome> FROZEN_CLIFFS = Ports.biome("frozen_cliffs");
    public static final ResourceKey<Biome> GLACIAL_CHASM = Ports.biome("glacial_chasm");
    public static final ResourceKey<Biome> ALPINE_HIGHLANDS = Ports.biome("alpine_highlands");
    public static final ResourceKey<Biome> SNOWY_PLAINS = Replicas.keyOf(Biomes.SNOWY_PLAINS);
    public static final ResourceKey<Biome> ICE_SPIKES = Replicas.keyOf(Biomes.ICE_SPIKES);
    public static final ResourceKey<Biome> SNOWY_TAIGA = Replicas.keyOf(Biomes.SNOWY_TAIGA);
    public static final ResourceKey<Biome> GROVE = Replicas.keyOf(Biomes.GROVE);
    public static final ResourceKey<Biome> FROZEN_PEAKS = Replicas.keyOf(Biomes.FROZEN_PEAKS);
    public static final ResourceKey<Biome> FROZEN_RIVER = Replicas.keyOf(Biomes.FROZEN_RIVER);
    public static final List<ResourceKey<Biome>> PORTED = List.of(STEPPE, COLD_SHRUBLAND, WINTRY_LOWLANDS,
            WINTRY_FOREST, SIBERIAN_TAIGA, BIRCH_TAIGA, SHIELD, SNOWY_SHIELD, ICE_MARSH, FROZEN_CLIFFS, GLACIAL_CHASM,
            ALPINE_HIGHLANDS);
    public static final List<Replicas.Template> REPLICAS = List.of(
            Replicas.of(Biomes.SNOWY_PLAINS,
                    (placed, carvers) -> OverworldBiomes.plains(placed, carvers, false, true, false)),
            Replicas.of(Biomes.ICE_SPIKES,
                    (placed, carvers) -> OverworldBiomes.plains(placed, carvers, false, true, true)),
            Replicas.of(Biomes.SNOWY_TAIGA, (placed, carvers) -> OverworldBiomes.taiga(placed, carvers, true)),
            Replicas.of(Biomes.GROVE, OverworldBiomes::grove),
            Replicas.of(Biomes.FROZEN_PEAKS, OverworldBiomes::frozenPeaks),
            Replicas.of(Biomes.FROZEN_RIVER, (placed, carvers) -> OverworldBiomes.river(placed, carvers, true)));
    public static final List<BiomeName> NAMES = List.of(
            new BiomeName(STEPPE, "Steppe", "干草原"),
            new BiomeName(COLD_SHRUBLAND, "Cold Shrubland", "寒灌丛"),
            new BiomeName(WINTRY_LOWLANDS, "Wintry Lowlands", "寒原低地"),
            new BiomeName(WINTRY_FOREST, "Wintry Forest", "寒林"),
            new BiomeName(SIBERIAN_TAIGA, "Siberian Taiga", "西伯利亚针叶林"),
            new BiomeName(BIRCH_TAIGA, "Birch Taiga", "桦木针叶林"),
            new BiomeName(SHIELD, "Shield", "地盾"),
            new BiomeName(SNOWY_SHIELD, "Snowy Shield", "雪地盾"),
            new BiomeName(ICE_MARSH, "Ice Marsh", "冰沼"),
            new BiomeName(FROZEN_CLIFFS, "Frozen Cliffs", "冰崖"),
            new BiomeName(GLACIAL_CHASM, "Glacial Chasm", "冰川裂谷"),
            new BiomeName(ALPINE_HIGHLANDS, "Alpine Highlands", "高寒高地"),
            new BiomeName(SNOWY_PLAINS, "Snowy Plains", "雪原"),
            new BiomeName(ICE_SPIKES, "Ice Spikes", "冰刺之地"),
            new BiomeName(SNOWY_TAIGA, "Snowy Taiga", "积雪针叶林"),
            new BiomeName(GROVE, "Grove", "雪林"),
            new BiomeName(FROZEN_PEAKS, "Frozen Peaks", "冰封山峰"),
            new BiomeName(FROZEN_RIVER, "Frozen River", "冻河"));
    public static final Map<WildCropClimate, List<ResourceKey<Biome>>> CROPS = Map.of(
            WildCropClimate.HOT, List.of(),
            WildCropClimate.WET, List.of(SIBERIAN_TAIGA, SHIELD, SNOWY_SHIELD, ICE_MARSH),
            WildCropClimate.BEACH, List.of(),
            WildCropClimate.TEMPERATE, List.of(STEPPE, SHIELD, ALPINE_HIGHLANDS),
            WildCropClimate.COLD, List.of(COLD_SHRUBLAND, SIBERIAN_TAIGA, BIRCH_TAIGA, SNOWY_SHIELD, ICE_MARSH));

    private NorthernPlainsRoster() {}
}
