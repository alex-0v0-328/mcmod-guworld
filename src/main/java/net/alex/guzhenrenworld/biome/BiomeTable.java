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
 * 域的群系表：原样的 Terralith 主世界多噪声表（{@link Ports#getBiomeParameters}），来源群系按组映射到本域。
 * <ul>
 *   <li>组由域列出：名单群系对应它代替的来源群系名；地形是本域的 Tectonic 副本，侵蚀、山脊重映射到 Terralith 表的分段，
 *       故每个参数格保留 Terralith 的地形原型、得到本域同类群系。</li>
 *   <li>地形随侵蚀不随群系：Terralith 尖柱带（侵蚀 ≥ {@link #SPIRE_EROSION}）每格都长 Tectonic 尖柱地形；
 *       {@link #withSpires} 把该带的格改成尖柱群系（保留群系除外），{@link #of} 照组映射。</li>
 *   <li>⚠ {@link #getBiome} 遇未映射来源抛错，新移植群系令 {@code runData} 失败而非丢格；{@link #of} 遇重复映射抛
 *       错。</li>
 *   <li>只经 {@link #of}、{@link #withSpires} 构建，每张表都过了重复检查；无尖柱的表 {@code spireBiome} 为 null。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Ports#getBiomeParameters
 * @since 1.0.0
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
