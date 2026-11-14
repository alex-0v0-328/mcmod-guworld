package net.alex.guzhenrenworld.waterways;

/**
 * 一个区域的水系规划，由 {@link WaterwaysPlanner} 建成。
 *
 * <ul>
 *   <li>{@link Waterways#CELL} 格方形网格，沿 x 逐行；每格记偏移后取样点、地面高度、填后高度、流向格与汇流格数。</li>
 *   <li>{@link Kind#WATER} 地面低于海平面，{@link Kind#OUTLET} 其上的河流群系（西漠干河床），{@link Kind#EDGE}
 *       最外圈，{@link Kind#SINK} 深洼底，余为 {@link Kind#LAND}；非陆地格是填洼起点，流向格 {@code -1}。</li>
 *   <li>填后高度 ≥ 地面且沿排水路径严格下降，顺流向格必到起点；高出地面处是 ≤ {@link Waterways#LAKE_DEPTH} 格深的湖（更
 *       深的底才是汇点）。溪流只取陆地与边缘格，遇水、出口、汇点即止。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WaterwaysPlan {

    private final Waterways.Region region;
    private final int size;
    private final int seaLevel;
    private final int[] x;
    private final int[] z;
    private final float[] ground;
    private final double[] filled;
    private final int[] receiver;
    private final int[] catchment;
    private final Kind[] kind;

    WaterwaysPlan(Waterways.Region region, int seaLevel, int[] x, int[] z, float[] ground, double[] filled,
            int[] receiver, int[] catchment, Kind[] kind) {
        this.region = region;
        this.size = region.cells();
        this.seaLevel = seaLevel;
        this.x = x;
        this.z = z;
        this.ground = ground;
        this.filled = filled;
        this.receiver = receiver;
        this.catchment = catchment;
        this.kind = kind;
    }

    public Waterways.Region region() {
        return region;
    }

    public int size() {
        return size;
    }

    public int count() {
        return size * size;
    }

    public int seaLevel() {
        return seaLevel;
    }

    public int x(int cell) {
        return x[cell];
    }

    public int z(int cell) {
        return z[cell];
    }

    public float ground(int cell) {
        return ground[cell];
    }

    public double filled(int cell) {
        return filled[cell];
    }

    public double depression(int cell) {
        return filled[cell] - ground[cell];
    }

    public int receiver(int cell) {
        return receiver[cell];
    }

    public int catchment(int cell) {
        return catchment[cell];
    }

    public Kind kind(int cell) {
        return kind[cell];
    }

    public boolean isStream(int cell, int minCatchment) {
        return catchment[cell] >= minCatchment && (kind[cell] == Kind.LAND || kind[cell] == Kind.EDGE);
    }

    public double width(int cell, int minCatchment) {
        return Math.min(Waterways.MAX_WIDTH,
                Waterways.WIDTH_SCALE * Math.sqrt(catchment[cell] / (double) minCatchment));
    }

    public double length(int cell) {
        int next = receiver[cell];
        return next < 0 ? 0 : Math.hypot(x[next] - x[cell], z[next] - z[cell]);
    }

    public enum Kind {
        LAND,
        WATER,
        OUTLET,
        EDGE,
        SINK
    }
}
