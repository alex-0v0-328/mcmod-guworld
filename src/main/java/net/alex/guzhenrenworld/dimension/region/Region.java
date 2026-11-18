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
 * 四域（南疆、西漠、东海、北原）维度差异化配置的统一数据载体。
 * <ul>
 *   <li>将各域的差异提炼为纯数据对象，使 {@link RegionBootstraps} 能够以统一流程注册数据包条目；每个域包各自持有一个静
 *       态实例（如 {@code SouthernBorder#REGION}）。</li>
 *   <li>{@link #noiseSettings}、{@link #levelStem} 与 {@link #level} 均以 {@code guworld:<name>} 构造对应注册键，且
 *       {@link #name} 兼作该域移植地形副本的命名空间路径前缀。</li>
 *   <li>四域共用同一维度类型 {@link #DIMENSION_TYPE}（{@code guworld:region}）、垂直高度范围 {@link #MIN_Y} 至
 *       {@link #TOP_Y}（共 {@link #HEIGHT} 格），以及由 {@link net.alex.guzhenrenworld.dimension.RegionBorders} 施加的
 *       以{@code (0, 0)} 为中心、边长 {@link #BORDER_SIZE} 格（8 km）的方形世界边界。</li>
 *   <li>内嵌记录分别封装各子域配置：{@link Title} 保存中英文维度显示名；{@link Biomes} 汇总域独有的复刻与移植群系、全量
 *       群系名单（{@code roster}）、可供元泉[Spirit Spring]生成的非水体陆地群系（{@code land}）、群系译名、野生作物气候
 *       表及多噪声参数构造函数；{@link Hooks} 保存填充移植地形钩子的参数与密度函数 JSON 重塑回调（默认不重塑为
 *       {@link Hooks#AS_PORTED}）。</li>
 *   <li>⚠ 维度键路径必须与主模组 {@code net.alex.guzhenren.registry.world.ModDimensions} 中的同名维度键完全一致，否则主
 *       模组的 {@code /guworld enter guworld:<name>} 跨维度传送指令将无法解析目标维度。</li>
 * </ul>
 *
 * @param name     各注册键的共用路径名，兼作该域在移植地形中的路径前缀
 * @param title    维度的中英文显示名
 * @param seaLevel 噪声生成器的海平面高度
 * @param dry      是否仅保留最宽的主干水系有水（{@link Waterways.Region#dry()}）
 * @param bed      水系河床铺设的方块类型
 * @param biomes   该域放置的生物群系与多噪声表配置
 * @param hooks    该域填充移植地形路由钩子的参数与重塑函数
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

    public record Title(String english, String simplified) {}

    public record Biomes(List<Replicas.Template> replicas, List<ResourceKey<Biome>> ported,
            Set<ResourceKey<Biome>> roster, List<ResourceKey<Biome>> land, List<BiomeName> names,
            Map<WildCropClimate, List<ResourceKey<Biome>>> crops,
            Function<HolderGetter<Biome>, List<Pair<Climate.ParameterPoint, Holder<Biome>>>> parameters) {}

    public record Hooks(DensityFunction oceanOffset, double verticalScale, double dunes,
            BiFunction<String, JsonElement, JsonElement> reshape) {

        public static final BiFunction<String, JsonElement, JsonElement> AS_PORTED = (name, function) -> function;
    }
}
