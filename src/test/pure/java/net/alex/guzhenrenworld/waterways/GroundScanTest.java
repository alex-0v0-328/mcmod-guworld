package net.alex.guzhenrenworld.waterways;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 虚构地形上的地面扫描测试：普通列；起点猜在地面下、内、上；洞顶薄于一个扫描步长的洞穴；世界顶部的地面与世界底部。
 */
class GroundScanTest {

    private static final int MIN_Y = -64;
    private static final int TOP_Y = 320;
    private static final GroundScan.Solid<Rock> SOLID = (rock, x, y, z) -> isSolid(rock, y);
    private final GroundScan scan = new GroundScan(MIN_Y, TOP_Y);

    @Test
    void fromTheTopFindsOneAboveTheHighestSolidBlock() {
        assertEquals(101, scan.sample(SOLID, Rock.plain(100), 0, 0));
        assertEquals(MIN_Y, scan.sample(SOLID, Rock.plain(MIN_Y - 1), 0, 0));
    }

    @Test
    void aGuessFindsTheSameGroundFromAnyStart() {
        Rock rock = Rock.plain(100);
        for (int guess : new int[] {MIN_Y - 10, MIN_Y, 0, 60, 99, 100, 103, 150, 200, TOP_Y - 1, TOP_Y + 40}) {
            assertEquals(101, scan.sample(SOLID, rock, 0, 0, guess), "guess " + guess);
        }
    }

    @Test
    void aGuessInsideACaveFindsTheFloorNotTheRoof() {
        Rock rock = new Rock(40, 70, 100);
        assertEquals(41, scan.sample(SOLID, rock, 0, 0, 55), "the cave floor under the guess");
        assertEquals(101, scan.sample(SOLID, rock, 0, 0, 75), "the top of the rock the guess is in");
    }

    @Test
    void aRoofThinnerThanAScanStepIsStillFoundFromAboveOrBelow() {
        Rock rock = new Rock(40, 60, 65);
        assertEquals(66, scan.sample(SOLID, rock, 0, 0), "from the top, the coarse step lands in it");
        assertEquals(66, scan.sample(SOLID, rock, 0, 0, 80), "from air above it");
        assertEquals(66, scan.sample(SOLID, rock, 0, 0, 62), "from inside it");
        assertEquals(41, scan.sample(SOLID, rock, 0, 0, 50), "from the cave under it");
    }

    @Test
    void groundAtTheTopOfTheWorldIsFoundAndCapped() {
        for (int top : new int[] {317, 318, 319}) {
            Rock rock = Rock.plain(top);
            assertEquals(top + 1, scan.sample(SOLID, rock, 0, 0), "from the top, ground " + top);
            for (int guess : new int[] {TOP_Y - 1, TOP_Y + 40, top, top - 5}) {
                assertEquals(top + 1, scan.sample(SOLID, rock, 0, 0, guess), "ground " + top + " guess " + guess);
            }
        }
    }

    private static boolean isSolid(Rock rock, int y) {
        return y <= rock.floorTop() || y >= rock.roofBottom() && y <= rock.roofTop();
    }

    private record Rock(int floorTop, int roofBottom, int roofTop) {

        private static Rock plain(int top) {
            return new Rock(top, 1, 0);
        }
    }
}
