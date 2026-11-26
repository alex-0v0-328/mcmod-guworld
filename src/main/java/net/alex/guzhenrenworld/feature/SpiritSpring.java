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
 * 元泉世界生成的注册键、主世界陆地群系名单与数据包引导入口。
 * <ul>
 *   <li>各键共用资源标识 {@link #ID}；{@link SpiritSpringRegistration} 以该标识注册地物，并在
 *       {@code TOP_LAYER_MODIFICATION}（植被装饰之后）将放置地物注入 {@link #BIOMES} 群系标签；本类的两个引导方法由
 *       {@code datagen.DatapackProvider} 调用。</li>
 *   <li>{@code datagen.BiomeTagsProvider} 将本类的主世界陆地群系名单（{@link #LAND_BIOMES}，不含洞穴变体）与四域、狐仙福
 *       地的陆地群系汇总写入 {@link #BIOMES}；世界生成条目归属本模组命名空间，而元泉方块、流体、物品与渲染归属主模组。</li>
 *   <li>放置地物按 {@link #RARITY} 稀有度尝试触发，实际能否成泉取决于 {@link SpiritSpringGround} 是否在候选范围内找到合
 *       格平地。</li>
 *   <li>⚠ 元泉为陆地地表结构，{@link #LAND_BIOMES} 严禁简化替换为包含海洋与河流的 {@code #minecraft:is_overworld}。
 *       </li>
 * </ul>
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
