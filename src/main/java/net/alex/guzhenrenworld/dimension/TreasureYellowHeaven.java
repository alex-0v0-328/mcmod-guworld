package net.alex.guzhenrenworld.dimension;

import java.util.List;
import java.util.Optional;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.SurfaceDimensionTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/**
 * 宝黄天[Treasure Yellow Heaven]维度的注册键与数据包条目。
 *
 * <ul>
 *   <li>各键共用 ID {@code guworld:treasure_yellow_heaven}；{@link #registerDimensionType}、{@link #registerBiome}、
 *       {@link #registerLevelStem} 是 {@code datagen.DatapackProvider} 跑的引导：维度类型、唯一群系、层级（无地层平坦生
 *       成器，纯虚空）。</li>
 *   <li>{@link #EFFECTS}：维度类型所填、{@code client.dimension.TreasureYellowHeavenEffects} 注册的天空效果 ID；
 *       {@link #COLOR} 染天空、雾、水，也是语言 provider 写的 Traveler's Titles 颜色，并据这些键命名维度与群系。</li>
 *   <li>床与重生锚可用，床可设出生点：维度类型为自然维度，钟和指南针也因此可用；玩家不能在此放方块，万一床或重生锚到此
 *       也照常可用，不爆炸。</li>
 *   <li>平坦生成器显式写空结构集列表，同原版虚空预设：不写则考虑全部已注册结构集，含其他模组的。</li>
 *   <li>⚠ 各键共用主模组维度键 {@code net.alex.guzhenren.registry.world.ModDimensions#TREASURE_YELLOW_HEAVEN} 的 ID；
 *       层级才使该键成为已加载维度，此处改名会让主模组 {@code /guworld enter} 指空，直到那边同步改名。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class TreasureYellowHeaven {

    private static final ResourceLocation ID = GuWorld.id("treasure_yellow_heaven");
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, ID);
    public static final ResourceKey<LevelStem> LEVEL_STEM = ResourceKey.create(Registries.LEVEL_STEM, ID);
    public static final ResourceLocation EFFECTS = ID;
    public static final int COLOR = 0xF4D35E;
    private static final int MIN_Y = 0;
    private static final int HEIGHT = 256;
    private static final boolean RESPAWN_ANCHORS_WORK = true;

    private TreasureYellowHeaven() {}

    public static void registerDimensionType(BootstrapContext<DimensionType> context) {
        context.register(DIMENSION_TYPE, SurfaceDimensionTypes.create(MIN_Y, HEIGHT, RESPAWN_ANCHORS_WORK, EFFECTS,
                new DimensionType.MonsterSettings(false, false, ConstantInt.of(0), 0)));
    }

    public static void registerBiome(BootstrapContext<Biome> context) {
        context.register(BIOME, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(COLOR)
                        .fogColor(COLOR)
                        .waterColor(COLOR)
                        .waterFogColor(COLOR)
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build());
    }

    public static void registerLevelStem(BootstrapContext<LevelStem> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        Holder<Biome> biome = context.lookup(Registries.BIOME).getOrThrow(BIOME);
        FlatLevelGeneratorSettings settings =
                new FlatLevelGeneratorSettings(Optional.of(HolderSet.empty()), biome, List.of());
        context.register(LEVEL_STEM, new LevelStem(
                dimensionTypes.getOrThrow(DIMENSION_TYPE),
                new FlatLevelSource(settings)
        ));
    }
}
