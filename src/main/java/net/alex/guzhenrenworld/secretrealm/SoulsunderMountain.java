package net.alex.guzhenrenworld.secretrealm;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

/**
 * 天地秘境[Secret Realm]荡魂山[Soulsunder Mountain]的注册键、群系标签与数据包引导入口；全世界只有狐仙福地中心一座。
 * <ul>
 *   <li>结构、结构集、结构类型与结构片类型共用 {@link #ID}；主模组按结构集 ID 找山心，改名两仓库同任务改。</li>
 *   <li>结构集用同心环放置：{@link #RING_COUNT} 1、{@link #RING_DISTANCE} 0、偏好群系为空。原版首环半径是 distance 的
 *       2.75–5.25 倍区块，distance 0 时恒为 0；偏好群系为空时不做「112 格内就近挪到偏好群系」那一步，所以首环确定落在
 *       区块 (0, 0)，山心方块是 (8, 山底, 8)。主模组按同一约定（环区块中心）从环位置还原山心，福地入口落点和山巅判定都
 *       以它为准。</li>
 *   <li>⚠ 不要把福地群系放进偏好群系：福地只有一个群系，112 格内处处命中，环位会被随机挪开最多约 112 格。</li>
 *   <li>结构群系标签 {@link #BIOMES} 只含狐仙福地的群系；原版建结构状态时会滤掉群系源里没有这些群系的结构集，所以主世界
 *       与其他维度都算不出环位，不生成荡魂山。</li>
 *   <li>生成步为 {@code SURFACE_STRUCTURES}、地形适配为 {@code beard_thin}：山体由 {@link SoulsunderMountainPiece} 逐列
 *       直接写入，噪声地形只在山核下被抬到山底作地基，山脚随原地形起伏（福地山心 192 格内是平地）。</li>
 *   <li>山心 {@link #CLEARANCE} 格内不生成别的地表结构，见 {@link SoulsunderClearance}。</li>
 * </ul>
 */

public final class SoulsunderMountain {

    public static final ResourceLocation ID = GuWorld.id("soulsunder_mountain");
    public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, ID);
    public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, ID);
    public static final TagKey<Biome> BIOMES =
            TagKey.create(Registries.BIOME, GuWorld.id("has_structure/soulsunder_mountain"));
    public static final int RING_DISTANCE = 0;
    public static final int RING_SPREAD = 1;
    public static final int RING_COUNT = 1;
    public static final int CLEARANCE = 96;

    private SoulsunderMountain() {}

    public static void registerStructure(BootstrapContext<Structure> context) {
        context.register(STRUCTURE, new SoulsunderMountainStructure(new Structure.StructureSettings.Builder(
                context.lookup(Registries.BIOME).getOrThrow(BIOMES))
                .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                .terrainAdapation(TerrainAdjustment.BEARD_THIN)
                .build()));
    }

    public static void registerStructureSet(BootstrapContext<StructureSet> context) {
        context.register(STRUCTURE_SET, new StructureSet(context.lookup(Registries.STRUCTURE).getOrThrow(STRUCTURE),
                new ConcentricRingsStructurePlacement(RING_DISTANCE, RING_SPREAD, RING_COUNT, HolderSet.empty())));
    }
}
