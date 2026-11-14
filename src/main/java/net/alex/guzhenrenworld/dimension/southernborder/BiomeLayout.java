package net.alex.guzhenrenworld.dimension.southernborder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.alex.guzhenrenworld.biome.BiomeTable;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.biome.WildCropClimate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 南疆的群系摆放。
 * <ul>
 *   <li>原样用 Terralith 主世界多噪声表（{@link Ports#getBiomeParameters}），来源群系映射到本域名单（{@link #GROUPS}：
 *       名单群系 → 其替代的来源群系）；下层 Tectonic 地形（{@link Terrain}）把侵蚀与山脊重映射回该表分段，参数格保留
 *       Terralith 地形原型，得南疆同类群系。</li>
 *   <li>名单按地形带：山地为雾山、云雾林、裸岩峰、翠峰、丛林山、彩岩山、花岗岩崖、高山草甸，及嶂石岩（Terralith 崖与峡
 *       谷格）、喀斯特峰林（石林带格）两种地貌；丘陵台地为竹海（{@link BiomeSettings}）、雨林、高原、林间高原、桃林；平
 *       地为平原、兰花沼泽、热海、河流；地下为繁茂洞穴、溶洞。</li>
 *   <li>来源归同地貌、植被最近的名单群系：寒带归山地森林与草甸，沙漠与干灌丛归平原，恶地归彩岩山；Terralith 把雾山、翠
 *       峰、丛林山设得稀有，各多接一个常见邻居：原版山坡、尖峭山峰、岩石丛林。</li>
 *   <li>海洋、海滩、岛屿来源只为完整而映射，{@link Terrain} 的大陆度到不了。</li>
 *   <li>{@link #ROSTER}：能放的全部群系，含喀斯特峰林；{@link #LAND}：除河流外的地表群系，元泉[Spirit Spring]可生于此；
 *       {@link #CROPS}：陆地按 {@link WildCropClimate} 的温度降水规则分进农夫乐事野生作物气候。</li>
 *   <li>地形随侵蚀不随群系：Terralith 石林带（侵蚀 ≥ {@link BiomeTable#SPIRE_EROSION}，Tectonic 仅在自身侵蚀超过 1 处到
 *       达）一律长 Tectonic 低因子尖柱地形，故陆地格改为喀斯特峰林（{@link BiomeTable#withSpires}），{@link #TABLE} 保
 *       留的河流、洞穴、沼泽、热海、嶂石岩除外。</li>
 *   <li>⚠ {@link BiomeTable#getBiome} 遇 {@link #GROUPS} 未映射的来源群系即抛错，重新移植多出一个则 {@code runData} 失
 *       败，不丢格。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Terrain
 * @since 1.0.0
 */

public final class BiomeLayout {

    private static final Map<ResourceKey<Biome>, List<String>> GROUPS = Map.ofEntries(
            Map.entry(Replicas.RIVER, List.of("minecraft:river", "minecraft:frozen_river", "terralith:warm_river")),
            Map.entry(Replicas.PLAINS, List.of("minecraft:plains", "minecraft:sunflower_plains",
                    "minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:savanna", "minecraft:desert",
                    "minecraft:beach", "minecraft:snowy_beach", "minecraft:stony_shore", "minecraft:mushroom_fields",
                    "minecraft:ocean", "minecraft:deep_ocean", "minecraft:cold_ocean", "minecraft:deep_cold_ocean",
                    "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean", "minecraft:lukewarm_ocean",
                    "minecraft:deep_lukewarm_ocean", "minecraft:warm_ocean", "terralith:deep_warm_ocean",
                    "terralith:gravel_beach", "terralith:alpha_islands", "terralith:alpha_islands_winter",
                    "terralith:mirage_isles", "terralith:skylands_autumn", "terralith:skylands_spring",
                    "terralith:skylands_summer", "terralith:skylands_winter", "terralith:steppe",
                    "terralith:shrubland", "terralith:cold_shrubland", "terralith:hot_shrubland",
                    "terralith:rocky_shrubland", "terralith:brushland", "terralith:gravel_desert",
                    "terralith:lush_desert", "terralith:ancient_sands", "terralith:ashen_savanna",
                    "terralith:wintry_lowlands", "terralith:yosemite_lowlands", "terralith:valley_clearing",
                    "terralith:desert_oasis", "terralith:red_oasis")),
            Map.entry(Ports.ORCHID_SWAMP, List.of("minecraft:swamp", "minecraft:mangrove_swamp",
                    "terralith:orchid_swamp", "terralith:ice_marsh")),
            Map.entry(Ports.HOT_SPRINGS, List.of("terralith:yellowstone")),
            Map.entry(Ports.FORESTED_PLATEAU, List.of("minecraft:forest", "minecraft:flower_forest",
                    "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "terralith:birch_taiga",
                    "terralith:lavender_forest", "terralith:forested_highlands")),
            Map.entry(BiomeSettings.BAMBOO_SEA, List.of("minecraft:bamboo_jungle", "minecraft:sparse_jungle")),
            Map.entry(Ports.RAINFOREST, List.of("minecraft:jungle", "minecraft:dark_forest",
                    "terralith:tropical_jungle", "terralith:amethyst_rainforest")),
            Map.entry(Ports.CLOUD_FOREST, List.of("minecraft:taiga", "minecraft:snowy_taiga",
                    "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                    "terralith:siberian_taiga", "terralith:siberian_grove", "terralith:wintry_forest",
                    "terralith:snowy_maple_forest", "terralith:moonlight_grove", "terralith:moonlight_valley",
                    "terralith:cloud_forest")),
            Map.entry(Ports.PLATEAU, List.of("minecraft:savanna_plateau", "minecraft:windswept_hills",
                    "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills", "minecraft:windswept_savanna",
                    "terralith:highlands", "terralith:temperate_highlands", "terralith:arid_highlands",
                    "terralith:shield", "terralith:shield_clearing", "terralith:snowy_shield",
                    "terralith:savanna_slopes", "terralith:blooming_plateau")),
            Map.entry(Ports.PEACH_GROVE, List.of("minecraft:cherry_grove", "terralith:sakura_grove",
                    "terralith:sakura_valley", "terralith:snowy_cherry_grove", "terralith:blooming_valley",
                    "terralith:lavender_valley", "terralith:lush_valley")),
            Map.entry(Ports.ALPINE_MEADOW, List.of("minecraft:meadow", "terralith:alpine_highlands")),
            Map.entry(Ports.ROCKY_PEAKS, List.of("minecraft:frozen_peaks", "minecraft:stony_peaks",
                    "terralith:rocky_mountains")),
            Map.entry(Ports.EMERALD_PEAKS, List.of("minecraft:jagged_peaks", "terralith:emerald_peaks",
                    "terralith:scarlet_mountains")),
            Map.entry(Ports.MISTY_MOUNTAINS, List.of("minecraft:grove", "minecraft:snowy_slopes",
                    "terralith:alpine_grove", "terralith:haze_mountain")),
            Map.entry(Ports.JUNGLE_MOUNTAINS, List.of("terralith:jungle_mountains", "terralith:rocky_jungle")),
            Map.entry(Ports.PAINTED_MOUNTAINS, List.of("minecraft:badlands", "minecraft:eroded_badlands",
                    "minecraft:wooded_badlands", "terralith:painted_mountains", "terralith:snowy_badlands",
                    "terralith:savanna_badlands", "terralith:warped_mesa")),
            Map.entry(Ports.GRANITE_CLIFFS, List.of("terralith:granite_cliffs", "terralith:frozen_cliffs",
                    "terralith:volcanic_peaks", "terralith:volcanic_crater", "terralith:caldera",
                    "terralith:basalt_cliffs", "terralith:white_cliffs")),
            Map.entry(Ports.ZHANGSHIYAN_CLIFFS, List.of("terralith:yosemite_cliffs", "terralith:white_mesa",
                    "terralith:sandstone_valley", "terralith:glacial_chasm", "terralith:amethyst_canyon",
                    "terralith:desert_canyon", "terralith:bryce_canyon")),
            Map.entry(Ports.KARST_PEAK_FOREST, List.of("terralith:stony_spires", "terralith:windswept_spires",
                    "terralith:desert_spires", "terralith:fractured_savanna")),
            Map.entry(Replicas.LUSH_CAVES, List.of("minecraft:lush_caves", "terralith:cave/underground_jungle",
                    "terralith:cave/fungal_caves")),
            Map.entry(Replicas.DRIPSTONE_CAVES, List.of("minecraft:dripstone_caves", "minecraft:deep_dark",
                    "terralith:cave/andesite_caves", "terralith:cave/deep_caves", "terralith:cave/diorite_caves",
                    "terralith:cave/frostfire_caves", "terralith:cave/granite_caves", "terralith:cave/infested_caves",
                    "terralith:cave/mantle_caves", "terralith:cave/thermal_caves", "terralith:cave/tuff_caves")));
    public static final Set<ResourceKey<Biome>> ROSTER = GROUPS.keySet();
    private static final Set<ResourceKey<Biome>> WATER_AND_CAVES = Set.of(Replicas.RIVER, Replicas.LUSH_CAVES,
            Replicas.DRIPSTONE_CAVES);
    public static final List<ResourceKey<Biome>> LAND = ROSTER.stream()
            .filter(biome -> !WATER_AND_CAVES.contains(biome))
            .sorted(Comparator.comparing(ResourceKey::location)).toList();
    public static final Map<WildCropClimate, List<ResourceKey<Biome>>> CROPS = Map.of(
            WildCropClimate.HOT, List.of(Ports.PAINTED_MOUNTAINS),
            WildCropClimate.WET, List.of(Ports.CLOUD_FOREST, Ports.HOT_SPRINGS, Ports.ORCHID_SWAMP, Ports.PEACH_GROVE,
                    Ports.RAINFOREST, Ports.JUNGLE_MOUNTAINS, Ports.ZHANGSHIYAN_CLIFFS, BiomeSettings.BAMBOO_SEA),
            WildCropClimate.BEACH, List.of(),
            WildCropClimate.TEMPERATE, List.of(Replicas.PLAINS, Ports.PLATEAU, Ports.ORCHID_SWAMP, Ports.PEACH_GROVE,
                    Ports.GRANITE_CLIFFS, Ports.ALPINE_MEADOW, Ports.KARST_PEAK_FOREST),
            WildCropClimate.COLD, List.of(Ports.CLOUD_FOREST, Ports.MISTY_MOUNTAINS, Ports.HOT_SPRINGS,
                    Ports.ROCKY_PEAKS, Ports.EMERALD_PEAKS));

    public static final BiomeTable TABLE = BiomeTable.withSpires("Southern Border", GROUPS,
            Set.of(Replicas.RIVER, Replicas.LUSH_CAVES, Replicas.DRIPSTONE_CAVES, Ports.ORCHID_SWAMP, Ports.HOT_SPRINGS,
                    Ports.ZHANGSHIYAN_CLIFFS),
            Ports.KARST_PEAK_FOREST);

    private BiomeLayout() {}
}
