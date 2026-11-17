package net.alex.guzhenrenworld.waterways;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 小网格上水系规划的排水各步测试：盆地填到最低出口，比湖深的盆地改作汇点；每个陆地格流向更低邻格；汇流顺流向逐级累加。
 */
class WaterwaysFloodTest {

    private static final int SIZE = 5;

    @Test
    void shallowBasinFillsToItsLowestRimWhileDeepBasinBecomesSink() {
        WaterwaysPlan.Kind[] kind = border(SIZE, WaterwaysPlan.Kind.EDGE);
        float[] ground = new float[SIZE * SIZE];
        Arrays.fill(ground, 10F);
        ground[12] = 8F;
        double[] filled = new double[SIZE * SIZE];
        WaterwaysFlood.fill(kind, ground, filled, new int[SIZE * SIZE], SIZE);
        assertEquals(WaterwaysPlan.Kind.LAND, kind[12]);
        assertEquals(10.0 + 2 * WaterwaysFlood.EPSILON, filled[12], 1e-9, "a 2-block basin fills to just over its rim");

        ground[12] = 10F - (float) Waterways.LAKE_DEPTH - 1F;
        kind = border(SIZE, WaterwaysPlan.Kind.EDGE);
        WaterwaysFlood.fill(kind, ground, filled, new int[SIZE * SIZE], SIZE);
        assertEquals(WaterwaysPlan.Kind.SINK, kind[12], "a basin deeper than a lake ends the stream at its floor");
        assertEquals(ground[12], filled[12], 1e-9);
    }

    @Test
    void everyLandCellDrainsDownhillAndCatchmentsSumDownTheFlow() {
        int count = SIZE * SIZE;
        WaterwaysPlan.Kind[] kind = border(SIZE, WaterwaysPlan.Kind.OUTLET);
        float[] ground = new float[count];
        int[] x = new int[count];
        int[] z = new int[count];
        for (int cell = 0; cell < count; cell++) {
            int row = cell / SIZE;
            x[cell] = cell % SIZE * Waterways.CELL;
            z[cell] = row * Waterways.CELL;
            ground[cell] = 20F + row * 5F + (cell % SIZE == 2 ? 0F : 2F);
        }
        double[] filled = new double[count];
        int[] order = new int[count];
        WaterwaysFlood.fill(kind, ground, filled, order, SIZE);
        int[] receiver = WaterwaysFlood.getReceivers(kind, filled, x, z, SIZE);
        int[] catchment = WaterwaysFlood.getCatchments(receiver, order);

        int land = 0;
        for (int cell = 0; cell < count; cell++) {
            if (kind[cell] != WaterwaysPlan.Kind.LAND) {
                assertEquals(-1, receiver[cell]);
                continue;
            }
            land++;
            assertTrue(receiver[cell] >= 0 && filled[receiver[cell]] < filled[cell], "cell " + cell + " drains down");
        }
        int reaching = 0;
        for (int cell = 0; cell < count; cell++) {
            if (kind[cell] == WaterwaysPlan.Kind.LAND) continue;
            reaching += catchment[cell] - 1;
        }
        assertEquals(land, reaching, "every land cell ends in exactly one non-land cell");
    }

    @Test
    void aLandCellDrainsIntoTheNeighborWithTheSteepestFallPerBlock() {
        assertEquals(5, drain(new float[] {100, 100, 100, 100, 100, 92, 100, 100, 90}),
                "east falls 8 over 16 blocks, south-east 10 over 22.6: the longer drop is shallower");
        assertEquals(8, drain(new float[] {100, 100, 100, 100, 100, 95, 100, 100, 84}),
                "south-east falls 16 over 22.6, east 5 over 16");
    }

    @Test
    void equalFallsGoToTheFirstDirectionInOrder() {
        assertEquals(3, drain(new float[] {100, 94, 100, 94, 100, 100, 100, 100, 100}),
                "west comes before north in the direction order");
        assertEquals(5, drain(new float[] {100, 100, 100, 100, 100, 94, 100, 94, 100}),
                "east comes before south in the direction order");
    }

    @Test
    void catchmentsFollowTheReceiversInReversePopOrder() {
        int[] receiver = {-1, 0, 1, 1};
        int[] order = {0, 1, 2, 3};
        assertArrayEquals(new int[] {4, 3, 1, 1}, WaterwaysFlood.getCatchments(receiver, order));
    }

    private static int drain(float[] ground) {
        WaterwaysPlan.Kind[] kind = border(3, WaterwaysPlan.Kind.OUTLET);
        int[] x = new int[9];
        int[] z = new int[9];
        for (int cell = 0; cell < 9; cell++) {
            x[cell] = cell % 3 * Waterways.CELL;
            z[cell] = cell / 3 * Waterways.CELL;
        }
        double[] filled = new double[9];
        WaterwaysFlood.fill(kind, ground, filled, new int[9], 3);
        return WaterwaysFlood.getReceivers(kind, filled, x, z, 3)[4];
    }

    private static WaterwaysPlan.Kind[] border(int size, WaterwaysPlan.Kind rim) {
        WaterwaysPlan.Kind[] kind = new WaterwaysPlan.Kind[size * size];
        for (int cell = 0; cell < kind.length; cell++) {
            int column = cell % size;
            int row = cell / size;
            boolean edge = column == 0 || row == 0 || column == size - 1 || row == size - 1;
            kind[cell] = edge ? rim : WaterwaysPlan.Kind.LAND;
        }
        return kind;
    }
}
