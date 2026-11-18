package net.alex.guzhenrenworld.waterways;

/**
 * {@link GroundHeights} 寻找单列地表高度（最高实心方块上方一格）的垂直步进扫描算法。
 * <ul>
 *   <li>地形实心判定抽象为无状态函数式接口 {@link Solid}，将地形对象作为参数显式传入以避免闭包分配：无猜测值时从顶部按
 *       {@link #STEP} 格粗步向下探测，命中实心方块后再逐格向上回扫定位精确表面。</li>
 *   <li>传入猜测高度 {@code guess}（如河岸处的水面加下切深度）时优先在 {@code guess} 附近局部搜索：若该处为实心则每次向
 *       上爬升{@link #CLIMB} 格直至空气再逐格细化（在悬崖或浮空地形下方会优先取贴近溪流的谷底地面），若为空气则向下探测
 *       至多{@link #DESCENT} 格，仍未命中才退回全列粗步下扫，将单列采样次数从约 40 次降至数次。</li>
 * </ul>
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
