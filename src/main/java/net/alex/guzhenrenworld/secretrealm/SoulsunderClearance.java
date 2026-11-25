package net.alex.guzhenrenworld.secretrealm;

import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import org.jetbrains.annotations.Nullable;

/**
 * 荡魂山的净空：山心 {@link SoulsunderMountain#CLEARANCE} 格内不生成别的地表结构（村庄、前哨站、废弃传送门等）。
 * <ul>
 *   <li>由 {@code ChunkGeneratorMixin} 接入原版 {@code ChunkGenerator#tryGenerateStructure}：结构起点生成后、写进区块
 *       前，若它是 {@code SURFACE_STRUCTURES} 步、不是荡魂山、地形适配不是 {@code bury}，且起点边界框（胡须结构已外扩
 *       12 格）水平上进了山心净空圆，就当作无效起点丢掉，整座结构不生成。</li>
 *   <li>⚠ 原版要塞也在 {@code SURFACE_STRUCTURES} 步（默认步），只靠 {@code bury} 埋进地下；按步拦会连要塞一起拦掉，所
 *       以埋地结构放行。</li>
 *   <li>山心从结构状态的环位置还原（与 {@link SoulsunderMountainStructure} 同约定，环区块中心），只依赖世界种子与群系
 *       源，与区块生成顺序无关；该维度没有荡魂山结构集时环位置为空，不拦。</li>
 *   <li>不用结构集的 {@code exclusion_zone}：它挂在被排斥的一方，要改写原版与其他模组的结构集，且每个放置只能排斥一个结
 *       构集（前哨站已占用，排斥村庄）。</li>
 *   <li>不影响要塞（埋地，且同心环已错开）与地下结构（矿井、古城、试炼密室、埋藏的宝藏、古迹废墟），它们照常可在山下生
 *       成；主模组的地表结构与海洋地表结构（沉船、海底废墟、海底神殿）同样受净空约束。</li>
 *   <li>已知不修：{@code /locate structure} 与探险地图走原版跳过结构检查的路径（只看放置与群系，不生成起点），绕过本规
 *       则，可能报出净空圆里实际不生成的村庄；只影响显示。</li>
 *   <li>⚠ 结构状态经 {@link #STATE} 从 {@code createStructures} 传到 {@code tryGenerateStructure}：后者的参数里没有
 *       它；区块生成在多线程上并行，故用线程局部变量。</li>
 * </ul>
 */

public final class SoulsunderClearance {

    public static final ThreadLocal<ChunkGeneratorStructureState> STATE = new ThreadLocal<>();

    private SoulsunderClearance() {}

    public static boolean crowds(@Nullable ChunkGeneratorStructureState state, RegistryAccess registries,
            StructureStart start) {
        Structure structure = start.getStructure();
        if (state == null || structure.step() != GenerationStep.Decoration.SURFACE_STRUCTURES
                || structure.terrainAdaptation() == TerrainAdjustment.BURY) {
            return false;
        }

        List<ChunkPos> mountains = getMountains(state, registries);
        if (mountains == null || registries.registryOrThrow(Registries.STRUCTURE)
                .getResourceKey(structure).filter(SoulsunderMountain.STRUCTURE::equals).isPresent()) {
            return false;
        }
        BoundingBox box = start.getBoundingBox();
        for (ChunkPos mountain : mountains) {
            if (crowds(box, mountain.getMiddleBlockX(), mountain.getMiddleBlockZ())) return true;
        }
        return false;
    }

    /** {@code box} 的水平投影是否进了以 ({@code x}, {@code z}) 为心的净空圆。 */
    public static boolean crowds(BoundingBox box, int x, int z) {
        long dx = Math.max(0, Math.max(box.minX() - x, x - box.maxX()));
        long dz = Math.max(0, Math.max(box.minZ() - z, z - box.maxZ()));
        return dx * dx + dz * dz < (long) SoulsunderMountain.CLEARANCE * SoulsunderMountain.CLEARANCE;
    }

    private static @Nullable List<ChunkPos> getMountains(ChunkGeneratorStructureState state,
            RegistryAccess registries) {
        StructureSet set = registries.registryOrThrow(Registries.STRUCTURE_SET).get(SoulsunderMountain.STRUCTURE_SET);
        return set != null && set.placement() instanceof ConcentricRingsStructurePlacement rings
                ? state.getRingPositionsFor(rings) : null;
    }
}
