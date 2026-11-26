package net.alex.guzhenrenworld.secretrealm;

import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandBiomes;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandSite;
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

/**
 * 天地秘境[Secret Realm]荡魂山[Soulsunder Mountain]的注册键、群系标签与数据包引导入口；全世界只有狐仙福地一座。
 * <ul>
 *   <li>结构、结构集、结构类型与结构片类型共用 {@link #ID}；主模组按结构集 ID 找山心，改名两仓库同任务改。</li>
 *   <li>结构集用缓地放置（{@link GentleSitePlacement}，盐 {@link #SALT}）：福地生成器在原点 ±32 区块内挑一处缓地
 *       （{@link BlissfulLandSite}），山心方块是该区块中心、y 为挑址给出的山底。主模组在原点 ±33 区块内扫放置器找山心
 *       区块，再读结构片边界框最高顶（山巅）减 117 得山底；福地入口落点、山巅判定与行宫都相对山心。</li>
 *   <li>结构群系标签 {@link #BIOMES} 是福地全部群系（{@link BlissfulLandBiomes#ROSTER}）：原版建结构状态时滤掉群系源里
 *       没有这些群系的结构集；与别的域共用的复刻由放置器挡住（别的生成器处处不命中）。</li>
 *   <li>生成步为 {@code SURFACE_STRUCTURES}、地形适配为 {@code beard_thin}：山体由 {@link SoulsunderMountainPiece} 逐列
 *       直接写入，噪声地形只在山核下被抬到山底作地基，山脚随原地形起伏。</li>
 *   <li>山心 {@link #CLEARANCE} 格内不生成别的地表结构，见 {@link SoulsunderClearance}。</li>
 * </ul>
 */

public final class SoulsunderMountain {

    public static final ResourceLocation ID = GuWorld.id("soulsunder_mountain");
    public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, ID);
    public static final ResourceKey<StructureSet> STRUCTURE_SET = ResourceKey.create(Registries.STRUCTURE_SET, ID);
    public static final TagKey<Biome> BIOMES =
            TagKey.create(Registries.BIOME, GuWorld.id("has_structure/soulsunder_mountain"));
    public static final int SALT = 20261126;
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
                new GentleSitePlacement(SALT)));
    }
}
