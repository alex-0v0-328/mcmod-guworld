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
 * 北原维度独有的群系清单，供数据生成阶段的各 Provider 汇总读取。
 * <ul>
 *   <li>{@link #PORTED} 列出北原独有的 Terralith 移植群系键（对应清单
 *       {@code src/ports/terralith/index.northern_plains.json}）；{@link #REPLICAS} 列出除 {@link Replicas#SHARED} 外的
 *       原版复刻群系（雪林、冰封山峰与狐仙福地共用，放在 {@link Replicas#SHARED}）；{@link #NAMES} 汇总两者的显示名；{@link #CROPS} 将陆地群系按农夫乐事野生作物气候（
 *       {@link WildCropClimate}）分组。</li>
 *   <li>野生作物气候按各群系 JSON 的温度与降水量划分：干草原、地盾与高寒高地归温带（其中地盾兼入湿润）；寒灌丛、西伯利
 *       亚针叶林、桦木针叶林、雪地盾与冰沼归寒冷（其中西伯利亚针叶林、雪地盾与冰沼兼入湿润）；温度低于 0.1 的严寒群系及
 *       冻河水体不归入任何气候。</li>
 *   <li>⚠ 更新移植清单新增群系时，必须在同一次改动中同步补齐 {@link #PORTED}、{@link #NAMES} 与 {@link #CROPS}，否则
 *       {@link Ports#registerBiomes} 会在校验清单与键集合时直接报错。</li>
 * </ul>
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
    public static final ResourceKey<Biome> GROVE = Replicas.GROVE;
    public static final ResourceKey<Biome> FROZEN_PEAKS = Replicas.FROZEN_PEAKS;
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
            Replicas.of(Biomes.FROZEN_RIVER, (placed, carvers) -> OverworldBiomes.river(placed, carvers, true)));
    public static final List<BiomeName> NAMES = List.of(
            new BiomeName(STEPPE, "干草原"),
            new BiomeName(COLD_SHRUBLAND, "寒灌丛"),
            new BiomeName(WINTRY_LOWLANDS, "寒原低地"),
            new BiomeName(WINTRY_FOREST, "寒林"),
            new BiomeName(SIBERIAN_TAIGA, "西伯利亚针叶林"),
            new BiomeName(BIRCH_TAIGA, "桦木针叶林"),
            new BiomeName(SHIELD, "地盾"),
            new BiomeName(SNOWY_SHIELD, "雪地盾"),
            new BiomeName(ICE_MARSH, "冰沼"),
            new BiomeName(FROZEN_CLIFFS, "冰崖"),
            new BiomeName(GLACIAL_CHASM, "冰川裂谷"),
            new BiomeName(ALPINE_HIGHLANDS, "高寒高地"),
            new BiomeName(SNOWY_PLAINS, "雪原"),
            new BiomeName(ICE_SPIKES, "冰刺之地"),
            new BiomeName(SNOWY_TAIGA, "积雪针叶林"),
            new BiomeName(FROZEN_RIVER, "冻河"));
    public static final Map<WildCropClimate, List<ResourceKey<Biome>>> CROPS = Map.of(
            WildCropClimate.HOT, List.of(),
            WildCropClimate.WET, List.of(SIBERIAN_TAIGA, SHIELD, SNOWY_SHIELD, ICE_MARSH),
            WildCropClimate.BEACH, List.of(),
            WildCropClimate.TEMPERATE, List.of(STEPPE, SHIELD, ALPINE_HIGHLANDS),
            WildCropClimate.COLD, List.of(COLD_SHRUBLAND, SIBERIAN_TAIGA, BIRCH_TAIGA, SNOWY_SHIELD, ICE_MARSH));

    private NorthernPlainsRoster() {}
}
