package net.alex.guzhenrenworld.waterways;

import java.util.function.DoubleUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.Nullable;

/**
 * A region's waterways [水系] as straight segments, one per stream cell of its {@link WaterwaysPlan}, from the cell's
 * point to its receiver's, with the full channel width at each end and the water surface at each knot, bucketed on
 * the plan's {@link Waterways#CELL}-block grid for the carving to find the segments near a column in one array read.
 *
 * <p>A segment's end width is its receiver's when the receiver carries a stream too, its own otherwise; its water
 * surface at its {@link Waterways#KNOTS} + 1 knots comes from the {@link WaterwaysProfile}, linear between them. A
 * bucket lists every segment whose channel, banks and meander can reach into it. A saved index with a value that is
 * not finite fails to load and is rebuilt.
 *
 * <p>{@link #getSurface} is the carved surface at a point: the lowest over the segments near it of the bottom inside
 * a channel or, across the banks, the water surface eased back up (smoothstep) to the ground plus
 * {@link Waterways#BANK_MARGIN}. The bottom sits at the bottom of the top water block (one under the water surface
 * rounded up) at the channel's edges, so even the edges hold one block of water, and deepens as a parabola to
 * {@link Waterways#getDepth} blocks at the centerline; the banks start at the water surface, a step of up to one block
 * above the edge. Taking the lowest, not the paper's inverse-distance blend (eq. 31), keeps every confluence open.
 * {@link #getChannel} is the channel a point lies in, with the lowest bottom there; in a dry region only a channel at
 * least {@link Waterways#DRY_WIDTH} wide is wet, and where channels meet, the lowest water surface of the wet ones
 * holds. The segments go into a world's saved data ({@link #save}, {@link #load}); the buckets are rebuilt.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WaterwaysIndex {

    private static final String[] FIELDS = Stream.concat(Stream.of("x0", "z0", "x1", "z1", "width0", "width1"),
            IntStream.rangeClosed(0, Waterways.KNOTS).mapToObj(knot -> "level" + knot)).toArray(String[]::new);
    private static final int LEVELS = 6;

    private final Waterways.Region region;
    private final float[][] fields;
    private final int segments;
    private final int size;
    private final int[] start;
    private final int[] members;

    private WaterwaysIndex(Waterways.Region region, float[][] fields) {
        this.region = region;
        this.fields = fields;
        this.segments = fields[0].length;
        this.size = region.cells();
        this.start = new int[size * size + 1];
        int[] reach = new int[segments * 4];
        for (int segment = 0; segment < segments; segment++) {
            double margin = getReach(segment);
            reach[segment * 4] = bucketOf(Math.min(x0(segment), x1(segment)) - margin);
            reach[segment * 4 + 1] = bucketOf(Math.max(x0(segment), x1(segment)) + margin);
            reach[segment * 4 + 2] = bucketOf(Math.min(z0(segment), z1(segment)) - margin);
            reach[segment * 4 + 3] = bucketOf(Math.max(z0(segment), z1(segment)) + margin);
            for (int row = reach[segment * 4 + 2]; row <= reach[segment * 4 + 3]; row++) {
                for (int column = reach[segment * 4]; column <= reach[segment * 4 + 1]; column++) {
                    start[row * size + column + 1]++;
                }
            }
        }
        for (int bucket = 0; bucket < size * size; bucket++) {
            start[bucket + 1] += start[bucket];
        }
        this.members = new int[start[size * size]];
        int[] next = start.clone();
        for (int segment = 0; segment < segments; segment++) {
            for (int row = reach[segment * 4 + 2]; row <= reach[segment * 4 + 3]; row++) {
                for (int column = reach[segment * 4]; column <= reach[segment * 4 + 1]; column++) {
                    members[next[row * size + column]++] = segment;
                }
            }
        }
    }

    public static WaterwaysIndex of(WaterwaysPlan plan, GroundHeights ground, NormalNoise meander) {
        WaterwaysProfile profile = WaterwaysProfile.of(plan, ground, meander);
        int count = 0;
        for (int cell = 0; cell < plan.count(); cell++) {
            if (isSegment(plan, cell)) count++;
        }
        float[][] fields = new float[FIELDS.length][count];
        int segment = 0;
        for (int cell = 0; cell < plan.count(); cell++) {
            if (!isSegment(plan, cell)) continue;
            int receiver = plan.receiver(cell);
            fields[0][segment] = plan.x(cell);
            fields[1][segment] = plan.z(cell);
            fields[2][segment] = plan.x(receiver);
            fields[3][segment] = plan.z(receiver);
            fields[4][segment] = (float) plan.width(cell, Waterways.MIN_CATCHMENT);
            fields[5][segment] = (float) getEndWidth(plan, cell);
            for (int knot = 0; knot <= Waterways.KNOTS; knot++) {
                fields[LEVELS + knot][segment] = profile.level(cell, knot);
            }
            segment++;
        }
        return new WaterwaysIndex(plan.region(), fields);
    }

    public static WaterwaysIndex empty(Waterways.Region region) {
        return new WaterwaysIndex(region, new float[FIELDS.length][0]);
    }

    public static WaterwaysIndex load(CompoundTag tag, Waterways.Region region) {
        float[][] fields = new float[FIELDS.length][];
        for (int field = 0; field < FIELDS.length; field++) {
            int[] bits = tag.getIntArray(FIELDS[field]);
            fields[field] = new float[bits.length];
            for (int segment = 0; segment < bits.length; segment++) {
                fields[field][segment] = Float.intBitsToFloat(bits[segment]);
                if (!Float.isFinite(fields[field][segment])) {
                    throw new IllegalArgumentException("waterways index value");
                }
            }
            if (bits.length != fields[0].length) throw new IllegalArgumentException("ragged waterways index");
        }
        return new WaterwaysIndex(region, fields);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (int field = 0; field < FIELDS.length; field++) {
            int[] bits = new int[segments];
            for (int segment = 0; segment < segments; segment++) {
                bits[segment] = Float.floatToRawIntBits(fields[field][segment]);
            }
            tag.putIntArray(FIELDS[field], bits);
        }
        return tag;
    }

    public Waterways.Region region() {
        return region;
    }

    public int segments() {
        return segments;
    }

    public float x0(int segment) {
        return fields[0][segment];
    }

    public float z0(int segment) {
        return fields[1][segment];
    }

    public float x1(int segment) {
        return fields[2][segment];
    }

    public float z1(int segment) {
        return fields[3][segment];
    }

    public float width0(int segment) {
        return fields[4][segment];
    }

    public float width1(int segment) {
        return fields[5][segment];
    }

    public float level(int segment, int knot) {
        return fields[LEVELS + knot][segment];
    }

    public int getBucket(int x, int z) {
        int column = Math.floorDiv(x - region.origin(), Waterways.CELL);
        int row = Math.floorDiv(z - region.origin(), Waterways.CELL);
        if (column < 0 || row < 0 || column >= size || row >= size) return -1;
        int bucket = row * size + column;
        return start[bucket] == start[bucket + 1] ? -1 : bucket;
    }

    public double getSurface(int bucket, double x, double z, DoubleUnaryOperator ground) {
        double surface = Double.POSITIVE_INFINITY;
        double groundHeight = Double.NaN;
        for (int member = start[bucket]; member < start[bucket + 1]; member++) {
            int segment = members[member];
            double along = getAlong(segment, x, z);
            double distance = getDistance(segment, along, x, z);
            double width = width0(segment) + along * (width1(segment) - width0(segment));
            double half = width / 2;
            double bank = Waterways.getBank(width);
            if (distance >= half + bank) continue;
            double level = getLevel(segment, along);
            double target;
            if (distance <= half) {
                target = getBottom(level, width, distance / half);
            } else {
                if (Double.isNaN(groundHeight)) groundHeight = ground.applyAsDouble(level + Waterways.INCISION);
                double share = (distance - half) / bank;
                double rise = share * share * (3 - 2 * share);
                target = level + rise * (groundHeight + Waterways.BANK_MARGIN - level);
            }
            surface = Math.min(surface, target);
        }
        return surface;
    }

    public @Nullable Channel getChannel(int bucket, double x, double z) {
        double bottom = Double.POSITIVE_INFINITY;
        double wetLevel = Double.POSITIVE_INFINITY;
        double dryLevel = Double.POSITIVE_INFINITY;
        for (int member = start[bucket]; member < start[bucket + 1]; member++) {
            int segment = members[member];
            double along = getAlong(segment, x, z);
            double width = width0(segment) + along * (width1(segment) - width0(segment));
            double distance = getDistance(segment, along, x, z);
            if (distance > width / 2) continue;
            double level = getLevel(segment, along);
            bottom = Math.min(bottom, getBottom(level, width, distance / (width / 2)));
            if (!region.dry() || width >= Waterways.DRY_WIDTH) {
                wetLevel = Math.min(wetLevel, level);
            } else {
                dryLevel = Math.min(dryLevel, level);
            }
        }
        if (bottom == Double.POSITIVE_INFINITY) return null;
        boolean wet = wetLevel != Double.POSITIVE_INFINITY;
        return new Channel(wet ? wetLevel : dryLevel, bottom, wet);
    }

    private static double getBottom(double level, double width, double share) {
        return Math.ceil(level) - 1 - (Waterways.getDepth(width) - 1) * (1 - share * share);
    }

    private double getLevel(int segment, double along) {
        double scaled = along * Waterways.KNOTS;
        int knot = Math.min((int) scaled, Waterways.KNOTS - 1);
        double from = level(segment, knot);
        return from + (scaled - knot) * (level(segment, knot + 1) - from);
    }

    private double getAlong(int segment, double x, double z) {
        double dx = x1(segment) - x0(segment);
        double dz = z1(segment) - z0(segment);
        double lengthSquared = dx * dx + dz * dz;
        return lengthSquared == 0 ? 0
                : Math.clamp(((x - x0(segment)) * dx + (z - z0(segment)) * dz) / lengthSquared, 0.0, 1.0);
    }

    private double getDistance(int segment, double along, double x, double z) {
        double offsetX = x - x0(segment) - along * (x1(segment) - x0(segment));
        double offsetZ = z - z0(segment) - along * (z1(segment) - z0(segment));
        return Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
    }

    private double getReach(int segment) {
        double width = Math.max(width0(segment), width1(segment));
        return width / 2 + Waterways.getBank(width) + Waterways.MEANDER + 1;
    }

    private int bucketOf(double coordinate) {
        return Math.clamp(Math.floorDiv((int) Math.floor(coordinate) - region.origin(), Waterways.CELL), 0, size - 1);
    }

    static boolean isSegment(WaterwaysPlan plan, int cell) {
        return plan.isStream(cell, Waterways.MIN_CATCHMENT) && plan.receiver(cell) >= 0;
    }

    static double getEndWidth(WaterwaysPlan plan, int cell) {
        int receiver = plan.receiver(cell);
        return plan.width(plan.isStream(receiver, Waterways.MIN_CATCHMENT) ? receiver : cell, Waterways.MIN_CATCHMENT);
    }

    public record Channel(double level, double bottom, boolean wet) {}
}
