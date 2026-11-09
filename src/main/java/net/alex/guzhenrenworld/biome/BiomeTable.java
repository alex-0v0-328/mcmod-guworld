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
 * A region's multi-noise table [群系表]: Terralith's overworld table as shipped ({@link Ports#getBiomeParameters}) with
 * every source biome mapped onto one of the region's own, by the groups the region lists (a roster biome to the source
 * biome names it stands in for). The terrain under it is the region's copy of Tectonic's, which remaps its own erosion
 * and ridges into the bands Terralith's table expects, so each parameter cell keeps the terrain archetype Terralith
 * gave it and gets the region's biome of its kind.
 *
 * <p>Terrain follows erosion, not the biome: every cell of Terralith's spire band, erosion {@link #SPIRE_EROSION} and
 * up, grows Tectonic's spire terrain whatever biome sits there. A table made with {@link #withSpires} turns a cell
 * there into its spire biome unless the cell's own biome is one it keeps; one made with {@link #of} leaves the cells
 * as the groups map them.
 *
 * <p>⚠ {@link #getBiome} throws on a source biome the groups do not map, so a re-port that adds one fails
 * {@code runData} instead of dropping cells; {@link #of} throws on a source biome two groups map.
 *
 * <p>Built only through {@link #of} and {@link #withSpires}, so every table has passed the duplicate check. A table
 * without spires has no {@code spireBiome}, which is null there.
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
