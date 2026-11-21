package net.alex.guzhenrenworld.secretrealm;

import java.util.List;
import java.util.Set;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.feature.SpiritSpring;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

/**
 * 天地秘境[Secret Realm]荡魂山[Soulsunder Mountain]的注册键、群系标签与数据包引导入口；全世界只有中州（主世界）一座。
 * <ul>
 *   <li>结构、结构集、结构类型与结构片类型共用 {@link #ID}；主模组按结构集 ID 找山心，改名两仓库同任务改。</li>
 *   <li>结构集用同心环放置，{@link #RING_COUNT} 为 1：第一环半径为 {@link #RING_DISTANCE} 的 2.75–5.25 倍区块，再在
 *       {@link #BIASED_TO} 标签的群系里就近偏移至多 112 格；与主模组的约定是山心方块为该环区块的中心
 *       {@code (cx * 16 + 8, cz * 16 + 8)}。</li>
 *   <li>⚠ 同心环的随机源只取世界种子、不加盐，第一个点的角度总与 1 号要塞相同，半径与 distance 成正比；取要塞的 32 会让
 *       山压在 1 号要塞上。{@link #RING_DISTANCE} 定为 42（Alex 定），山心离原点约 1850–3530 格，与第一环要塞径向错开约
 *       三成半径；改它先跑 {@code SoulsunderMountainGameTests} 的要塞间距检查。</li>
 *   <li>{@link #BIASED_TO} 只偏好陆地群系（元泉的主世界陆地名单，去掉三种高峰、海滩、石岸与蘑菇岛）；结构本身的群系标签
 *       {@link #BIOMES} 是整个 {@code #minecraft:is_overworld}，偏好找不到陆地时山也照样生成，不会让这唯一一座山消失。
 *       </li>
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
    public static final TagKey<Biome> BIASED_TO =
            TagKey.create(Registries.BIOME, GuWorld.id("soulsunder_mountain_biased_to"));
    public static final int RING_DISTANCE = 42;
    public static final int RING_SPREAD = 1;
    public static final int RING_COUNT = 1;
    public static final int CLEARANCE = 96;
    private static final Set<ResourceKey<Biome>> EXCLUDED = Set.of(Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS,
            Biomes.STONY_PEAKS, Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE, Biomes.MUSHROOM_FIELDS);
    public static final List<ResourceKey<Biome>> PREFERRED_BIOMES = SpiritSpring.LAND_BIOMES.stream()
            .filter(biome -> !EXCLUDED.contains(biome)).toList();

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
                new ConcentricRingsStructurePlacement(RING_DISTANCE, RING_SPREAD, RING_COUNT,
                        context.lookup(Registries.BIOME).getOrThrow(BIASED_TO))));
    }
}
