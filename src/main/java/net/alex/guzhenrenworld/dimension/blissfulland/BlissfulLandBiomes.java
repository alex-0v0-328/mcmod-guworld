package net.alex.guzhenrenworld.dimension.blissfulland;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.alex.guzhenrenworld.biome.BiomeName;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.biome.WildCropClimate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

/**
 * 狐仙福地的群系名单与多噪声表：照中州（原版主世界）的温带陆地与山地群系，平地圈另占一格。
 * <ul>
 *   <li>表取原版主世界预设（{@link MultiNoiseBiomeSourceParameterList.Preset#OVERWORLD}）逐条照抄，只把群系按
 *       {@link #SOURCES} 换成本名单里的复刻；沙漠、热带草原、恶地、丛林、红树林与海洋、海滩这些名单外的群系并到最接近的
 *       平原或森林（原著称狐仙福地是草原地貌，不是沙漠），雪原、冰刺并到针叶林，冻河并到河流，深暗之域并到溶洞。</li>
 *   <li>平地圈：表里另加一格，大陆度在 {@link #RING_CONTINENTALNESS} 以上、深度 0 到 {@link #RING_DEPTH}、其余参数全
 *       域，指向 {@link FoxImmortalBlissfulLand#BIOME}；圈外大陆度截到 1，只有 {@link BlissfulLandTerrain} 在山心周围
 *       把大陆度压到 2 时才落进这一格，荡魂山的结构群系标签也只认这个群系。这一格离 1 留得远：Tectonic 的温度、侵
 *       蚀会超出原版范围，离原版各格都远的点会被全域的这一格抢走。</li>
 *   <li>气候参数编解码只收 ±{@link #PARAMETER_LIMIT}（±2）；原版各格深度只有 0、1、0.2–0.9 与 1.1 几档，若平地圈深
 *       度设为全域会让远处高空（深度很负）落进该格，故限定在 0 到 {@link #RING_DEPTH}。</li>
 *   <li>复刻：{@link #REPLICAS} 是只有福地用的原版复刻；平原、河流、两种洞穴、雪林与冰封山峰和别的域共用，在
 *       {@link Replicas#SHARED}。</li>
 *   <li>⚠ 原版预设里每个群系都要在 {@link #SOURCES} 有去处，漏掉的在数据生成时直接报错。</li>
 * </ul>
 */

public final class BlissfulLandBiomes {

