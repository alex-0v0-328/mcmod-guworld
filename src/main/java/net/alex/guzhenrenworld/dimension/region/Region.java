package net.alex.guzhenrenworld.dimension.region;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.BiomeName;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.biome.WildCropClimate;
import net.alex.guzhenrenworld.waterways.Waterways;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * 四域之一的全部差异值。
 * <ul>
 *   <li>四域即中州（主世界）外的南疆、西漠、东海、北原；差异全为值，{@link RegionBootstraps} 方能同法注册；各域包持一实
 *       例（{@code SouthernBorder#REGION} 等），附该域说明。</li>
 *   <li>键共用位置 {@code guworld:<name>}，由 {@link #noiseSettings}、{@link #levelStem}、{@link #level} 构建；
 *       {@link #name} 兼作移植地形副本前缀。</li>
 *   <li>四域共用维度类型 {@link #DIMENSION_TYPE}（{@code guworld:region}，字段无差别）、高度 {@link #MIN_Y} 至
 *       {@link #TOP_Y}（{@link #HEIGHT} 格）、{@link net.alex.guzhenrenworld.dimension.RegionBorders} 应用的方形世界边
 *       界（中心 0, 0，边长 {@link #BORDER_SIZE} 格即 8 km，约 26.2 万区块）。</li>
 *   <li>⚠ 键与主模组 {@code net.alex.guzhenren.registry.world.ModDimensions} 同名维度键共用位置，管理员传送
 *       {@code /guworld enter guworld:<name>} 靠它落到 0, 0 上方地表（旅行归主模组）；只改一边则命令落空。</li>
 * </ul>
 *
 * @param name     每个键的路径，也是该域在移植地形里的前缀
 * @param title    维度显示名
 * @param seaLevel 噪声设置的海平面
 * @param dry      是否只有最宽的水系有水（{@link Waterways.Region#dry}）
 * @param bed      水系水下的方块
 * @param biomes   该域放置的群系
 * @param hooks    该域如何填它那份移植地形的钩子
 * @author Alex
 * @version 1.0.0
 * @see RegionBootstraps
 * @since 1.0.0
 */

public record Region(String name, Title title, int seaLevel, boolean dry, Waterways.Bed bed, Biomes biomes,
        Hooks hooks) {

    public static final double BORDER_SIZE = 8192.0;
    public static final int MIN_Y = -64;
    public static final int HEIGHT = 384;
    public static final int TOP_Y = MIN_Y + HEIGHT;
    public static final ResourceKey<DimensionType> DIMENSION_TYPE =
            GuWorld.key(Registries.DIMENSION_TYPE, "region");

    public ResourceKey<NoiseGeneratorSettings> noiseSettings() {
        return GuWorld.key(Registries.NOISE_SETTINGS, name);
    }

    public ResourceKey<LevelStem> levelStem() {
        return GuWorld.key(Registries.LEVEL_STEM, name);
    }

    public ResourceKey<Level> level() {
        return GuWorld.key(Registries.DIMENSION, name);
    }

    public Waterways.Region waterways() {
        return new Waterways.Region(name, BORDER_SIZE, dry, bed);
    }

    /**
     * 维度的英文与简体中文显示名。
     */
    public record Title(String english, String simplified) {}

    /**
     * 一个域放置的群系：{@code replicas}、{@link #ported}：{@link Replicas#SHARED}、{@link Ports#BIOMES} 外另加的原版复
     * 刻与 Terralith 移植；{@link #roster}：表能放的全部群系；{@link #parameters}：群系表；{@link #land}：除河流外的地
     * 表群系，元泉[Spirit Spring]可生成于此；{@link #names}：本域独有群系的显示名；{@link #crops}：陆地按农夫乐事野生作
     * 物气候分组。
     */
    public record Biomes(List<Replicas.Template> replicas, List<ResourceKey<Biome>> ported,
            Set<ResourceKey<Biome>> roster, List<ResourceKey<Biome>> land, List<BiomeName> names,
            Map<WildCropClimate, List<ResourceKey<Biome>>> crops,
            Function<HolderGetter<Biome>, List<Pair<Climate.ParameterPoint, Holder<Biome>>>> parameters) {}

    /**
     * 一个域填进其移植路由副本钩子的值（{@link RegionBootstraps}）；{@link #reshape} 按名改写移植函数，
     * {@link #AS_PORTED} 全不改。
     */
    public record Hooks(DensityFunction oceanOffset, double verticalScale, double dunes,
            BiFunction<String, JsonElement, JsonElement> reshape) {

        public static final BiFunction<String, JsonElement, JsonElement> AS_PORTED = (name, function) -> function;
    }
}
