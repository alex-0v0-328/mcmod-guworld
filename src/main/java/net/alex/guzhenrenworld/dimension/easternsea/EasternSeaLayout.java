package net.alex.guzhenrenworld.dimension.easternsea;

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
 * 东海的群系摆放。
 *
 * <ul>
 *   <li>原样用 Terralith 主世界多噪声表（{@link Ports#getBiomeParameters}），来源群系按 {@link #GROUPS}（名单群系 →
 *       其替代的来源）映射到本域；下层本域 Tectonic 副本（{@link EasternSeaTerrain}）把侵蚀与山脊重映射回该表的分段，
 *       参数格保留原地形原型，取同类群系。</li>
 *   <li>名单与来源归属见 wiki《东海#群系》；无来源映射到寒冷或冰冻群系。</li>
 *   <li>{@link #ROSTER}：本域可放的全部群系；{@link #LAND}：除水体（{@link #WATER_AND_CAVES}）外的地表群系，
 *       元泉[Spirit Spring]可立于其上。</li>
 *   <li>⚠ {@link BiomeTable#getBiome} 遇未映射来源即抛错：重移植新增来源时 {@code runData} 失败，不丢格。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see EasternSeaTerrain
 * @since 1.0.0
 */

public final class EasternSeaLayout {

    private static final Map<ResourceKey<Biome>, List<String>> GROUPS = Map.ofEntries(
            Map.entry(EasternSeaRoster.WARM_OCEAN, List.of("minecraft:warm_ocean", "minecraft:lukewarm_ocean",
                    "minecraft:ocean", "terralith:skylands_summer", "terralith:skylands_autumn",
                    "terralith:skylands_spring")),
            Map.entry(EasternSeaRoster.LUKEWARM_OCEAN, List.of("minecraft:cold_ocean", "terralith:skylands_winter")),
            Map.entry(EasternSeaRoster.OCEAN, List.of("minecraft:frozen_ocean")),
            Map.entry(EasternSeaRoster.DEEP_WARM_OCEAN, List.of("terralith:deep_warm_ocean",
                    "minecraft:deep_lukewarm_ocean", "minecraft:deep_ocean", "minecraft:deep_cold_ocean")),
            Map.entry(EasternSeaRoster.DEEP_OCEAN, List.of("minecraft:deep_frozen_ocean")),
            Map.entry(EasternSeaRoster.BEACH, List.of("minecraft:beach", "minecraft:snowy_beach", "minecraft:desert",
                    "terralith:lush_desert", "terralith:ancient_sands", "terralith:desert_oasis",
                    "terralith:red_oasis")),
            Map.entry(EasternSeaRoster.STONY_SHORE, List.of("minecraft:stony_shore")),
            Map.entry(EasternSeaRoster.GRAVEL_BEACH, List.of("terralith:gravel_beach", "terralith:gravel_desert")),
            Map.entry(EasternSeaRoster.ISLES, List.of("terralith:alpha_islands", "terralith:alpha_islands_winter",
                    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:snowy_plains", "minecraft:ice_spikes",
                    "minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                    "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga",
                    "minecraft:old_growth_spruce_taiga", "minecraft:savanna", "minecraft:savanna_plateau",
                    "minecraft:windswept_savanna", "minecraft:windswept_forest", "minecraft:swamp",
                    "minecraft:meadow", "terralith:orchid_swamp", "terralith:ice_marsh", "terralith:shrubland",
                    "terralith:cold_shrubland", "terralith:hot_shrubland", "terralith:rocky_shrubland",
                    "terralith:brushland", "terralith:steppe", "terralith:wintry_lowlands",
                    "terralith:wintry_forest", "terralith:siberian_taiga", "terralith:birch_taiga",
                    "terralith:highlands", "terralith:temperate_highlands", "terralith:forested_highlands",
                    "terralith:alpine_highlands", "terralith:arid_highlands", "terralith:shield",
                    "terralith:shield_clearing", "terralith:valley_clearing", "terralith:yosemite_lowlands")),
            Map.entry(EasternSeaRoster.MIRAGE_ISLES, List.of("terralith:mirage_isles", "terralith:warped_mesa",
                    "minecraft:mushroom_fields", "minecraft:flower_forest", "minecraft:cherry_grove",
                    "terralith:sakura_grove", "terralith:sakura_valley", "terralith:snowy_cherry_grove",
                    "terralith:blooming_plateau", "terralith:blooming_valley", "terralith:lavender_forest",
                    "terralith:lavender_valley", "terralith:lush_valley", "terralith:moonlight_grove",
                    "terralith:moonlight_valley", "terralith:amethyst_canyon")),
            Map.entry(Ports.RAINFOREST, List.of("terralith:tropical_jungle", "terralith:amethyst_rainforest",
                    "terralith:rocky_jungle", "terralith:jungle_mountains", "terralith:haze_mountain",
                    "minecraft:bamboo_jungle", "minecraft:mangrove_swamp")),
            Map.entry(EasternSeaRoster.JUNGLE, List.of("minecraft:jungle", "minecraft:sparse_jungle",
                    "minecraft:dark_forest", "terralith:cloud_forest")),
            Map.entry(EasternSeaRoster.VOLCANIC_PEAKS, List.of("terralith:volcanic_peaks", "minecraft:stony_peaks",
                    "terralith:savanna_slopes", "terralith:ashen_savanna", "terralith:painted_mountains",
                    "terralith:yellowstone")),
            Map.entry(EasternSeaRoster.VOLCANIC_CRATER, List.of("terralith:volcanic_crater", "terralith:caldera")),
            Map.entry(EasternSeaRoster.BASALT_CLIFFS, List.of("terralith:basalt_cliffs", "terralith:granite_cliffs",
                    "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands",
                    "terralith:savanna_badlands", "terralith:bryce_canyon", "terralith:white_mesa",
                    "terralith:desert_canyon", "terralith:sandstone_valley", "terralith:desert_spires",
                    "terralith:fractured_savanna", "terralith:stony_spires", "terralith:windswept_spires")),
            Map.entry(EasternSeaRoster.WHITE_CLIFFS, List.of("terralith:white_cliffs", "terralith:frozen_cliffs",
                    "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:snowy_slopes", "minecraft:grove",
                    "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "terralith:alpine_grove",
                    "terralith:emerald_peaks", "terralith:scarlet_mountains", "terralith:rocky_mountains",
                    "terralith:snowy_maple_forest", "terralith:snowy_shield", "terralith:siberian_grove",
                    "terralith:snowy_badlands", "terralith:yosemite_cliffs", "terralith:glacial_chasm")),
            Map.entry(Replicas.RIVER, List.of("minecraft:river", "minecraft:frozen_river", "terralith:warm_river")),
            Map.entry(Replicas.LUSH_CAVES, List.of("minecraft:lush_caves", "terralith:cave/underground_jungle",
                    "terralith:cave/fungal_caves")),
            Map.entry(Replicas.DRIPSTONE_CAVES, List.of("minecraft:dripstone_caves", "minecraft:deep_dark",
                    "terralith:cave/andesite_caves", "terralith:cave/deep_caves", "terralith:cave/diorite_caves",
                    "terralith:cave/frostfire_caves", "terralith:cave/granite_caves", "terralith:cave/infested_caves",
                    "terralith:cave/mantle_caves", "terralith:cave/thermal_caves", "terralith:cave/tuff_caves")));
    public static final Set<ResourceKey<Biome>> ROSTER = GROUPS.keySet();
    private static final Set<ResourceKey<Biome>> WATER_AND_CAVES = Set.of(Replicas.RIVER,
            EasternSeaRoster.WARM_OCEAN, EasternSeaRoster.LUKEWARM_OCEAN, EasternSeaRoster.OCEAN,
            EasternSeaRoster.DEEP_WARM_OCEAN, EasternSeaRoster.DEEP_OCEAN, Replicas.LUSH_CAVES,
            Replicas.DRIPSTONE_CAVES);
    public static final List<ResourceKey<Biome>> LAND = ROSTER.stream()
            .filter(biome -> !WATER_AND_CAVES.contains(biome))
            .sorted(Comparator.comparing(ResourceKey::location)).toList();

    public static final BiomeTable TABLE = BiomeTable.of("Eastern Sea", GROUPS);

    private EasternSeaLayout() {}
}
