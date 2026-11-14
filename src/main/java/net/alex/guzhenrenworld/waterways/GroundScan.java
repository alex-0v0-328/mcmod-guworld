package net.alex.guzhenrenworld.waterways;

/**
 * {@link GroundHeights} 找一列地面的扫描：最高实心方块之上一格。
 *
 * <ul>
 *   <li>自顶向下每步 {@link #STEP} 格再逐格细化；地形为任意 {@link Solid}，不需要密度函数。</li>
 *   <li>带猜测值（水面加下切）时：实心则每次上爬 {@link #CLIMB} 格再细化，否则下探 ≤ {@link #DESCENT} 格，
 *       仍无退回粗步；几次取样代替约 40 次，且取离溪流最近的地面（悬垂下取地板）。</li>
 * </ul>
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

    /** {@code T} 类地形某方块是否实心；地形作参数传入，实现不必捕获，扫描零分配。 */
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
