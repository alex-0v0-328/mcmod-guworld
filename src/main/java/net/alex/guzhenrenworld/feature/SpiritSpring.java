package net.alex.guzhenrenworld.feature;

import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

/**
 * 元泉[Spirit Spring] 世界生成：键、生成群系与数据包条目。
 *
 * <ul>
 *   <li>键共用 {@link #ID}，{@link SpiritSpringRegistration} 以它注册地物，并在 {@code TOP_LAYER_MODIFICATION}（植被
 *       后）把放置地物加进 {@link #BIOMES}；两个引导由 {@code datagen.DatapackProvider} 运行。</li>
 *   <li>{@code datagen.BiomeTagsProvider} 按 {@link #LAND_BIOMES} 与南疆陆地群系（{@code BiomeLayout.LAND}）
 *       填 {@link #BIOMES}；世界生成 ID 全在 {@code guworld}，方块、流体、物品与渲染归主模组。</li>
 *   <li>{@link #LAND_BIOMES}：39 种主世界陆地群系，抄自主模组野生蛊陆地表，或会分叉；无地下变体（洞穴难过检查）。</li>
 *   <li>⚠ 陆地结构：{@link #LAND_BIOMES} 不得并成 {@code #minecraft:is_overworld}（含海洋、河流）。</li>
 *   <li>⚠ {@link #RARITY} 由 Alex 定，不得私调：每 300 区块抽一次，{@link SpiritSpringGround} 找到平地才成泉群。
 *       三块 256 格样区 1.2% 列过 15×15 检查，树林为 0；约 2% 抽中成群，主世界约每 15,000 区块一处（高度图估算）。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see SpiritSpringCluster
 * @since 1.0.0
 */

public final class SpiritSpring {

    static final ResourceLocation ID = GuWorld.id("spirit_spring");
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, GuWorld.id("spirit_spring_generates"));
    public static final List<ResourceKey<Biome>> LAND_BIOMES = List.of(
            Biomes.PLAINS,
            Biomes.SUNFLOWER_PLAINS,
            Biomes.MEADOW,
            Biomes.CHERRY_GROVE,
            Biomes.FLOWER_FOREST,

            Biomes.FOREST,
            Biomes.BIRCH_FOREST,
            Biomes.DARK_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.WINDSWEPT_FOREST,

            Biomes.TAIGA,
            Biomes.SNOWY_TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA,
            Biomes.GROVE,

            Biomes.SAVANNA,
            Biomes.SAVANNA_PLATEAU,
            Biomes.WINDSWEPT_SAVANNA,

            Biomes.JUNGLE,
            Biomes.SPARSE_JUNGLE,
            Biomes.BAMBOO_JUNGLE,

            Biomes.DESERT,
            Biomes.BADLANDS,
            Biomes.WOODED_BADLANDS,
            Biomes.ERODED_BADLANDS,

            Biomes.SNOWY_PLAINS,
            Biomes.ICE_SPIKES,
            Biomes.SNOWY_SLOPES,
            Biomes.FROZEN_PEAKS,
            Biomes.JAGGED_PEAKS,
            Biomes.STONY_PEAKS,

            Biomes.WINDSWEPT_HILLS,
            Biomes.WINDSWEPT_GRAVELLY_HILLS,

            Biomes.SWAMP,
            Biomes.MANGROVE_SWAMP,

            Biomes.BEACH,
            Biomes.SNOWY_BEACH,
            Biomes.STONY_SHORE,

            Biomes.MUSHROOM_FIELDS);
    private static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ID);
    static final ResourceKey<PlacedFeature> PLACED_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, ID);
    private static final int RARITY = 300;

    private SpiritSpring() {}

    public static void registerConfiguredFeature(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(CONFIGURED_FEATURE,
                new ConfiguredFeature<>(SpiritSpringRegistration.FEATURE.get(), NoneFeatureConfiguration.INSTANCE));
    }

    public static void registerPlacedFeature(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(PLACED_FEATURE, new PlacedFeature(
                configuredFeatures.getOrThrow(CONFIGURED_FEATURE),
                List.of(RarityFilter.onAverageOnceEvery(RARITY), InSquarePlacement.spread(), BiomeFilter.biome())));
    }
}
