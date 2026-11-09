package net.alex.guzhenrenworld.waterways;

/**
 * One region's waterways [水系] plan, as {@link WaterwaysPlanner} built it: a square grid of
 * {@link Waterways#CELL}-block cells, row by row along x, each with its jittered sample point, its ground height, its
 * filled height, the cell it drains into and its catchment in cells.
 *
 * <p>A cell is {@link Kind#WATER} when its ground lies below sea level, {@link Kind#OUTLET} when it stands in a river
 * biome above that (the Western Desert's [西漠] dry riverbeds), {@link Kind#EDGE} on the grid's outer ring,
 * {@link Kind#SINK} at the floor of a depression deeper than {@link Waterways#LAKE_DEPTH}, and {@link Kind#LAND}
 * otherwise. All but land seed the flood; their own receiver is {@code -1}. The filled height never falls below the
 * ground and strictly falls along every drainage path, so following receivers always ends at a seed; where it stands
 * above the ground, the cell lies in a lake at most {@link Waterways#LAKE_DEPTH} deep. Streams take land and edge
 * cells only: they end where they reach water, an outlet or a sink.
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
