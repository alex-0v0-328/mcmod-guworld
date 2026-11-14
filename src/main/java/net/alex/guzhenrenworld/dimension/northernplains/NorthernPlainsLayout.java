package net.alex.guzhenrenworld.dimension.northernplains;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenrenworld.biome.BiomeTable;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 北原的群系摆放。
 *
 * <ul>
 *   <li>原样用 Terralith 主世界多噪声表（{@link Ports#getBiomeParameters}），来源群系按 {@link #GROUPS}（名单群系 →
 *       其替代的来源）映射到本域；下层本域 Tectonic 副本（{@link NorthernPlainsTerrain}）把侵蚀与山脊重映射回该表的分
 *       段，参数格保留原地形原型，取同类群系。</li>
 *   <li>山地（{@link #MOUNTAINS}）：冰封山峰、冰崖、冰川裂谷（地貌）；丘陵台地（{@link #HILLS}）：雪林、高寒高地、
 *       地盾、雪地盾；平地：干草原、寒灌丛、寒原低地、雪原、冰刺之地、冰沼、西伯利亚针叶林、桦木针叶林、积雪针叶林、
 *       寒林、两种河流；地下：溶洞，接全部洞穴来源。</li>
 *   <li>Terralith 表先按温度分段，本域保留：冰冻段（低于 -0.45）来源映射到下雪群系，更暖的映射到不下雪的（雪地盾、
 *       雪坡除外）；{@link NorthernPlainsTerrain} 给温度加的纬度梯度因此北雪南草，表不必改。</li>
 *   <li>山峰一律归冰封山峰，悬崖与恶地归冰崖，不论温度（本域只有冰山），峡谷归冰川裂谷；Terralith 冰崖稀有，故冰冻段两
 *       个山峰来源（翠峰、绯红山）也归冰崖。</li>
 *   <li>暖段、热段来源（梯度下几乎采不到）与海洋、海滩、岛屿来源（大陆度到不了）只为表完整，映射到最近地貌；
 *       {@link #FROZEN}：冰冻段够到的群系。</li>
 *   <li>{@link #ROSTER}：本域可放的全部群系；{@link #LAND}：除水体与洞穴（{@link #WATER_AND_CAVES}）外的地表群系，
 *       元泉[Spirit Spring]可立于其上。</li>
 *   <li>⚠ {@link BiomeTable#getBiome} 遇未映射来源即抛错：重移植新增来源时 {@code runData} 失败，不丢格。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see NorthernPlainsTerrain
 * @since 1.0.0
 */

public final class NorthernPlainsLayout {

    private static final Map<ResourceKey<Biome>, List<String>> GROUPS = Map.ofEntries(
            Map.entry(Replicas.RIVER, List.of("minecraft:river", "terralith:warm_river")),
            Map.entry(NorthernPlainsRoster.FROZEN_RIVER, List.of("minecraft:frozen_river")),
            Map.entry(NorthernPlainsRoster.STEPPE, List.of("minecraft:plains", "minecraft:sunflower_plains",
                    "minecraft:savanna", "minecraft:desert", "minecraft:beach", "minecraft:stony_shore",
                    "minecraft:mushroom_fields", "minecraft:ocean", "minecraft:deep_ocean", "minecraft:cold_ocean",
                    "minecraft:deep_cold_ocean", "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean",
                    "minecraft:warm_ocean", "terralith:deep_warm_ocean", "terralith:gravel_beach",
                    "terralith:alpha_islands", "terralith:mirage_isles", "terralith:skylands_autumn",
                    "terralith:skylands_spring", "terralith:skylands_summer", "terralith:skylands_winter",
                    "terralith:steppe", "terralith:shrubland", "terralith:hot_shrubland", "terralith:brushland",
                    "terralith:ancient_sands", "terralith:lush_desert", "terralith:desert_oasis",
                    "terralith:red_oasis", "terralith:yosemite_lowlands", "terralith:valley_clearing",
                    "terralith:lush_valley", "terralith:moonlight_valley", "terralith:blooming_valley",
                    "terralith:lavender_valley", "terralith:sakura_valley")),
            Map.entry(NorthernPlainsRoster.BIRCH_TAIGA, List.of("minecraft:forest", "minecraft:flower_forest",
                    "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest",
                    "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                    "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle",
                    "terralith:birch_taiga", "terralith:lavender_forest", "terralith:moonlight_grove",
                    "terralith:sakura_grove", "terralith:tropical_jungle", "terralith:amethyst_rainforest",
                    "terralith:rocky_jungle", "terralith:forested_highlands")),
            Map.entry(NorthernPlainsRoster.SHIELD, List.of("minecraft:windswept_hills",
                    "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:windswept_savanna",
                    "minecraft:savanna_plateau", "terralith:shield", "terralith:shield_clearing",
                    "terralith:arid_highlands", "terralith:yellowstone", "terralith:cloud_forest",
                    "terralith:haze_mountain", "terralith:jungle_mountains", "terralith:stony_spires",
                    "terralith:windswept_spires", "terralith:desert_spires", "terralith:fractured_savanna")),
            Map.entry(NorthernPlainsRoster.SNOWY_SHIELD, List.of("terralith:snowy_shield",
                    "terralith:snowy_maple_forest")),
            Map.entry(NorthernPlainsRoster.ALPINE_HIGHLANDS, List.of("minecraft:meadow", "minecraft:cherry_grove",
                    "terralith:alpine_highlands", "terralith:temperate_highlands", "terralith:highlands",
                    "terralith:blooming_plateau", "terralith:savanna_slopes", "terralith:ashen_savanna")),
            Map.entry(NorthernPlainsRoster.COLD_SHRUBLAND, List.of("terralith:cold_shrubland",
                    "terralith:rocky_shrubland", "terralith:gravel_desert")),
            Map.entry(NorthernPlainsRoster.WINTRY_LOWLANDS, List.of("minecraft:snowy_beach",
                    "terralith:wintry_lowlands", "terralith:snowy_badlands", "terralith:alpha_islands_winter")),
            Map.entry(NorthernPlainsRoster.SNOWY_PLAINS, List.of("minecraft:snowy_plains", "minecraft:frozen_ocean",
                    "minecraft:deep_frozen_ocean")),
            Map.entry(NorthernPlainsRoster.ICE_SPIKES, List.of("minecraft:ice_spikes")),
            Map.entry(NorthernPlainsRoster.ICE_MARSH, List.of("minecraft:swamp", "minecraft:mangrove_swamp",
                    "terralith:orchid_swamp", "terralith:ice_marsh")),
            Map.entry(NorthernPlainsRoster.SNOWY_TAIGA, List.of("minecraft:snowy_taiga")),
            Map.entry(NorthernPlainsRoster.SIBERIAN_TAIGA, List.of("terralith:siberian_taiga",
                    "terralith:siberian_grove")),
            Map.entry(NorthernPlainsRoster.WINTRY_FOREST, List.of("terralith:wintry_forest")),
            Map.entry(NorthernPlainsRoster.GROVE, List.of("minecraft:grove", "minecraft:snowy_slopes",
                    "terralith:alpine_grove", "terralith:snowy_cherry_grove")),
            Map.entry(NorthernPlainsRoster.FROZEN_PEAKS, List.of("minecraft:frozen_peaks", "minecraft:jagged_peaks",
                    "minecraft:stony_peaks", "terralith:rocky_mountains")),
            Map.entry(NorthernPlainsRoster.FROZEN_CLIFFS, List.of("minecraft:badlands", "minecraft:eroded_badlands",
                    "minecraft:wooded_badlands", "terralith:frozen_cliffs", "terralith:emerald_peaks",
                    "terralith:scarlet_mountains", "terralith:white_cliffs",
                    "terralith:granite_cliffs", "terralith:basalt_cliffs", "terralith:volcanic_peaks",
                    "terralith:volcanic_crater", "terralith:caldera", "terralith:painted_mountains",
                    "terralith:savanna_badlands")),
            Map.entry(NorthernPlainsRoster.GLACIAL_CHASM, List.of("terralith:glacial_chasm",
                    "terralith:yosemite_cliffs", "terralith:desert_canyon", "terralith:amethyst_canyon",
                    "terralith:bryce_canyon", "terralith:sandstone_valley", "terralith:white_mesa",
                    "terralith:warped_mesa")),
            Map.entry(Replicas.DRIPSTONE_CAVES, List.of("minecraft:dripstone_caves", "minecraft:deep_dark",
                    "minecraft:lush_caves", "terralith:cave/underground_jungle", "terralith:cave/fungal_caves",
                    "terralith:cave/andesite_caves", "terralith:cave/deep_caves", "terralith:cave/diorite_caves",
                    "terralith:cave/frostfire_caves", "terralith:cave/granite_caves", "terralith:cave/infested_caves",
                    "terralith:cave/mantle_caves", "terralith:cave/thermal_caves", "terralith:cave/tuff_caves")));
    public static final Set<ResourceKey<Biome>> ROSTER = GROUPS.keySet();
    public static final List<ResourceKey<Biome>> MOUNTAINS = List.of(NorthernPlainsRoster.FROZEN_PEAKS,
            NorthernPlainsRoster.FROZEN_CLIFFS, NorthernPlainsRoster.GLACIAL_CHASM);
    public static final List<ResourceKey<Biome>> HILLS = List.of(NorthernPlainsRoster.GROVE,
            NorthernPlainsRoster.ALPINE_HIGHLANDS, NorthernPlainsRoster.SHIELD, NorthernPlainsRoster.SNOWY_SHIELD);
    public static final List<ResourceKey<Biome>> FROZEN = List.of(NorthernPlainsRoster.FROZEN_RIVER,
            NorthernPlainsRoster.COLD_SHRUBLAND, NorthernPlainsRoster.WINTRY_LOWLANDS,
            NorthernPlainsRoster.SNOWY_PLAINS, NorthernPlainsRoster.ICE_SPIKES, NorthernPlainsRoster.ICE_MARSH,
            NorthernPlainsRoster.SNOWY_TAIGA, NorthernPlainsRoster.SIBERIAN_TAIGA, NorthernPlainsRoster.WINTRY_FOREST,
            NorthernPlainsRoster.GROVE, NorthernPlainsRoster.FROZEN_PEAKS, NorthernPlainsRoster.FROZEN_CLIFFS,
            NorthernPlainsRoster.GLACIAL_CHASM);
    private static final Set<ResourceKey<Biome>> WATER_AND_CAVES = Set.of(Replicas.RIVER,
            NorthernPlainsRoster.FROZEN_RIVER, Replicas.DRIPSTONE_CAVES);
    public static final List<ResourceKey<Biome>> LAND = ROSTER.stream()
            .filter(biome -> !WATER_AND_CAVES.contains(biome))
            .sorted(Comparator.comparing(ResourceKey::location)).toList();

    public static final BiomeTable TABLE = BiomeTable.of("Northern Plains", GROUPS);

    private NorthernPlainsLayout() {}
}
