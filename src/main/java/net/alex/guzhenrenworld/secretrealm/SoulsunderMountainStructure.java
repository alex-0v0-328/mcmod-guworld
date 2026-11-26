package net.alex.guzhenrenworld.secretrealm;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Optional;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandSite;
import net.alex.guzhenrenworld.dimension.blissfulland.BoundedNoiseGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * 荡魂山的结构：在放置器选中的区块中心立山，按区块拆成 {@link SoulsunderMountainPiece}，自身不读模板、不带随机。
 * <ul>
 *   <li>山心方块固定为起始区块中心（{@code getMiddleBlockX/Z}），主模组按同一约定从放置器命中区块还原山心，二者不得分开
 *       改。</li>
 *   <li>山底 y：福地生成器下取挑址给出的山底（{@link BlissfulLandSite#floor}，山心周围 3×3、间距 32 格的刻河前地面中位
 *       数，不等水系建图）；别的生成器（GameTest 在原版主世界立山）取山心周围 3×3、间距 {@link #SAMPLE_STEP} 格共 9 点
 *       {@code WORLD_SURFACE_WG} 高度的中位数。两种都再压到不高于 {@link BlissfulLandSite#MAX_FLOOR}，山巅不超建造上
 *       限。山核下的起伏由胡须地形适配抹平，山脚由结构片贴着原地形放，更深的落差补石。</li>
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
        int floor = context.chunkGenerator() instanceof BoundedNoiseGenerator bounded
                ? bounded.getSite(context.randomState()).floor() : getBaseHeight(context, x, z);
        BlockPos centre = new BlockPos(x, Math.min(floor, BlissfulLandSite.MAX_FLOOR), z);
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
