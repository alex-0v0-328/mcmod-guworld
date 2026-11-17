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
 * 野生知心草的世界生成：键、生成群系与数据包条目。
 *
 * <ul>
 *   <li>全用原版地物类型，无自定义地物：{@code random_patch} 每次尝试经 {@code simple_block} 放主模组的两格高知心草
 *       （{@link GuzhenrenBlocks#INTIMATE_GRASS}），只落在空气且脚下是草方块处；双层植物上方无空位则该次尝试作废。</li>
 *   <li>⚠ {@link #RARITY}、{@link #TRIES}、{@link #XZ_SPREAD}、{@link #Y_SPREAD} 由 Alex 定，照抄农夫乐事野生胡萝卜，
 *       不得私调；频率估算见 wiki《世界》。</li>
 *   <li>{@code datagen.BiomeTagsProvider} 以 {@link SpiritSpring#LAND_BIOMES}（中州，即主世界陆地）加南疆陆地群系填
 *       {@link #BIOMES}；沙漠、雪峰等无草方块的群系在名单内也长不出。</li>
 *   <li>⚠ 标签按群系不按维度：南疆与别域共用的群系（雨林）在别域同样生成。</li>
 *   <li>群系修改器挂 {@code VEGETAL_DECORATION}，与原版草、花同步；三个引导由 {@code datagen.DatapackProvider} 运行。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