    public static final float RING_CONTINENTALNESS = 1.9F;
    private static final float PARAMETER_LIMIT = 2.0F;
    private static final float RING_DEPTH = 1.1F;
    public static final List<Replicas.Template> REPLICAS = List.of(
            Replicas.of(Biomes.SUNFLOWER_PLAINS,
                    (placed, carvers) -> OverworldBiomes.plains(placed, carvers, true, false, false)),
            Replicas.of(Biomes.MEADOW,
                    (placed, carvers) -> OverworldBiomes.meadowOrCherryGrove(placed, carvers, false)),
            Replicas.of(Biomes.SWAMP, OverworldBiomes::swamp),
            Replicas.of(Biomes.FOREST,
                    (placed, carvers) -> OverworldBiomes.forest(placed, carvers, false, false, false)),
            Replicas.of(Biomes.FLOWER_FOREST,
                    (placed, carvers) -> OverworldBiomes.forest(placed, carvers, false, false, true)),
            Replicas.of(Biomes.BIRCH_FOREST,
                    (placed, carvers) -> OverworldBiomes.forest(placed, carvers, true, false, false)),
            Replicas.of(Biomes.OLD_GROWTH_BIRCH_FOREST,
                    (placed, carvers) -> OverworldBiomes.forest(placed, carvers, true, true, false)),
            Replicas.of(Biomes.DARK_FOREST, OverworldBiomes::darkForest),
            Replicas.of(Biomes.CHERRY_GROVE,
                    (placed, carvers) -> OverworldBiomes.meadowOrCherryGrove(placed, carvers, true)),
            Replicas.of(Biomes.TAIGA, (placed, carvers) -> OverworldBiomes.taiga(placed, carvers, false)),
            Replicas.of(Biomes.OLD_GROWTH_PINE_TAIGA,
                    (placed, carvers) -> OverworldBiomes.oldGrowthTaiga(placed, carvers, false)),
            Replicas.of(Biomes.OLD_GROWTH_SPRUCE_TAIGA,
                    (placed, carvers) -> OverworldBiomes.oldGrowthTaiga(placed, carvers, true)),
            Replicas.of(Biomes.WINDSWEPT_HILLS,
                    (placed, carvers) -> OverworldBiomes.windsweptHills(placed, carvers, false)),
            Replicas.of(Biomes.WINDSWEPT_FOREST,
                    (placed, carvers) -> OverworldBiomes.windsweptHills(placed, carvers, true)),
            Replicas.of(Biomes.SNOWY_SLOPES, OverworldBiomes::snowySlopes),
            Replicas.of(Biomes.JAGGED_PEAKS, OverworldBiomes::jaggedPeaks),
            Replicas.of(Biomes.STONY_PEAKS, OverworldBiomes::stonyPeaks));
    private static final ResourceKey<Biome> SUNFLOWER_PLAINS = Replicas.keyOf(Biomes.SUNFLOWER_PLAINS);
    private static final ResourceKey<Biome> MEADOW = Replicas.keyOf(Biomes.MEADOW);
    private static final ResourceKey<Biome> SWAMP = Replicas.keyOf(Biomes.SWAMP);
    private static final ResourceKey<Biome> FOREST = Replicas.keyOf(Biomes.FOREST);
    private static final ResourceKey<Biome> FLOWER_FOREST = Replicas.keyOf(Biomes.FLOWER_FOREST);
    private static final ResourceKey<Biome> BIRCH_FOREST = Replicas.keyOf(Biomes.BIRCH_FOREST);
    private static final ResourceKey<Biome> OLD_GROWTH_BIRCH_FOREST = Replicas.keyOf(Biomes.OLD_GROWTH_BIRCH_FOREST);
    private static final ResourceKey<Biome> DARK_FOREST = Replicas.keyOf(Biomes.DARK_FOREST);
    private static final ResourceKey<Biome> CHERRY_GROVE = Replicas.keyOf(Biomes.CHERRY_GROVE);
    private static final ResourceKey<Biome> TAIGA = Replicas.keyOf(Biomes.TAIGA);
    private static final ResourceKey<Biome> OLD_GROWTH_PINE_TAIGA = Replicas.keyOf(Biomes.OLD_GROWTH_PINE_TAIGA);
    private static final ResourceKey<Biome> OLD_GROWTH_SPRUCE_TAIGA = Replicas.keyOf(Biomes.OLD_GROWTH_SPRUCE_TAIGA);
    private static final ResourceKey<Biome> WINDSWEPT_HILLS = Replicas.keyOf(Biomes.WINDSWEPT_HILLS);
    private static final ResourceKey<Biome> WINDSWEPT_FOREST = Replicas.keyOf(Biomes.WINDSWEPT_FOREST);
    private static final ResourceKey<Biome> SNOWY_SLOPES = Replicas.keyOf(Biomes.SNOWY_SLOPES);
    private static final ResourceKey<Biome> JAGGED_PEAKS = Replicas.keyOf(Biomes.JAGGED_PEAKS);
    private static final ResourceKey<Biome> STONY_PEAKS = Replicas.keyOf(Biomes.STONY_PEAKS);
    public static final List<BiomeName> NAMES = List.of(
            new BiomeName(SUNFLOWER_PLAINS, "向日葵平原"),
            new BiomeName(MEADOW, "草甸"),
            new BiomeName(SWAMP, "沼泽"),
            new BiomeName(FOREST, "森林"),
            new BiomeName(FLOWER_FOREST, "繁花森林"),
            new BiomeName(BIRCH_FOREST, "桦木森林"),
            new BiomeName(OLD_GROWTH_BIRCH_FOREST, "原始桦木森林"),
            new BiomeName(DARK_FOREST, "黑森林"),
            new BiomeName(CHERRY_GROVE, "樱花树林"),
            new BiomeName(TAIGA, "针叶林"),
            new BiomeName(OLD_GROWTH_PINE_TAIGA, "原始松木针叶林"),
            new BiomeName(OLD_GROWTH_SPRUCE_TAIGA, "原始云杉针叶林"),
            new BiomeName(WINDSWEPT_HILLS, "风袭丘陵"),
            new BiomeName(WINDSWEPT_FOREST, "风袭森林"),
            new BiomeName(SNOWY_SLOPES, "积雪山坡"),
            new BiomeName(JAGGED_PEAKS, "尖峭山峰"),
            new BiomeName(STONY_PEAKS, "裸岩山峰"));
    private static final Map<ResourceKey<Biome>, List<ResourceKey<Biome>>> SOURCES = Map.ofEntries(
            Map.entry(Replicas.PLAINS, List.of(Biomes.PLAINS, Biomes.DESERT, Biomes.SAVANNA, Biomes.BADLANDS,
                    Biomes.ERODED_BADLANDS, Biomes.MUSHROOM_FIELDS, Biomes.BEACH, Biomes.SNOWY_BEACH,
                    Biomes.STONY_SHORE, Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN,
                    Biomes.DEEP_LUKEWARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN,
                    Biomes.DEEP_FROZEN_OCEAN)),
            Map.entry(SUNFLOWER_PLAINS, List.of(Biomes.SUNFLOWER_PLAINS)),
            Map.entry(MEADOW, List.of(Biomes.MEADOW, Biomes.SAVANNA_PLATEAU)),
            Map.entry(SWAMP, List.of(Biomes.SWAMP)),
            Map.entry(FOREST, List.of(Biomes.FOREST, Biomes.WOODED_BADLANDS, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE,
                    Biomes.BAMBOO_JUNGLE, Biomes.MANGROVE_SWAMP)),
            Map.entry(FLOWER_FOREST, List.of(Biomes.FLOWER_FOREST)),
            Map.entry(BIRCH_FOREST, List.of(Biomes.BIRCH_FOREST)),
            Map.entry(OLD_GROWTH_BIRCH_FOREST, List.of(Biomes.OLD_GROWTH_BIRCH_FOREST)),
            Map.entry(DARK_FOREST, List.of(Biomes.DARK_FOREST)),
            Map.entry(CHERRY_GROVE, List.of(Biomes.CHERRY_GROVE)),
            Map.entry(TAIGA, List.of(Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES)),
            Map.entry(OLD_GROWTH_PINE_TAIGA, List.of(Biomes.OLD_GROWTH_PINE_TAIGA)),
            Map.entry(OLD_GROWTH_SPRUCE_TAIGA, List.of(Biomes.OLD_GROWTH_SPRUCE_TAIGA)),
            Map.entry(WINDSWEPT_HILLS, List.of(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS,
                    Biomes.WINDSWEPT_SAVANNA)),
            Map.entry(WINDSWEPT_FOREST, List.of(Biomes.WINDSWEPT_FOREST)),
            Map.entry(Replicas.GROVE, List.of(Biomes.GROVE)),
            Map.entry(SNOWY_SLOPES, List.of(Biomes.SNOWY_SLOPES)),
            Map.entry(JAGGED_PEAKS, List.of(Biomes.JAGGED_PEAKS)),
            Map.entry(STONY_PEAKS, List.of(Biomes.STONY_PEAKS)),
            Map.entry(Replicas.FROZEN_PEAKS, List.of(Biomes.FROZEN_PEAKS)),
            Map.entry(Replicas.RIVER, List.of(Biomes.RIVER, Biomes.FROZEN_RIVER)),
            Map.entry(Replicas.LUSH_CAVES, List.of(Biomes.LUSH_CAVES)),
            Map.entry(Replicas.DRIPSTONE_CAVES, List.of(Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK)));
    public static final Set<ResourceKey<Biome>> ROSTER = getRoster();
    public static final Map<WildCropClimate, List<ResourceKey<Biome>>> CROPS = Map.of(
            WildCropClimate.TEMPERATE, List.of(Replicas.PLAINS, SUNFLOWER_PLAINS, MEADOW, FOREST, FLOWER_FOREST,
                    BIRCH_FOREST, OLD_GROWTH_BIRCH_FOREST, DARK_FOREST, CHERRY_GROVE),
            WildCropClimate.WET, List.of(SWAMP),
            WildCropClimate.COLD, List.of(TAIGA, OLD_GROWTH_PINE_TAIGA, OLD_GROWTH_SPRUCE_TAIGA, WINDSWEPT_HILLS,
                    WINDSWEPT_FOREST));

