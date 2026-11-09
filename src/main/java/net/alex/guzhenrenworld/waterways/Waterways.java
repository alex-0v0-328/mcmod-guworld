package net.alex.guzhenrenworld.waterways;

import com.mojang.serialization.Codec;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.NotNull;

/**
 * Waterways [水系]: Gu World's own streams, planned once for each finite region from its terrain, following the plate
 * plan of Davis, "Plate-Local River Generation" (arXiv 2610.00301; reference code CC BY 4.0,
 * github.com/Davismk3/plate-local-river-generation), with the whole 8192-block region as one plate. This class holds
 * the numbers the planner, the index and the carving share; {@link WaterwaysPlanner} builds a {@link WaterwaysPlan}
 * per region, {@link WaterwaysIndex} turns its streams into segments, and {@link WaterwaysDensity}, the density
 * function a region wraps its final density in, carves them through {@link WaterwaysCarver}.
 *
 * <p>The plan samples the ground on a grid of {@link #CELL}-block cells, each sample jittered up to {@link #JITTER}
 * blocks so channels do not follow the grid. A cell carries a stream once its catchment reaches
 * {@link #MIN_CATCHMENT} cells; the stream's full width grows with the square root of the catchment, from
 * {@link #WIDTH_SCALE} blocks at the threshold up to {@link #MAX_WIDTH}. A closed depression up to
 * {@link #LAKE_DEPTH} blocks deep holds a lake on the plan; a deeper one ends the stream at a sink on its floor. The
 * Western Desert [西漠] is {@link Region#dry}: its streams are carved alike, but only the widest carry water, the rest
 * are dry gullies.
 *
 * <p>The water surface runs at least {@link #INCISION} blocks under the ground, the sea level standing in for the
 * ground under it, and never rises downstream ({@link WaterwaysProfile}, which reads the ground at {@link #KNOTS} + 1
 * knots along each segment and on both sides of it, skipping dips deeper than {@link #MAX_DIP}); shallow lakes on the
 * plan are cut through. The water over the bottom is a whole block deep across the channel and, at the centerline,
 * {@link #MIN_DEPTH} blocks at the narrowest and {@link #MAX_DEPTH} at {@link #MAX_WIDTH}. Banks
 * {@code max(}{@link #BANK_MIN}{@code , }{@link #BANK_FACTOR}{@code  × width)} blocks wide rise from the water surface
 * back to the ground, ending {@link #BANK_MARGIN} blocks above the scanned ground so its usual 1 to 2 blocks of error
 * leave no step. Every channel wanders up to {@link #MEANDER} blocks along each axis off its centerline, a warp read
 * from {@link #MEANDER_NOISE} ({@link #getMeanderX}, {@link #getMeanderZ}), whose instance also tells the carving
 * which level's random state it is filling. {@link #PARAMETERS} names the numbers a saved index depends on, so one
 * built with others is rebuilt ({@link WaterwaysData}).
 *
 * <p>{@link WaterwaysFeature} fills the channels with water and lays each region's {@link Bed} under it; in a dry
 * region only channels at least {@link #DRY_WIDTH} wide hold water.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class Waterways {

    public static final int CELL = 16;
    public static final int JITTER = 5;
    public static final int MIN_CATCHMENT = 32;
    public static final double WIDTH_SCALE = 1.5;
    public static final double MAX_WIDTH = 7.0;
    public static final double LAKE_DEPTH = 4.0;
    public static final double INCISION = 1.0;
    public static final double MIN_DEPTH = 1.0;
    public static final double MAX_DEPTH = 3.0;
    public static final double BANK_MIN = 5.0;
    public static final double BANK_FACTOR = 2.0;
    public static final double BANK_MARGIN = 3.0;
    public static final double MEANDER = 2.5;
    public static final int KNOTS = 3;
    public static final double MAX_DIP = 4.0;
    public static final double DRY_WIDTH = 5.0;
    public static final ResourceKey<NormalNoise.NoiseParameters> MEANDER_NOISE =
            GuWorld.key(Registries.NOISE, "waterways/meander");
    private static final int MEANDER_OCTAVE = -6;
    private static final double MEANDER_OFFSET = 4096.0;
    private static final double CLAY = 0.4;
    public static final String PARAMETERS = String.join("/", "v3", String.valueOf(CELL), String.valueOf(JITTER),
            String.valueOf(MIN_CATCHMENT), String.valueOf(WIDTH_SCALE), String.valueOf(MAX_WIDTH),
            String.valueOf(LAKE_DEPTH), String.valueOf(INCISION), String.valueOf(MEANDER),
            String.valueOf(MEANDER_OCTAVE), String.valueOf(KNOTS), String.valueOf(MAX_DIP));

    private Waterways() {}

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        context.register(MEANDER_NOISE, new NormalNoise.NoiseParameters(MEANDER_OCTAVE, 1.0, 1.0));
    }

    public static double getDepth(double width) {
        double share = Math.clamp((width - WIDTH_SCALE) / (MAX_WIDTH - WIDTH_SCALE), 0.0, 1.0);
        return MIN_DEPTH + (MAX_DEPTH - MIN_DEPTH) * share;
    }

    public static double getBank(double width) {
        return Math.max(BANK_MIN, BANK_FACTOR * width);
    }

    public static double getMeanderX(NormalNoise meander, double x, double z) {
        return MEANDER * Math.clamp(meander.getValue(x, 0, z), -1.0, 1.0);
    }

    public static double getMeanderZ(NormalNoise meander, double x, double z) {
        return MEANDER * Math.clamp(meander.getValue(x, MEANDER_OFFSET, z), -1.0, 1.0);
    }

    public enum Bed implements StringRepresentable {
        GRAVEL_CLAY("gravel_clay"),
        GRAVEL("gravel"),
        SAND("sand");

        public static final Codec<Bed> CODEC = StringRepresentable.fromEnum(Bed::values);

        private final String name;

        Bed(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }

        public BlockState getBlock(NormalNoise meander, int x, int z) {
            return switch (this) {
                case GRAVEL_CLAY -> meander.getValue(x, 2 * MEANDER_OFFSET, z) > CLAY
                        ? Blocks.CLAY.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
                case GRAVEL -> Blocks.GRAVEL.defaultBlockState();
                case SAND -> Blocks.SAND.defaultBlockState();
            };
        }
    }

    public record Region(String name, double borderSize, boolean dry, Bed bed) {

        public int cells() {
            return (int) (borderSize / CELL);
        }

        public int origin() {
            return (int) (-borderSize / 2);
        }
    }
}
