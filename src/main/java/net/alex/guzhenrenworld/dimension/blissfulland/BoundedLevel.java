package net.alex.guzhenrenworld.dimension.blissfulland;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * 疆域边上的装饰视图：把 {@link WorldGenLevel} 包一层，界外坐标读作实心的默认方块、写入一律丢弃，界内原样转交。
 * <ul>
 *   <li>装饰阶段写半径 1 个区块，紧挨界外的界内区块做装饰时，原版地物会读写界外那几个空区块：泉见断面上 4 面石头 1 面
 *       空就放一个往虚空流的泉眼，晶洞外壳、树冠会伸出去半截。界外读成石头后泉判成没有出口，伸出去的方块写不进去。
 *       </li>
 *   <li>按第一个参数拦截：它是界外的 {@link BlockPos} 时，读方块、读流体、按条件判断方块或流体、判空、取方块实体、写
 *       入、拆除与 {@code ensureCanWrite}（矿石地物按它决定写不写区块段）各按上面的规则返回；同名以外的接口默认方法在代
 *       理自身上执行，于是也走这些拦截；其余调用全部转交原对象。</li>
 *   <li>界内挨着界外的边缘列（{@code edge}）里，写入含流体的方块（泉眼、熔岩湖、含水方块）一律换成 {@code solid}：
 *       断面封成断崖（Alex 定），开口朝内的泉和地下熔岩湖也不会在断面上留下往虚空流的流体。</li>
 *   <li>只用在紧挨界外的区块上（{@link BoundedNoiseGenerator#applyBiomeDecoration}），界内其他区块不经代理，没有反射开
 *       销。</li>
 *   <li>⚠ 代理只实现 {@link WorldGenLevel} 接口；按具体类（{@code WorldGenRegion}）判断的代码在边缘区块上会走另一条分
 *       支，原版地物里没有这种判断。</li>
 * </ul>
 */

public final class BoundedLevel {

    private BoundedLevel() {}

    public static WorldGenLevel wrap(WorldGenLevel level, Predicate<BlockPos> inside, Predicate<BlockPos> edge,
            BlockState solid) {
        return (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, new Handler(level, inside, edge, solid));
    }

    private record Handler(WorldGenLevel level, Predicate<BlockPos> inside, Predicate<BlockPos> edge,
            BlockState solid) implements InvocationHandler {

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
            if (arguments != null && arguments.length > 0 && arguments[0] instanceof BlockPos pos
                    && !inside.test(pos)) {
                FluidState dry = Fluids.EMPTY.defaultFluidState();
                switch (method.getName()) {
                    case "getBlockState":
                        return solid;
                    case "getFluidState":
                        return dry;
                    case "isStateAtPosition":
                        return ((Predicate<BlockState>) arguments[1]).test(solid);
                    case "isFluidAtPosition":
                        return ((Predicate<FluidState>) arguments[1]).test(dry);
                    case "isEmptyBlock", "setBlock", "removeBlock", "destroyBlock", "ensureCanWrite":
                        return false;
                    case "getBlockEntity":
                        return method.getReturnType() == Optional.class ? Optional.empty() : null;
                    default:
                        if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, arguments);
                }
            }
            if (method.getName().equals("setBlock") && arguments[1] instanceof BlockState state
                    && !state.getFluidState().isEmpty() && edge.test((BlockPos) arguments[0])) {
                arguments[1] = solid;
            }
            try {
                return method.invoke(level, arguments);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        }
    }
}
