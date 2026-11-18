package net.alex.guzhenrenworld.waterways;

import java.util.function.DoubleUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.Nullable;

/**
 * 由 {@link WaterwaysPlan} 构建的水系河道线段空间索引：将每个达标溪流格与其下游接收格连接为有向河道线段。
 * <ul>
 *   <li>每条线段保存首尾两端的河宽（下游格未成河时沿用上游宽度）以及由 {@link WaterwaysProfile} 计算的
 *       {@link Waterways#KNOTS} + 1 个等距节点水位；初始化时按 {@link Waterways#CELL} 网格建立空间分桶，将每条线段注册
 *       到其河宽、岸带与蜿蜒偏移（{@link #getReach}）所能触及的全部网格桶中，使单列查询只需遍历所在桶的紧凑数组。</li>
 *   <li>{@link #getSurface} 对桶内各线段计算刻河目标高度并取最小值（不同于论文公式 31 的反距离加权，取最小值可确保干支
 *       流交汇口必定挖通）：落入河宽内时返回抛物线河底高度，落入岸带内时按 Smoothstep 从水面平滑抬升至原地面上方
 *       {@link Waterways#BANK_MARGIN} 格。</li>
 *   <li>河底横截面（{@link #getBottom}）在河岸边缘处对齐顶层水块底面（{@code ceil(level) - 1}，保证边缘至少有 1 格水
 *       深），向河道中线按抛物线加深至 {@link Waterways#getDepth}。</li>
 *   <li>{@link #getChannel} 查询某水平点命中的河道参数：取所有覆盖线段的最低河底；在干旱域中仅河宽 ≥
 *       {@link Waterways#DRY_WIDTH}的线段视为有水，交汇处优先取有水线段的最低水位。</li>
 *   <li>线段字段数组支持通过 {@link #save} 与 {@link #load} 以原始浮点位模式序列化到维度存档，加载时自动校验数值有限性
 *       并重建空间分桶。</li>
 * </ul>
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
