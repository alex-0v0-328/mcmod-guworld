package net.alex.guzhenrenworld.dimension;

import java.util.ArrayList;
import java.util.List;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

/**
 * The Southern Border [南疆] surface rules: rock that shows what each landform is made of, then the surfaces of the
 * biomes ported from Terralith ({@code Ports}), then vanilla's overworld rules with Gu World's replicas added beside
 * the biomes they copy ({@code Replicas}), which cover everything else.
 *
 * <p>The landform rules run only inside the landform zones, gated by the zone noise itself ({@link #getRules}): a
 * surface rule's noise condition reads the same noise the terrain does, once per column, so the rest of the region
 * never pays for a biome lookup per block. Inside, the landform type noise picks the rock by the same cuts the terrain
 * uses ({@link #getType}), not the biome: the game frays every biome border, so rock chosen by biome comes out as a
 * mosaic of two rocks on a wall where two landforms meet. They run only in a dry column: a riverbed or lake floor
 * inside a zone falls through to the ported and vanilla rules and gets their bed of sand, gravel and clay, while a
 * wall beside the water has no water above it and keeps its rock. Above y {@link #ROCK_BOTTOM}:
 * <ul>
 *   <li>Every landform keeps grass on top and dirt under it, cliff tops and ledges included.</li>
 *   <li>Danxia and the Zhangshiyan cliffs are red beds: horizontal bands of terracotta and red sandstone from the
 *   foot to the top ({@link #DANXIA_BANDS}, {@link #CLIFF_BANDS}), each band 2..6 blocks thick and lifted by the
 *   column's surface depth, so the bands wave a little along a wall.</li>
 *   <li>The sandstone pillars are grey bedding ({@link #PILLAR_BANDS}) cut by vertical andesite joints.</li>
 *   <li>The karst towers are stone streaked from top to bottom with calcite and andesite, the way water stains
 *   limestone: a 2D noise condition holds for a whole column.</li>
 * </ul>
 *
 * <p>⚠ Block choices and bands are a first prototype for Alex to judge in game, not a settled pick.
 *
 * @author Alex
 * @version 1.0.0
 * @see SouthernBorderBiomes
 * @since 1.0.0
 */

final class SouthernBorderSurface {

    private static final int ROCK_BOTTOM = 48;
    private static final int ROCK_TOP = (int) SouthernBorderLandforms.PEAK_Y;
    private static final int[] BAND_THICKNESS = { 3, 2, 4, 2, 5, 3, 2, 6, 2, 4, 3, 5 };
    private static final List<Block> DANXIA_BANDS = List.of(Blocks.RED_TERRACOTTA, Blocks.RED_SANDSTONE,
            Blocks.ORANGE_TERRACOTTA, Blocks.RED_SANDSTONE, Blocks.TERRACOTTA, Blocks.RED_TERRACOTTA,
            Blocks.RED_SANDSTONE, Blocks.ORANGE_TERRACOTTA);
    private static final List<Block> CLIFF_BANDS = List.of(Blocks.RED_SANDSTONE, Blocks.ORANGE_TERRACOTTA,
            Blocks.RED_SANDSTONE, Blocks.RED_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.TERRACOTTA);
    private static final List<Block> PILLAR_BANDS = List.of(Blocks.STONE, Blocks.ANDESITE,
            Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.STONE, Blocks.TUFF, Blocks.ANDESITE, Blocks.STONE);

    private SouthernBorderSurface() {}

    static SurfaceRules.RuleSource getRules() {
        float[] cuts = SouthernBorderLandforms.TYPE_CUTS;
        SurfaceRules.RuleSource landforms = SurfaceRules.sequence(
                getTopsoil(),
                SurfaceRules.ifTrue(getType(cuts[0], cuts[1]), getBands(DANXIA_BANDS)),
                SurfaceRules.ifTrue(getType(cuts[2], Double.MAX_VALUE), getBands(CLIFF_BANDS)),
                SurfaceRules.ifTrue(getType(cuts[1], cuts[2]),
                        SurfaceRules.sequence(getStreaks(-0.02, 0.02, Blocks.ANDESITE), getBands(PILLAR_BANDS))),
                SurfaceRules.ifTrue(getType(-Double.MAX_VALUE, cuts[0]),
                        SurfaceRules.sequence(getStreaks(-0.0125, 0.0125, Blocks.CALCITE),
                                getStreaks(0.25, 0.3, Blocks.ANDESITE))));
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(SouthernBorderLandforms.ZONE_NOISE,
                                SouthernBorderLandforms.ZONE_MIDDLE - SouthernBorderLandforms.ZONE_HALF_WIDTH),
                        SurfaceRules.ifTrue(getAbove(ROCK_BOTTOM),
                                SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), landforms))),
                Ports.getSurfaceRules(),
                Replicas.getSurfaceRules());
    }

    private static SurfaceRules.RuleSource getTopsoil() {
        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, getState(Blocks.GRASS_BLOCK)),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, getState(Blocks.DIRT)));
    }

    private static SurfaceRules.ConditionSource getType(double low, double high) {
        return SurfaceRules.noiseCondition(SouthernBorderLandforms.TYPE_NOISE, low, high);
    }

    private static SurfaceRules.RuleSource getStreaks(double low, double high, Block block) {
        return SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, low, high), getState(block));
    }

    private static SurfaceRules.RuleSource getBands(List<Block> blocks) {
        List<SurfaceRules.RuleSource> bands = new ArrayList<>();
        int bottom = ROCK_TOP;
        for (int index = 0; bottom > ROCK_BOTTOM; index++) {
            bottom -= BAND_THICKNESS[index % BAND_THICKNESS.length];
            bands.add(SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(bottom), 1),
                    getState(blocks.get(index % blocks.size()))));
        }
        return SurfaceRules.sequence(bands.toArray(new SurfaceRules.RuleSource[0]));
    }

    private static SurfaceRules.ConditionSource getAbove(int y) {
        return SurfaceRules.yBlockCheck(VerticalAnchor.absolute(y), 0);
    }

    private static SurfaceRules.RuleSource getState(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}
