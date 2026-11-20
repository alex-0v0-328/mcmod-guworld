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
 * 西漠维度独有的群系清单，供数据生成阶段的各 Provider 汇总读取。
 * <ul>
 *   <li>{@link #PORTED} 列出西漠独有的 Terralith 移植群系键（对应清单
 *       {@code src/ports/terralith/index.western_desert.json}）；{@link #REPLICAS} 列出除 {@link Replicas#SHARED} 外的
 *       原版复刻群系；{@link #NAMES} 汇总两者的显示名。</li>
 *   <li>{@link #CROPS} 将陆地群系按农夫乐事野生作物气候（{@link WildCropClimate}）分组：沙漠、恶地、峡谷、台地、旱原高
 *       地与荆丛地（温度 ≥ 1.0 且干燥）归干热；绿洲与红绿洲按名称归入湿润（同时满足温度条件也归入干热）；热灌丛归温带；
 *       戈壁归寒冷。</li>
 *   <li>⚠ 更新移植清单新增群系时，必须在同一次改动中同步补齐 {@link #PORTED}、{@link #NAMES} 与 {@link #CROPS}，否则
 *       {@link Ports#registerBiomes} 会在校验清单与键集合时直接报错。</li>
 * </ul>
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
            new BiomeName(ANCIENT_SANDS, "古沙海"),
            new BiomeName(DESERT_CANYON, "沙漠峡谷"),
            new BiomeName(DESERT_OASIS, "绿洲"),
            new BiomeName(RED_OASIS, "红绿洲"),
            new BiomeName(DESERT_SPIRES, "沙漠石柱"),
            new BiomeName(GRAVEL_DESERT, "戈壁"),
            new BiomeName(LUSH_DESERT, "疏林沙漠"),
            new BiomeName(SANDSTONE_VALLEY, "砂岩谷"),
            new BiomeName(ARID_HIGHLANDS, "旱原高地"),
            new BiomeName(WHITE_MESA, "白台地"),
            new BiomeName(BRYCE_CANYON, "石林峡谷"),
            new BiomeName(HOT_SHRUBLAND, "热灌丛"),
            new BiomeName(BRUSHLAND, "荆丛地"),
            new BiomeName(DESERT, "沙漠"),
            new BiomeName(BADLANDS, "恶地"),
            new BiomeName(ERODED_BADLANDS, "风蚀恶地"),
            new BiomeName(WOODED_BADLANDS, "疏林恶地"));
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
