package net.alex.guzhenrenworld.dimension;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

/**
 * The Southern Border [南疆] biome table: which vanilla biome stands where, by altitude, temperature and humidity.
 *
 * <p>{@link #SURFACE} is indexed {@code [elevation][temperature][humidity]}. The six {@link #ELEVATIONS} bands
 * read {@code SouthernBorderTerrain}'s elevation, so in blocks they are plain floor below y 94, hills to 164,
 * mid slopes to 269, high slopes to 348, peaks to 383 and snowy summits above, the few highest points. The
 * three {@link #TEMPERATURES} are cold, mild and warm, cut so that, measured over eight kilometers, cold is a
 * few percent and warm about half the region, the south; the three {@link #HUMIDITIES} are dry, medium and
 * wet, cut so dry is about a tenth and wet over two fifths, since the region leans wet. Each surface biome goes
 * in twice per weirdness half, at depth 0 and 1, as vanilla adds its surface biomes, so the ground under a
 * biome keeps it.
 *
 * <p>Rivers take the narrow weirdness band {@link #RIVER_CHANNEL} on the two lowest bands only, where the
 * terrain carves its channels; every land entry stays outside that band, so the two never tie. Lush caves sit
 * under the wet third and dripstone caves under the dry third, between {@link #CAVE_DEPTH}.
 *
 * <p>⚠ The table is a first prototype for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see SouthernBorderTerrain
 * @since 1.0.0
 */

public final class SouthernBorderBiomes {

    private static final List<Climate.Parameter> ELEVATIONS = List.of(
            Climate.Parameter.span(-1.0F, -0.75F),
            Climate.Parameter.span(-0.75F, -0.35F),
            Climate.Parameter.span(-0.35F, 0.25F),
            Climate.Parameter.span(0.25F, 0.7F),
            Climate.Parameter.span(0.7F, 0.9F),
            Climate.Parameter.span(0.9F, 1.0F));
    private static final List<Climate.Parameter> TEMPERATURES = List.of(
            Climate.Parameter.span(-1.0F, -0.4F),
            Climate.Parameter.span(-0.4F, -0.03F),
            Climate.Parameter.span(-0.03F, 1.0F));
    private static final List<Climate.Parameter> HUMIDITIES = List.of(
            Climate.Parameter.span(-1.0F, -0.45F),
            Climate.Parameter.span(-0.45F, 0.0F),
            Climate.Parameter.span(0.0F, 1.0F));
    private static final List<List<List<ResourceKey<Biome>>>> SURFACE = List.of(
            List.of(
                    List.of(Biomes.TAIGA, Biomes.TAIGA, Biomes.TAIGA),
                    List.of(Biomes.PLAINS, Biomes.PLAINS, Biomes.SWAMP),
                    List.of(Biomes.SAVANNA, Biomes.SPARSE_JUNGLE, Biomes.JUNGLE)),
            List.of(
                    List.of(Biomes.TAIGA, Biomes.TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
                    List.of(Biomes.BIRCH_FOREST, Biomes.FOREST, Biomes.DARK_FOREST),
                    List.of(Biomes.SAVANNA_PLATEAU, Biomes.BAMBOO_JUNGLE, Biomes.JUNGLE)),
            List.of(
                    List.of(Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA),
                    List.of(Biomes.WINDSWEPT_FOREST, Biomes.FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST),
                    List.of(Biomes.WINDSWEPT_SAVANNA, Biomes.FOREST, Biomes.BAMBOO_JUNGLE)),
            List.of(
                    List.of(Biomes.GROVE, Biomes.GROVE, Biomes.GROVE),
                    List.of(Biomes.WINDSWEPT_HILLS, Biomes.CHERRY_GROVE, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
                    List.of(Biomes.WINDSWEPT_HILLS, Biomes.MEADOW, Biomes.OLD_GROWTH_SPRUCE_TAIGA)),
            List.of(
                    List.of(Biomes.SNOWY_SLOPES, Biomes.SNOWY_SLOPES, Biomes.SNOWY_SLOPES),
                    List.of(Biomes.STONY_PEAKS, Biomes.STONY_PEAKS, Biomes.STONY_PEAKS),
                    List.of(Biomes.STONY_PEAKS, Biomes.STONY_PEAKS, Biomes.STONY_PEAKS)),
            List.of(
                    List.of(Biomes.JAGGED_PEAKS, Biomes.JAGGED_PEAKS, Biomes.JAGGED_PEAKS),
                    List.of(Biomes.JAGGED_PEAKS, Biomes.JAGGED_PEAKS, Biomes.JAGGED_PEAKS),
                    List.of(Biomes.JAGGED_PEAKS, Biomes.JAGGED_PEAKS, Biomes.JAGGED_PEAKS)));
    private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0F, 1.0F);
    private static final Climate.Parameter RIVER_CHANNEL = Climate.Parameter.span(-0.012F, 0.012F);
    private static final List<Climate.Parameter> LAND_WEIRDNESS = List.of(
            Climate.Parameter.span(-1.0F, -0.012F),
            Climate.Parameter.span(0.012F, 1.0F));
    private static final Climate.Parameter LOWLANDS = Climate.Parameter.span(-1.0F, -0.35F);
    private static final Climate.Parameter CAVE_DEPTH = Climate.Parameter.span(0.2F, 0.9F);
    private static final float[] SURFACE_DEPTHS = { 0.0F, 1.0F };

    private SouthernBorderBiomes() {}

    public static Climate.ParameterList<Holder<Biome>> getParameters(HolderGetter<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries = new ArrayList<>();
        for (int elevation = 0; elevation < ELEVATIONS.size(); elevation++) {
            for (int temperature = 0; temperature < TEMPERATURES.size(); temperature++) {
                for (int humidity = 0; humidity < HUMIDITIES.size(); humidity++) {
                    Holder<Biome> biome = biomes.getOrThrow(SURFACE.get(elevation).get(temperature).get(humidity));
                    for (Climate.Parameter weirdness : LAND_WEIRDNESS) {
                        addSurface(entries, biome, TEMPERATURES.get(temperature), HUMIDITIES.get(humidity),
                                ELEVATIONS.get(elevation), weirdness);
                    }
                }
            }
        }
        addSurface(entries, biomes.getOrThrow(Biomes.RIVER), FULL_RANGE, FULL_RANGE, LOWLANDS, RIVER_CHANNEL);
        entries.add(Pair.of(Climate.parameters(FULL_RANGE, HUMIDITIES.getLast(), FULL_RANGE, FULL_RANGE, CAVE_DEPTH,
                FULL_RANGE, 0.0F), biomes.getOrThrow(Biomes.LUSH_CAVES)));
        entries.add(Pair.of(Climate.parameters(FULL_RANGE, HUMIDITIES.getFirst(), FULL_RANGE, FULL_RANGE, CAVE_DEPTH,
                FULL_RANGE, 0.0F), biomes.getOrThrow(Biomes.DRIPSTONE_CAVES)));
        return new Climate.ParameterList<>(entries);
    }

    private static void addSurface(List<Pair<Climate.ParameterPoint, Holder<Biome>>> entries, Holder<Biome> biome,
            Climate.Parameter temperature, Climate.Parameter humidity, Climate.Parameter elevation,
            Climate.Parameter weirdness) {
        for (float depth : SURFACE_DEPTHS) {
            entries.add(Pair.of(Climate.parameters(temperature, humidity, FULL_RANGE, elevation,
                    Climate.Parameter.point(depth), weirdness, 0.0F), biome));
        }
    }
}
