package net.alex.guzhenrenworld.waterways;

/**
 * How {@link GroundHeights} finds the ground in a column: one above the highest solid block, scanning down from the top
 * {@link #STEP} blocks at a time and then block by block, over any {@link Solid} terrain, so the scan needs no density
 * function.
 *
 * <p>The banks of a stream ask with a guess, the stream's water surface plus the incision: the scan starts there,
 * climbs {@link #CLIMB} blocks at a time while solid and refines, or walks down up to {@link #DESCENT} blocks before
 * falling back to the coarse steps. That takes a handful of samples instead of some forty, and finds the ground
 * nearest the stream: under an overhang, the floor, not the overhang's top.
 *
 * @author Alex
 * @version 1.0.0
 * @see GroundHeights
 * @since 1.0.0
 */

final class GroundScan {

    private static final int STEP = 8;
    private static final int CLIMB = 4;
    private static final int DESCENT = 8;

    private final int minY;
    private final int topY;

    GroundScan(int minY, int topY) {
        this.minY = minY;
        this.topY = topY;
    }

    /**
     * Whether a block of a terrain of type {@code T} is solid; the terrain travels as an argument so a caller's
     * implementation need not capture it and a scan allocates nothing.
     */
    @FunctionalInterface
    interface Solid<T> {

        boolean at(T terrain, int x, int y, int z);
    }

    <T> int sample(Solid<T> solid, T terrain, int x, int z) {
        return scanDown(solid, terrain, x, topY - STEP, z);
    }

    <T> int sample(Solid<T> solid, T terrain, int x, int z, int guess) {
        int y = Math.clamp(guess, minY, topY - 1);
        if (solid.at(terrain, x, y, z)) {
            int air = y + CLIMB;
            while (air < topY && solid.at(terrain, x, air, z)) {
                y = air;
                air += CLIMB;
            }
            for (int fine = Math.min(air, topY) - 1; fine > y; fine--) {
                if (solid.at(terrain, x, fine, z)) return fine + 1;
            }
            return y + 1;
        }
        int floor = Math.max(minY, y - DESCENT);
        for (int fine = y - 1; fine >= floor; fine--) {
            if (solid.at(terrain, x, fine, z)) return fine + 1;
        }
        return floor > minY ? scanDown(solid, terrain, x, floor - STEP, z) : minY;
    }

    private <T> int scanDown(Solid<T> solid, T terrain, int x, int start, int z) {
        for (int y = start; y >= minY; y -= STEP) {
            if (!solid.at(terrain, x, y, z)) continue;
            for (int fine = y + STEP - 1; fine > y; fine--) {
                if (solid.at(terrain, x, fine, z)) return fine + 1;
            }
            return y + 1;
        }
        return minY;
    }
}