    private BlissfulLandBiomes() {}

    public static List<Replicas.Template> getSurfaceTemplates() {
        return Stream.concat(Replicas.SHARED.stream(), REPLICAS.stream()).toList();
    }

    public static List<Pair<Climate.ParameterPoint, Holder<Biome>>> getParameters(HolderGetter<Biome> biomes) {
        Map<ResourceKey<Biome>, ResourceKey<Biome>> targets = new HashMap<>();
        SOURCES.forEach((target, sources) -> sources.forEach(source -> {
            if (targets.put(source, target) != null) throw new IllegalStateException(source + " is mapped twice");
        }));
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters = new ArrayList<>();
        for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry : MultiNoiseBiomeSourceParameterList.knownPresets()
                .get(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD).values()) {
            ResourceKey<Biome> target = targets.get(entry.getSecond());
            if (target == null) throw new IllegalStateException("no blissful land biome for " + entry.getSecond());
            parameters.add(Pair.of(entry.getFirst(), biomes.getOrThrow(target)));
        }
        Climate.Parameter whole = Climate.Parameter.span(-PARAMETER_LIMIT, PARAMETER_LIMIT);
        Climate.Parameter ring = Climate.Parameter.span(RING_CONTINENTALNESS, PARAMETER_LIMIT);
        Climate.Parameter depth = Climate.Parameter.span(0.0F, RING_DEPTH);
        parameters.add(Pair.of(Climate.parameters(whole, whole, ring, whole, depth, whole, 0.0F),
                biomes.getOrThrow(FoxImmortalBlissfulLand.BIOME)));
        return parameters;
    }

    private static Set<ResourceKey<Biome>> getRoster() {
        Set<ResourceKey<Biome>> roster = new LinkedHashSet<>(SOURCES.keySet());
        roster.add(FoxImmortalBlissfulLand.BIOME);
        return Set.copyOf(roster);
    }
}
