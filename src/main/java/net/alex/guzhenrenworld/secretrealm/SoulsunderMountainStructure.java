package net.alex.guzhenrenworld.secretrealm;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * 荡魂山的结构：在同心环选中的区块中心立山，按区块拆成 {@link SoulsunderMountainPiece}，自身不读模板、不带随机。
 * <ul>
 *   <li>山心方块固定为起始区块中心（{@code getMiddleBlockX/Z}），主模组按同一约定从环位置还原山心，二者不得分开改。
 *       </li>
 *   <li>山底 y 取山心周围 3×3、间距 {@link #SAMPLE_STEP} 格共 9 点 {@code WORLD_SURFACE_WG} 高度的中位数：水面按表面
 *       算，单点的树坑或尖石不左右整座山；山核下的起伏由胡须地形适配抹平，山脚由结构片贴着原地形放，更深的落差补石。
 *       </li>
 *   <li>群系检查由原版在生成点（山心、山底高度）上做，用的是结构设置里的 {@link SoulsunderMountain#BIOMES}。</li>
 * </ul>
 */

public final class SoulsunderMountainStructure extends Structure {

    public static final MapCodec<SoulsunderMountainStructure> CODEC = simpleCodec(SoulsunderMountainStructure::new);
    private static final int SAMPLE_STEP = 24;

    public SoulsunderMountainStructure(StructureSettings settings) { super(settings); }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        BlockPos centre = new BlockPos(x, getBaseHeight(context, x, z), z);
        int bottom = context.heightAccessor().getMinBuildHeight();
        return Optional.of(new GenerationStub(centre, builder -> SoulsunderMountainPiece.forMountain(centre, bottom)
                .forEach(builder::addPiece)));
    }

    @Override
    public StructureType<?> type() { return SoulsunderMountainRegistration.STRUCTURE_TYPE; }

    public static int getBaseHeight(GenerationContext context, int x, int z) {
        int[] heights = new int[9];
        int index = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                heights[index++] = context.chunkGenerator().getFirstOccupiedHeight(x + dx * SAMPLE_STEP,
                        z + dz * SAMPLE_STEP, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
                        context.randomState());
            }
        }
        Arrays.sort(heights);
        return heights[4];
    }
}
