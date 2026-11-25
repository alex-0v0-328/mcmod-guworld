package net.alex.guzhenrenworld.biome;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.Nullable;

/**
 * 域的多噪声群系映射表：基于 Terralith 主世界多噪声参数表（{@link Ports#getBiomeParameters}），将来源群系按分组映射为本
 * 域群系。
 * <ul>
 *   <li>各域定义分组映射表，指明每个本域群系替代哪些来源群系；由于域地形使用 Tectonic 副本并将侵蚀度与山脊值重映射到
 *       Terralith 的分段区间，每个气候参数格都能保留原有的地形原型并替换为本域对应的同类群系。</li>
 *   <li>地形形态取决于侵蚀度而非群系本身：Terralith 尖柱带（侵蚀度 ≥ {@link #SPIRE_EROSION}）在 Tectonic 下固定生成尖
 *       柱地形；{@link #withSpires} 会将该区间内除保留名单外的参数格统一替换为指定的尖柱群系，{@link #of} 则仅按分组原
 *       样映射。</li>
 *   <li>实例仅通过 {@link #of} 与 {@link #withSpires} 构建，构建时通过 {@link #getSources} 校验分组映射并转换为不可变
 *       表；不设尖柱群系的表其 {@code spireBiome} 为 {@code null}。</li>
 *   <li>⚠ {@link #getBiome} 遇到未映射的来源群系、或构建时遇到重复映射的来源群系都会直接抛出异常，使新增移植群系在
 *       {@code runData} 阶段显式失败，防止静默漏格。</li>
 * </ul>
 */

public final class BiomeTable {

    public static final float SPIRE_EROSION = 0.565F;

    private final String region;
    private final Map<ResourceLocation, ResourceKey<Biome>> sources;
    private final Set<ResourceKey<Biome>> kept;
    private final @Nullable ResourceKey<Biome> spireBiome;

    private BiomeTable(String region, Map<ResourceLocation, ResourceKey<Biome>> sources,
            Set<ResourceKey<Biome>> kept, @Nullable ResourceKey<Biome> spireBiome) {
        this.region = region;
        this.sources = sources;
        this.kept = kept;
        this.spireBiome = spireBiome;
    }

    public static BiomeTable of(String region, Map<ResourceKey<Biome>, List<String>> groups) {
        return new BiomeTable(region, getSources(groups), Set.of(), null);
    }

    public static BiomeTable withSpires(String region, Map<ResourceKey<Biome>, List<String>> groups,
            Set<ResourceKey<Biome>> kept, ResourceKey<Biome> spireBiome) {
        return new BiomeTable(region, getSources(groups), kept, spireBiome);
    }

    public ResourceKey<Biome> getBiome(Climate.ParameterPoint point, ResourceLocation source) {
        ResourceKey<Biome> biome = sources.get(source);
        if (biome == null) throw new IllegalStateException("no " + region + " biome for " + source);
        boolean spireCell = point.erosion().min() >= Climate.quantizeCoord(SPIRE_EROSION);
        return spireCell && spireBiome != null && !kept.contains(biome) ? spireBiome : biome;
    }

    public List<Pair<Climate.ParameterPoint, Holder<Biome>>> getParameters(HolderGetter<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> parameters = new ArrayList<>();
        for (Map.Entry<Climate.ParameterPoint, ResourceLocation> entry : Ports.getBiomeParameters()) {
            parameters.add(Pair.of(entry.getKey(), biomes.getOrThrow(getBiome(entry.getKey(), entry.getValue()))));
        }
        return parameters;
    }

    private static Map<ResourceLocation, ResourceKey<Biome>> getSources(
            Map<ResourceKey<Biome>, List<String>> groups) {
        Map<ResourceLocation, ResourceKey<Biome>> sources = new HashMap<>();
        groups.forEach((biome, names) -> names.forEach(name -> {
            if (sources.put(ResourceLocation.parse(name), biome) != null) {
                throw new IllegalStateException(name + " is mapped twice");
            }
        }));
        return Map.copyOf(sources);
    }
}
