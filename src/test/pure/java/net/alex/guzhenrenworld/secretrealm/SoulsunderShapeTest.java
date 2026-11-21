package net.alex.guzhenrenworld.secretrealm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 荡魂山山形文件的读取与不变量：随 jar 发的山形为 160×160×128，全部列落在山心 80 格圆盘内，最高到 112 格以上、体积超过
 * 30 万格，每段都经面相邻的段连到山底（无浮空）；坏魔数、段相接与尾随字节被拒收。
 */

class SoulsunderShapeTest {

    private static final int SIZE = 160;

    @Test
    void shippedShapeFitsTheDiscAndStandsOnItsBase() {
        SoulsunderShape shape = SoulsunderShape.get();
        assertEquals(SIZE, shape.getWidth());
        assertEquals(SIZE, shape.getDepth());
        assertEquals(128, shape.getHeight());
        assertEquals(80, shape.getCentre());
        int highest = 0;
        for (int column = 0; column < SIZE * SIZE; column++) {
            int count = shape.getSpanCount(column);
            if (count == 0) continue;

            double dx = column % SIZE + 0.5 - shape.getCentre();
            double dz = column / SIZE + 0.5 - shape.getCentre();
            assertTrue(dx * dx + dz * dz <= 80 * 80, "column " + column + " lies outside the 80-block disc");
            highest = Math.max(highest, shape.getTop(column, count - 1));
        }
        assertTrue(highest >= 112, "the summit reaches only y " + highest);
        assertTrue(shape.getVolume() > 300_000, "the mountain holds only " + shape.getVolume() + " blocks");
        assertEquals(0, countFloatingSpans(shape));
    }

    @Test
    void brokenFilesAreRefused() throws IOException {
        byte[] good = write(0, 3, 5, 9);
        assertEquals(9, SoulsunderShape.read(new ByteArrayInputStream(good)).getVolume());
        byte[] badMagic = good.clone();
        badMagic[0] = 'X';
        byte[] trailing = Arrays.copyOf(good, good.length + 1);
        for (byte[] broken : new byte[][]{badMagic, write(0, 3, 4, 9), trailing}) {
            assertThrows(IOException.class, () -> SoulsunderShape.read(new ByteArrayInputStream(broken)));
        }
    }

    private static int countFloatingSpans(SoulsunderShape shape) {
        int[] first = new int[SIZE * SIZE + 1];
        for (int column = 0; column < SIZE * SIZE; column++) {
            first[column + 1] = first[column] + shape.getSpanCount(column);
        }
        boolean[] reached = new boolean[first[SIZE * SIZE]];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int column = 0; column < SIZE * SIZE; column++) {
            if (shape.getSpanCount(column) > 0 && shape.getBottom(column, 0) == 0) {
                reached[first[column]] = true;
                queue.add(new int[]{column, 0});
            }
        }
        while (!queue.isEmpty()) {
            int[] span = queue.poll();
            int x = span[0] % SIZE;
            int z = span[0] / SIZE;
            int bottom = shape.getBottom(span[0], span[1]);
            int top = shape.getTop(span[0], span[1]);
            for (int[] step : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                int nx = x + step[0];
                int nz = z + step[1];
                if (nx < 0 || nz < 0 || nx >= SIZE || nz >= SIZE) continue;

                int neighbour = nz * SIZE + nx;
                for (int k = 0; k < shape.getSpanCount(neighbour); k++) {
                    boolean touches = shape.getBottom(neighbour, k) <= top && shape.getTop(neighbour, k) >= bottom;
                    if (!touches || reached[first[neighbour] + k]) continue;

                    reached[first[neighbour] + k] = true;
                    queue.add(new int[]{neighbour, k});
                }
            }
        }
        int floating = 0;
        for (boolean span : reached) if (!span) floating++;
        return floating;
    }

    private static byte[] write(int... firstColumn) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);
        data.writeInt(0x475A5351);
        data.writeByte(1);
        data.writeShort(SIZE);
        data.writeShort(SIZE);
        data.writeShort(128);
        for (int column = 0; column < SIZE * SIZE; column++) {
            int[] spans = column == 0 ? firstColumn : new int[0];
            data.writeByte(spans.length / 2);
            for (int value : spans) data.writeByte(value);
        }
        return bytes.toByteArray();
    }
}
