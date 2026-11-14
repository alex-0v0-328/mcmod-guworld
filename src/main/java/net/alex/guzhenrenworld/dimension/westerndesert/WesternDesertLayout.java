package net.alex.guzhenrenworld.dimension.westerndesert;

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
 * 西漠的群系摆放。
 * <ul>
 *   <li>原样用 Terralith 主世界多噪声表（{@link Ports#getBiomeParameters}），来源群系映射到本域名单（{@link #GROUPS}：
 *       名单群系 → 其替代的来源群系）；下层本域 Tectonic 地形（{@link WesternDesertTerrain}）把侵蚀与山脊重映射回该表分
 *       段，参数格保留 Terralith 地形原型，得本域同类群系。</li>
 *   <li>名单与来源归属见 wiki《西漠#群系》。</li>
 *   <li>海洋、海滩、岛屿来源只为完整而映射，{@link WesternDesertTerrain} 的大陆度到不了。</li>
 *   <li>{@link #ROSTER}：全部可放群系，含沙漠石柱；{@link #LAND}：除河流外地表群系，元泉[Spirit Spring]可生于此。</li>
 *   <li>地形随侵蚀不随群系：Terralith 石林带（侵蚀 ≥ {@link BiomeTable#SPIRE_EROSION}）一律长 Tectonic 尖柱地形，故陆地
 *       格改为沙漠石柱（{@link BiomeTable#withSpires}），但 {@link #TABLE} 保留河流、洞穴、两绿洲、砂岩谷、沙漠峡
 *       谷。</li>
 *   <li>⚠ {@link BiomeTable#getBiome} 遇 {@link #GROUPS} 未映射的来源群系即抛错，重新移植多出一个则 {@code runData} 失
 *       败，不丢格。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see WesternDesertTerrain
 * @since 1.0.0
 */

public final class WesternDesertLayout {

    private static final Map<ResourceKey<Biome>, List<String>> GROUPS = Map.ofEntries(
            Map.entry(Replicas.RIVER, List.of("minecraft:river", "minecraft:frozen_river", "terralith:warm_river")),
            Map.entry(WesternDesertRoster.DESERT, List.of("minecraft:desert", "minecraft:beach",
                    "minecraft:mushroom_fields", "minecraft:ocean", "minecraft:deep_ocean", "minecraft:cold_ocean",
                    "minecraft:deep_cold_ocean", "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean",
                    "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:warm_ocean",
                    "terralith:deep_warm_ocean", "terralith:alpha_islands", "terralith:alpha_islands_winter",
                    "terralith:mirage_isles", "terralith:skylands_autumn", "terralith:skylands_spring",
                    "terralith:skylands_summer", "terralith:skylands_winter")),
            Map.entry(WesternDesertRoster.ANCIENT_SANDS, List.of("terralith:ancient_sands")),
            Map.entry(WesternDesertRoster.LUSH_DESERT, List.of("terralith:lush_desert", "minecraft:jungle",
                    "minecraft:dark_forest", "terralith:tropical_jungle", "terralith:amethyst_rainforest")),
            Map.entry(WesternDesertRoster.DESERT_OASIS, List.of("terralith:desert_oasis", "minecraft:swamp",
                    "terralith:orchid_swamp")),
            Map.entry(WesternDesertRoster.RED_OASIS, List.of("terralith:red_oasis", "minecraft:mangrove_swamp")),
            Map.entry(WesternDesertRoster.HOT_SHRUBLAND, List.of("terralith:hot_shrubland", "minecraft:forest",
                    "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                    "minecraft:cherry_grove", "terralith:sakura_grove", "terralith:lavender_forest",
                    "terralith:yellowstone")),
            Map.entry(WesternDesertRoster.BRUSHLAND, List.of("terralith:brushland", "minecraft:savanna")),
            Map.entry(WesternDesertRoster.GRAVEL_DESERT, List.of("terralith:gravel_desert", "minecraft:plains",
                    "minecraft:sunflower_plains", "minecraft:meadow", "terralith:shrubland", "minecraft:snowy_plains",
                    "minecraft:ice_spikes", "minecraft:snowy_beach", "minecraft:stony_shore", "terralith:steppe",
                    "terralith:cold_shrubland", "terralith:rocky_shrubland", "terralith:wintry_lowlands",
                    "terralith:ice_marsh", "terralith:gravel_beach")),
            Map.entry(WesternDesertRoster.ARID_HIGHLANDS, List.of("terralith:arid_highlands",
                    "minecraft:savanna_plateau", "minecraft:windswept_hills", "minecraft:windswept_forest",
                    "minecraft:windswept_gravelly_hills", "minecraft:windswept_savanna", "minecraft:taiga",
                    "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                    "minecraft:grove", "minecraft:snowy_slopes", "terralith:highlands",
                    "terralith:temperate_highlands", "terralith:blooming_plateau", "terralith:shield",
                    "terralith:shield_clearing", "terralith:snowy_shield", "terralith:savanna_slopes",
                    "terralith:snowy_badlands", "terralith:siberian_taiga", "terralith:siberian_grove",
                    "terralith:wintry_forest", "terralith:snowy_maple_forest", "terralith:moonlight_grove",
                    "terralith:birch_taiga", "terralith:alpine_highlands", "terralith:forested_highlands",
                    "terralith:snowy_cherry_grove", "terralith:alpine_grove")),
            Map.entry(WesternDesertRoster.SANDSTONE_VALLEY, List.of("terralith:sandstone_valley",
                    "minecraft:frozen_peaks", "minecraft:jagged_peaks", "terralith:glacial_chasm",
                    "terralith:yosemite_cliffs", "terralith:yosemite_lowlands", "terralith:moonlight_valley",
                    "terralith:lavender_valley", "terralith:lush_valley", "terralith:blooming_valley",
                    "terralith:sakura_valley", "terralith:valley_clearing", "terralith:frozen_cliffs",
                    "terralith:white_cliffs", "terralith:emerald_peaks", "terralith:scarlet_mountains",
                    "terralith:rocky_mountains", "terralith:cloud_forest", "terralith:haze_mountain")),
            Map.entry(WesternDesertRoster.DESERT_CANYON, List.of("terralith:desert_canyon",
                    "terralith:amethyst_canyon")),
            Map.entry(WesternDesertRoster.BADLANDS, List.of("minecraft:badlands", "minecraft:stony_peaks",
                    "terralith:painted_mountains", "terralith:granite_cliffs", "terralith:volcanic_peaks",
                    "terralith:volcanic_crater", "terralith:caldera", "terralith:basalt_cliffs",
                    "terralith:ashen_savanna", "terralith:warped_mesa", "terralith:savanna_badlands")),
            Map.entry(WesternDesertRoster.ERODED_BADLANDS, List.of("minecraft:eroded_badlands")),
            Map.entry(WesternDesertRoster.WOODED_BADLANDS, List.of("minecraft:wooded_badlands",
                    "minecraft:sparse_jungle", "minecraft:bamboo_jungle", "terralith:rocky_jungle",
                    "terralith:jungle_mountains")),
            Map.entry(WesternDesertRoster.WHITE_MESA, List.of("terralith:white_mesa")),
            Map.entry(WesternDesertRoster.BRYCE_CANYON, List.of("terralith:bryce_canyon")),
            Map.entry(WesternDesertRoster.DESERT_SPIRES, List.of("terralith:desert_spires", "terralith:stony_spires",
                    "terralith:windswept_spires", "terralith:fractured_savanna")),
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

    public static final BiomeTable TABLE = BiomeTable.withSpires("Western Desert", GROUPS,
            Set.of(Replicas.RIVER, Replicas.LUSH_CAVES, Replicas.DRIPSTONE_CAVES, WesternDesertRoster.DESERT_OASIS,
                    WesternDesertRoster.RED_OASIS, WesternDesertRoster.SANDSTONE_VALLEY,
                    WesternDesertRoster.DESERT_CANYON),
            WesternDesertRoster.DESERT_SPIRES);

    private WesternDesertLayout() {}
}
