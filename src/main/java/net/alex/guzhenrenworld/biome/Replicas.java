package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Map;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.data.worldgen.biome.OverworldBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * 原版主世界生物群系的本模组命名空间复刻工具。
 * <ul>
 *   <li>将原版群系按同名路径复刻到本模组命名空间下（如 {@code guworld:plains} 对应 {@code minecraft:plains}），完整保留
 *       原版的气候、颜色、音效、刷怪与地物顺序，使各域能够独立放置并在日后调整而不污染原版主世界。</li>
 *   <li>统一移除地形雕刻器：采用高度场地形的域由密度函数直接雕刻洞穴，而原版雕刻器隧道可跨出起点达 8 个区块，会切穿邻近
 *       群系的崖壁或石柱；因此所有基于原版群系构建的复刻群系均经 {@link #getUncarved} 清空雕刻器，仅保留或替换地物列
 *       表。</li>
 *   <li>同步扩展原版表层规则：原版主世界表层规则按群系键精确匹配（如尖峭山峰的裸岩与方解石、风袭丘陵的石质地表），直接
 *       复刻会导致新群系丢失专属地表；{@link #getSurfaceRules} 会将原版主世界表层规则编码为 JSON，在每处匹配的原版群系
 *       旁追加对应的复刻群系键后再解码返回。</li>
 *   <li>修正生物刷怪分类（{@link #getSpawnsByCategory}）：原版竹林将豹猫配置在 {@code monster} 分类下，既挤占敌对生物刷
 *       怪上限，又会在模组群系中触发加载器警告；重建群系时会将所有刷怪项连同刷怪消耗按实体类型自身的
 *       {@link MobCategory} 重新归类。</li>
 *   <li>{@link Template} 封装复刻群系键、原版群系键及与原版签名一致的构建工厂（{@link Factory}），由 {@link #of} 按原版
 *       路径自动生成复刻键。</li>
 *   <li>{@link #SHARED} 汇总多域共用的复刻模板（平原、河流、两种洞穴、雪林与冰封山峰），{@link #BIOMES} 按相同顺序导出
 *       群系键供语言表生成器读取；仅单域使用的复刻模板留在对应域包内，在调用 {@link #registerBiomes} 与
 *       {@link #getSurfaceRules} 时与共用模板合并传入；一旦某个复刻群系被两个及以上域共用，必须上移至本类以避免重复注
 *       册。</li>
 * </ul>
 */

public final class Replicas {

    public static final ResourceKey<Biome> PLAINS = key("plains");
    public static final ResourceKey<Biome> RIVER = key("river");
    public static final ResourceKey<Biome> LUSH_CAVES = key("lush_caves");
    public static final ResourceKey<Biome> DRIPSTONE_CAVES = key("dripstone_caves");
    public static final ResourceKey<Biome> GROVE = key("grove");
    public static final ResourceKey<Biome> FROZEN_PEAKS = key("frozen_peaks");
    public static final List<Template> SHARED = List.of(
            of(Biomes.PLAINS, (placed, carvers) -> OverworldBiomes.plains(placed, carvers, false, false, false)),
            of(Biomes.RIVER, (placed, carvers) -> OverworldBiomes.river(placed, carvers, false)),
            of(Biomes.LUSH_CAVES, OverworldBiomes::lushCaves),
            of(Biomes.DRIPSTONE_CAVES, OverworldBiomes::dripstoneCaves),
            of(Biomes.GROVE, OverworldBiomes::grove),
            of(Biomes.FROZEN_PEAKS, OverworldBiomes::frozenPeaks));
    public static final List<ResourceKey<Biome>> BIOMES = SHARED.stream().map(Template::replica).toList();

    public interface Factory {
        Biome create(HolderGetter<PlacedFeature> placed, HolderGetter<ConfiguredWorldCarver<?>> carvers);
    }

    public record Template(ResourceKey<Biome> replica, ResourceKey<Biome> vanilla, Factory factory) {}

    private Replicas() {}

    public static Template of(ResourceKey<Biome> vanilla, Factory factory) {
        return new Template(keyOf(vanilla), vanilla, factory);
    }

    public static ResourceKey<Biome> keyOf(ResourceKey<Biome> vanilla) {
        return key(vanilla.location().getPath());
    }

    public static void registerBiomes(BootstrapContext<Biome> context, List<Template> templates) {
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        for (Template template : templates) {
            register(context, template.replica(), template.factory().create(placed, carvers));
        }
    }

    public static Biome getUncarved(Biome template) {
        return getUncarved(template, template.getGenerationSettings().features());
    }

    public static Biome getUncarved(Biome template, List<HolderSet<PlacedFeature>> features) {
        Biome.ClimateSettings climate = template.getModifiedClimateSettings();
        return new Biome.BiomeBuilder()
                .hasPrecipitation(climate.hasPrecipitation())
                .temperature(climate.temperature())
                .temperatureAdjustment(climate.temperatureModifier())
                .downfall(climate.downfall())
                .specialEffects(template.getModifiedSpecialEffects())
                .mobSpawnSettings(getSpawnsByCategory(template.getMobSettings()))
                .generationSettings(new BiomeGenerationSettings(Map.of(), List.copyOf(features)))
                .build();
    }

    private static MobSpawnSettings getSpawnsByCategory(MobSpawnSettings template) {
        MobSpawnSettings.Builder builder = new MobSpawnSettings.Builder()
                .creatureGenerationProbability(template.getCreatureProbability());
        for (MobCategory category : MobCategory.values()) {
            for (MobSpawnSettings.SpawnerData spawner : template.getMobs(category).unwrap()) {
                builder.addSpawn(spawner.type.getCategory(), spawner);
                MobSpawnSettings.MobSpawnCost cost = template.getMobSpawnCost(spawner.type);
                if (cost != null) builder.addMobCharge(spawner.type, cost.charge(), cost.energyBudget());
            }
        }
        return builder.build();
    }

    public static SurfaceRules.RuleSource getSurfaceRules(List<Template> templates) {
        JsonElement vanilla = SurfaceRuleJson.encode(SurfaceRuleData.overworld(), "vanilla overworld surface rule");
        addReplicas(vanilla, templates);
        return SurfaceRuleJson.decode(vanilla, "replica surface rule");
    }

    private static void register(BootstrapContext<Biome> context, ResourceKey<Biome> key, Biome template) {
        context.register(key, getUncarved(template));
    }

    private static void addReplicas(JsonElement rule, List<Template> templates) {
        if (rule.isJsonArray()) {
            rule.getAsJsonArray().forEach(element -> addReplicas(element, templates));
        } else if (rule.isJsonObject()) {
            JsonObject object = rule.getAsJsonObject();
            if (object.has("biome_is")) addTemplated(object.getAsJsonArray("biome_is"), templates);
            object.entrySet().forEach(entry -> addReplicas(entry.getValue(), templates));
        }
    }

    private static void addTemplated(JsonArray biomes, List<Template> templates) {
        for (Template template : templates) {
            if (biomes.contains(new JsonPrimitive(template.vanilla().location().toString()))) {
                biomes.add(template.replica().location().toString());
            }
        }
    }

    private static ResourceKey<Biome> key(String name) {
        return GuWorld.key(Registries.BIOME, name);
    }
}
