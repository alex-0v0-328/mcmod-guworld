package net.alex.guzhenrenworld.waterways;

/**
 * 单个域的水文网格规划结果数据载体，由 {@link WaterwaysPlanner} 构建并交由 {@link WaterwaysIndex} 转化为河道线段。
 * <ul>
 *   <li>将整个域划分为区域网格边长（{@link Waterways.Region#cell()}，默认 {@link Waterways#CELL} 格）的方形网格（按 X 轴
 *       逐行展平为一维数组），每个网格记录抖动后的采样点坐标、原始地面高度、Priority-Flood 填洼后高度、下游接收格下标、
 *       汇流面积格数以及水文分类（{@link Kind}）。</li>
 *   <li>网格分类规则：地面低于海平面为 {@link Kind#WATER}，海平面以上但群系为河流（如西漠干河床）为 {@link Kind#OUTLET}
 *       ，网格最外圈为 {@link Kind#EDGE}，深洼谷底为 {@link Kind#SINK}，其余为 {@link Kind#LAND}；所有非
 *       {@link Kind#LAND} 格均作为排水终点（其接收格为 {@code -1}）。</li>
 *   <li>填洼后高度满足 {@code filled >= ground} 且沿流向严格递减，保证任意陆地格顺流而下必然到达排水终点；
 *       {@code filled > ground}的区域对应深度不超过 {@link Waterways#LAKE_DEPTH} 的浅湖，而溪流线段仅从汇流达标的
 *       {@link Kind#LAND} 或{@link Kind#EDGE} 格发出。</li>
 * </ul>
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
        SINK,
        WALL
    }
}
