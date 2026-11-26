package net.alex.guzhenrenworld.feature;

import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.GuzhenrenBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 野生知心草的世界生成配置：注册键、生成群系标签与数据包引导入口。
 * <ul>
 *   <li>直接复用原版 {@code random_patch} 与 {@code simple_block} 地物类型，无需自定义地物类：每次尝试在下方为草方块且
 *       自身为空气的位置放置主模组的两格高知心草（{@link GuzhenrenBlocks#INTIMATE_GRASS}），上方无空间容纳上半格时自动
 *       放弃。</li>
 *   <li>{@link #RARITY}、{@link #TRIES}、{@link #XZ_SPREAD} 与 {@link #Y_SPREAD} 对齐农夫乐事野生胡萝卜的生成密度；群系
 *       修改器挂载在 {@code VEGETAL_DECORATION} 阶段，由 {@code datagen.DatapackProvider} 调用三个引导方法写出。</li>
 *   <li>{@code datagen.BiomeTagsProvider} 将主世界陆地群系（{@link SpiritSpring#LAND_BIOMES}）、南疆与狐仙福地的陆地群系
 *       （去掉与北原共用的雪地复刻）填入 {@link #BIOMES}；无草方块的沙漠或雪峰即使在标签内也不会实际长出知心草。</li>
 *   <li>⚠ 群系修改器按群系标签而非按维度生效，因此南疆与其他域共用的群系（如东海的雨林）也会自然生成野生知心草。</li>
 * </ul>
 */

public final class IntimateGrass {

    private static final ResourceLocation ID = GuWorld.id("intimate_grass");
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, GuWorld.id("intimate_grass_generates"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ID);
    public static final ResourceKey<PlacedFeature> PLACED_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, ID);
    private static final ResourceKey<BiomeModifier> BIOME_MODIFIER =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ID);
    private static final int RARITY = 120;
    private static final int TRIES = 64;
    private static final int XZ_SPREAD = 6;
    private static final int Y_SPREAD = 3;

    private IntimateGrass() {}

    public static void registerConfiguredFeature(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        BlockPredicate onGrass = BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesBlocks(Direction.DOWN.getNormal(), Blocks.GRASS_BLOCK));
        context.register(CONFIGURED_FEATURE, new ConfiguredFeature<>(Feature.RANDOM_PATCH,
                new RandomPatchConfiguration(TRIES, XZ_SPREAD, Y_SPREAD, PlacementUtils.filtered(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(GuzhenrenBlocks.INTIMATE_GRASS.get())),
                        onGrass))));
    }

    public static void registerPlacedFeature(BootstrapContext<PlacedFeature> context) {
        context.register(PLACED_FEATURE, new PlacedFeature(
                context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(CONFIGURED_FEATURE),
                List.of(RarityFilter.onAverageOnceEvery(RARITY), InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP,
                        BiomeFilter.biome())));
    }

    public static void registerBiomeModifier(BootstrapContext<BiomeModifier> context) {
        HolderSet<Biome> biomes = context.lookup(Registries.BIOME).getOrThrow(BIOMES);
        context.register(BIOME_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(biomes,
                HolderSet.direct(context.lookup(Registries.PLACED_FEATURE).getOrThrow(PLACED_FEATURE)),
                GenerationStep.Decoration.VEGETAL_DECORATION));
    }
}
