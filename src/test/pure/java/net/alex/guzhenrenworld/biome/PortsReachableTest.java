package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Ports.Reachable} 保留哪些移植地形的固定测试。开关全关：Tectonic 地下河、熔岩隧道与沙丘噪声全不留（沙丘样条留，
 * 由 {@code dunes} 钩子缩放），路由点名的其余全留；开地下河：其函数与噪声回来。同数据生成运行经一个系统属性读
 * {@code src/ports}，该属性由 {@code pureTest} 任务设置。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

class PortsReachableTest {

    private static final BiFunction<String, JsonElement, JsonElement> UNPATCHED = (name, json) -> json;
    private static final List<String> SWITCHED_OFF = List.of("terrain/tectonic/underground_river/",
            "terrain/tectonic/lava_tunnel/", "terrain/tectonic/region/diamond/dune_",
            "terrain/tectonic/region/diamond/dune/final", "terrain/tectonic/region/diamond/dune/noise",
            "terrain/tectonic/region/diamond/dune/offset_", "terrain/tectonic/__constants/lava_tunnels",
            "terrain/tectonic/__constants/underground_rivers");
    private static final String UNDERGROUND_RIVER_TOTAL = "terrain/tectonic/underground_river/total";

    @Test
    void withEverySwitchOffTheRouterReachesNothingOfTheOffFeatures() {
        Ports.Reachable reachable = Ports.Reachable.of(UNPATCHED, Set.of());
        assertTrue(reachable.functions().contains("terrain/tectonic/terrain_spline/factor/continents"));
        assertTrue(reachable.noises().contains("terrain/tectonic/parameter/continentalness"));
        assertTrue(reachable.functions().stream().noneMatch(PortsReachableTest::isSwitchedOff),
                "an off feature's function is reachable: " + reachable.functions().stream()
                        .filter(PortsReachableTest::isSwitchedOff).toList());
        assertTrue(reachable.noises().stream().noneMatch(PortsReachableTest::isSwitchedOff),
                "an off feature's noise is reachable: " + reachable.noises().stream()
                        .filter(PortsReachableTest::isSwitchedOff).toList());
    }

    @Test
    void aHookedFunctionBringsItsTreeAndItsNoisesBack() {
        Ports.Reachable reachable = Ports.Reachable.of(UNPATCHED, Set.of(UNDERGROUND_RIVER_TOTAL));
        assertTrue(reachable.functions().contains(UNDERGROUND_RIVER_TOTAL));
        assertTrue(reachable.functions().contains("terrain/tectonic/underground_river/spline/continents"));
        assertTrue(reachable.noises().contains("terrain/tectonic/underground_river/height"));
        assertFalse(reachable.functions().contains("terrain/tectonic/lava_tunnel/total"));
    }

    @Test
    void aHookedNameOutsideThePortIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> Ports.Reachable.of(UNPATCHED, Set.of("terrain/tectonic/no_such_function")));
    }

    private static boolean isSwitchedOff(String name) {
        return SWITCHED_OFF.stream().anyMatch(name::startsWith);
    }
}
