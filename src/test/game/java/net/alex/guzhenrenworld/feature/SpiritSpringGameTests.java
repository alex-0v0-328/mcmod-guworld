package net.alex.guzhenrenworld.feature;

import java.util.Arrays;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 元泉[Spirit Spring]无需世界的数值检查。
 * <ul>
 *   <li>{@link #basinLayersResolve}：两层石盆图层均 7×7，非保留符号皆解析为方块状态。</li>
 *   <li>{@link #clusterSizeFollowsWeights}：[0,100) 掷值按累积权重 50/20/15/10/5 定泉群大小。</li>
 *   <li>{@link #acceptedOffsetsKeepBasinsApart}：间距接受的成员偏移不让两石盆共用非保留格。</li>
 *   <li>跑在本就加载注册表的 GameTest 服务端，免 JUnit 层为几毫秒检查再启动模组；放 {@code feature} 包读包私有表。</li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiritSpringGameTests {

    private SpiritSpringGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void basinLayersResolve(GameTestHelper helper) {
        for (String[] layer : new String[][] { SpiritSpringBasin.LAYER_ONE, SpiritSpringBasin.LAYER_TWO }) {
            helper.assertTrue(layer.length == 7, "a layer has " + layer.length + " rows");
            for (String row : layer) {
                helper.assertTrue(row.length() == 7, "row " + row + " is not 7 wide");
                for (char symbol : row.toCharArray()) {
                    helper.assertTrue(symbol == 'x' || SpiritSpringBasin.getState(symbol) != null,
                            "symbol " + symbol + " in " + row + " resolves to no block state");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void clusterSizeFollowsWeights(GameTestHelper helper) {
        helper.assertTrue(Arrays.stream(SpiritSpringCluster.CLUSTER_SIZE_WEIGHTS).sum() == 100,
                "the cluster weights do not sum to 100");
        helper.assertTrue(SpiritSpringCluster.CLUSTER_ROLL_BOUND == 100, "the roll bound is not 100");
        int[] firstRolls = { 0, 50, 70, 85, 95 };
        for (int size = 1; size <= firstRolls.length; size++) {
            int roll = firstRolls[size - 1];
            helper.assertTrue(SpiritSpringCluster.getClusterSize(roll) == size,
                    "roll " + roll + " misses size " + size);
            helper.assertTrue(size == 1 || SpiritSpringCluster.getClusterSize(roll - 1) == size - 1,
                    "roll " + (roll - 1) + " misses size " + (size - 1));
        }
        helper.assertTrue(SpiritSpringCluster.getClusterSize(99) == 5, "roll 99 misses size 5");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void acceptedOffsetsKeepBasinsApart(GameTestHelper helper) {
        int reach = SpiritSpringCluster.CLUSTER_MAX_REACH;
        int accepted = 0;
        for (int offsetX = -reach; offsetX <= reach; offsetX++) {
            for (int offsetZ = -reach; offsetZ <= reach; offsetZ++) {
                if (!SpiritSpringCluster.isWithinReach(offsetX, offsetZ)
                        || !SpiritSpringCluster.isSeparationAcceptable(offsetX, offsetZ)) continue;
                accepted++;
                helper.assertFalse(hasSharedBasinCell(offsetX, offsetZ),
                        "offset " + offsetX + "," + offsetZ + " shares a basin cell");
            }
        }
        helper.assertTrue(accepted > 0, "the spacing accepts no member offset");
        helper.succeed();
    }

    private static boolean hasSharedBasinCell(int offsetX, int offsetZ) {
        int size = SpiritSpringBasin.LAYER_TWO.length;
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                int otherRow = row - offsetZ;
                int otherColumn = column - offsetX;
                if (otherRow < 0 || otherRow >= size || otherColumn < 0 || otherColumn >= size) continue;
                if (isBasin(row, column) && isBasin(otherRow, otherColumn)) return true;
            }
        }
        return false;
    }

    private static boolean isBasin(int row, int column) {
        return SpiritSpringBasin.LAYER_ONE[row].charAt(column) != 'x'
                || SpiritSpringBasin.LAYER_TWO[row].charAt(column) != 'x';
    }
}
