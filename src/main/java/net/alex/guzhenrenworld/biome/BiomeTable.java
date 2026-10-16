package net.alex.guzhenrenworld.biome;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.Nullable;

/**
 * The biome placement every Gu World region shares, Terralith's logic on vanilla's skeleton: the region's relief
 * splits into bands, each band a terrain archetype with its own family of biomes, and inside a band the climate picks
 * the biome from a five-by-five grid of vanilla's temperature and humidity bands ({@link #TEMPERATURES},
 * {@link #HUMIDITIES}).
 *
 * <p>The region feeds five 2D fields to the biome source: temperature, humidity, its own relief axis as erosion,
 * a landform code as continentalness, and a signed weirdness. A grid {@link Cell} holds up to three biomes, picked by
 * the weirdness: the normal one where it is negative, the variant where it is positive, and the rare one, if any,
 * where it passes {@link #RARE_WEIRDNESS}, so a variant noise splits each climate patch into neighbors and the rare
 * biome shows on the far tops of that noise. Rivers take the narrow weirdness band within the region's river half-width
 * of zero, and no land entry reaches into it, so the two never tie.
 *
 * <p>Each surface entry goes in at depth 0 and 1, as vanilla adds its surface biomes, so the ground under a biome
 * keeps it. Biomes that hold a fixed band of the landform code ({@link #addFixed}) cover the whole depth span 0..1:
 * depth is measured straight down from the 2D surface, so the face of a tall cliff reads as cave depth, and a cave
 * entry a mere code band away would otherwise take its walls. Caves ({@link #addCave}) sit under ordinary ground only.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class BiomeTable {

    public static final List<Climate.Parameter> TEMPERATURES = getBands(-0.45F, -0.15F, 0.2F, 0.55F);
    public static final List<Climate.Parameter> HUMIDITIES = getBands(-0.35F, -0.1F, 0.1F, 0.3F);
    public static final float RARE_WEIRDNESS = 0.85F;
    private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0F, 1.0F);
    private static final Climate.Parameter FIXED_DEPTH = Climate.Parameter.span(0.0F, 1.0F);
    private static final float[] SURFACE_DEPTHS = { 0.0F, 1.0F };
    private final List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> entries = new ArrayList<>();
    private final Climate.Parameter ordinary;
    private final float river;

    public BiomeTable(Climate.Parameter ordinary, float river) {
        this.ordinary = ordinary;
        this.river = river;
    }

    public record Cell(ResourceKey<Biome> normal, ResourceKey<Biome> variant, @Nullable ResourceKey<Biome> rare) {

        public static Cell of(ResourceKey<Biome> normal, ResourceKey<Biome> variant) {
            return new Cell(normal, variant, null);
        }

        public static Cell of(ResourceKey<Biome> both) {
            return new Cell(both, both, null);
        }
    }

    public BiomeTable addBand(Climate.Parameter relief, List<List<Cell>> grid) {
        for (int temperature = 0; temperature < TEMPERATURES.size(); temperature++) {
            for (int humidity = 0; humidity < HUMIDITIES.size(); humidity++) {
                Cell cell = grid.get(temperature).get(humidity);
                Climate.Parameter heat = TEMPERATURES.get(temperature);
                Climate.Parameter wet = HUMIDITIES.get(humidity);
                addSurface(cell.normal(), heat, wet, relief, Climate.Parameter.span(-1.0F, -river));
                float variantTop = cell.rare() == null ? 1.0F : RARE_WEIRDNESS;
                addSurface(cell.variant(), heat, wet, relief, Climate.Parameter.span(river, variantTop));
                if (cell.rare() != null) {
                    addSurface(cell.rare(), heat, wet, relief, Climate.Parameter.span(RARE_WEIRDNESS, 1.0F));
                }
            }
        }
        return this;
    }

    public BiomeTable addFixed(ResourceKey<Biome> biome, Climate.Parameter landform) {
        for (Climate.Parameter weirdness : getLandWeirdness()) {
            entries.add(Pair.of(Climate.parameters(FULL_RANGE, FULL_RANGE, landform, FULL_RANGE, FIXED_DEPTH,
                    weirdness, 0.0F), biome));
        }
        return this;
    }

    public BiomeTable addRiver(ResourceKey<Biome> biome, Climate.Parameter relief) {
        for (float depth : SURFACE_DEPTHS) {
            entries.add(Pair.of(Climate.parameters(FULL_RANGE, FULL_RANGE, FULL_RANGE, relief,
                    Climate.Parameter.point(depth), Climate.Parameter.span(-river, river), 0.0F), biome));
        }
        return this;
    }

    public BiomeTable addCave(ResourceKey<Biome> biome, Climate.Parameter humidity, Climate.Parameter depth) {
        entries.add(Pair.of(Climate.parameters(FULL_RANGE, humidity, ordinary, FULL_RANGE, depth, FULL_RANGE, 0.0F),
                biome));
        return this;
    }

    public Climate.ParameterList<Holder<Biome>> build(HolderGetter<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> resolved = new ArrayList<>();
        for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry : entries) {
            resolved.add(Pair.of(entry.getFirst(), biomes.getOrThrow(entry.getSecond())));
        }
        return new Climate.ParameterList<>(resolved);
    }

    private void addSurface(ResourceKey<Biome> biome, Climate.Parameter temperature, Climate.Parameter humidity,
            Climate.Parameter relief, Climate.Parameter weirdness) {
        for (float depth : SURFACE_DEPTHS) {
            entries.add(Pair.of(Climate.parameters(temperature, humidity, ordinary, relief,
                    Climate.Parameter.point(depth), weirdness, 0.0F), biome));
        }
    }

    private List<Climate.Parameter> getLandWeirdness() {
        return List.of(Climate.Parameter.span(-1.0F, -river), Climate.Parameter.span(river, 1.0F));
    }

    private static List<Climate.Parameter> getBands(float... cuts) {
        List<Climate.Parameter> bands = new ArrayList<>();
        float low = -1.0F;
        for (float cut : cuts) {
            bands.add(Climate.Parameter.span(low, cut));
            low = cut;
        }
        bands.add(Climate.Parameter.span(low, 1.0F));
        return List.copyOf(bands);
    }
}
