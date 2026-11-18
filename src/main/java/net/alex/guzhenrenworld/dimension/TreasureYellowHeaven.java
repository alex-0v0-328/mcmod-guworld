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
 * 宝黄天维度的注册键定义与数据包引导入口。
 * <ul>
 *   <li>{@link #DIMENSION_TYPE}、{@link #BIOME} 与 {@link #LEVEL_STEM} 共用同一资源路径
 *       {@code guworld:treasure_yellow_heaven}；由 {@code datagen.DatapackProvider} 调用本类的三个引导方法分别注册维度
 *       类型、唯一群系及无地层的纯虚空超平坦维度层级。</li>
 *   <li>{@link #EFFECTS} 供客户端 {@link net.alex.guzhenrenworld.client.dimension.TreasureYellowHeavenEffects} 绑定自定
 *       义天空效果；{@link #COLOR} 同时用于群系的天空、雾气、水体染色以及语言表中的 Traveler's Titles 维度标题颜色。
 *       </li>
 *   <li>维度类型设为自然维度且允许使用床与重生锚，使钟与指南针正常运作，并防止床或重生锚在维度内交互时发生爆炸。</li>
 *   <li>超平坦生成器显式传入空结构集列表（同原版虚空预设），防止原版或其他模组的结构集在虚空中尝试生成。</li>
 *   <li>⚠ 维度路径必须与主模组 {@code net.alex.guzhenren.registry.world.ModDimensions#TREASURE_YELLOW_HEAVEN} 保持严格
 *       一致；单侧改名会导致主模组的跨维度进入指令无法找到已加载的目标维度。</li>
 * </ul>
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
