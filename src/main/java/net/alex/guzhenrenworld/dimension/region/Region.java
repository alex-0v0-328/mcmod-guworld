package net.alex.guzhenrenworld.dimension.region;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.BiomeName;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.biome.WildCropClimate;
import net.alex.guzhenrenworld.waterways.Waterways;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * One of the four regions beyond the Central Continent [中州], which is the overworld [主世界]: everything that
 * differs between the Southern Border [南疆], the Western Desert [西漠], the Eastern Sea [东海] and the Northern
 * Plains [北原] as plain values, so {@link RegionBootstraps} registers all four the same way. Each region's package
 * holds one instance ({@code SouthernBorder#REGION} and its siblings) beside the region's own prose.
 *
 * <p>Every key a region owns shares the one location {@code guworld:<name>} ({@link #name} is its path and the prefix
 * of the region's copy of the ported terrain); {@link #noiseSettings}, {@link #levelStem} and {@link #level} build
 * them. The dimension type is the one all four share, {@link #DIMENSION_TYPE}, {@code guworld:region}, since the
 * regions differ in nothing a dimension type holds. All four regions share the height range ({@link #MIN_Y} up to
 * {@link #TOP_Y}, {@link #HEIGHT} blocks) and the square world border around 0, 0 that
 * {@link net.alex.guzhenrenworld.dimension.RegionBorders} applies, {@link #BORDER_SIZE} blocks, eight kilometers
 * (about 262,000 chunks).
 *
 * <p>⚠ Every key shares the location of Guzhenren's level key of the same name in
 * {@code net.alex.guzhenren.registry.world.ModDimensions}, the admin travel there (Guzhenren owns travel):
 * {@code /guworld enter guworld:<name>} lands players on the surface above 0, 0. A renamed key here leaves that
 * command pointing at nothing until that key is renamed too.
 *
 * @param name     the path of every key, and the region's prefix in the ported terrain
 * @param title    the dimension's display name
 * @param seaLevel the noise settings' sea level
 * @param dry      whether only the widest waterways [水系] hold water ({@link Waterways.Region#dry})
 * @param bed      the block under the waterways' water
 * @param biomes   the biomes the region places
 * @param hooks    how the region fills the hooks of its copy of the ported terrain
 * @author Alex
 * @version 1.0.0
 * @see RegionBootstraps
 * @since 1.0.0
 */

public record Region(String name, Title title, int seaLevel, boolean dry, Waterways.Bed bed, Biomes biomes,
        Hooks hooks) {

    public static final double BORDER_SIZE = 8192.0;
    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    public static final int TOP_Y = MIN_Y + HEIGHT;
    public static final ResourceKey<DimensionType> DIMENSION_TYPE =
            GuWorld.key(Registries.DIMENSION_TYPE, "region");

    public ResourceKey<NoiseGeneratorSettings> noiseSettings() {
        return GuWorld.key(Registries.NOISE_SETTINGS, name);
    }

    public ResourceKey<LevelStem> levelStem() {
        return GuWorld.key(Registries.LEVEL_STEM, name);
    }

    public ResourceKey<Level> level() {
        return GuWorld.key(Registries.DIMENSION, name);
    }

    public Waterways.Region waterways() {
        return new Waterways.Region(name, BORDER_SIZE, dry, bed);
    }

    /**
     * A dimension's display name in English and Simplified Chinese.
     */
    public record Title(String english, String simplified) {}

    /**
     * The biomes a region places: the vanilla replicas it adds beside {@link Replicas#SHARED}, the Terralith biomes
     * it {@link #ported} beside {@link Ports#BIOMES}, {@link #roster}, every biome its table can place, {@link
     * #land}, the surface biomes of it but the river, where the spirit spring [元泉] may stand, {@link #names}, the
     * display names of the biomes only it places, {@link #crops}, its land sorted into the wild crop climates of
     * Farmer's Delight, and {@link #parameters}, the table itself.
     */
    public record Biomes(List<Replicas.Template> replicas, List<ResourceKey<Biome>> ported,
            Set<ResourceKey<Biome>> roster, List<ResourceKey<Biome>> land, List<BiomeName> names,
            Map<WildCropClimate, List<ResourceKey<Biome>>> crops,
            Function<HolderGetter<Biome>, List<Pair<Climate.ParameterPoint, Holder<Biome>>>> parameters) {}

    /**
     * What a region puts in the hooks of its copy of the ported router ({@link RegionBootstraps}): {@link
     * #oceanOffset}, {@link #verticalScale}, {@link #dunes}, and {@link #reshape}, which rewrites a ported function by
     * its name ({@link #AS_PORTED} leaves them all).
     */
    public record Hooks(DensityFunction oceanOffset, double verticalScale, double dunes,
            BiFunction<String, JsonElement, JsonElement> reshape) {

        public static final BiFunction<String, JsonElement, JsonElement> AS_PORTED = (name, function) -> function;
    }
}
