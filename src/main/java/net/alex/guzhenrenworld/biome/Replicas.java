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
 * 原版主世界群系的复刻：原版群系以原名进本模组命名空间。
 * <ul>
 *   <li>{@code guworld:plains} 即 {@code minecraft:plains}；沿用原版气候、颜色、音乐、刷怪与地物（原版顺序），域可放
 *       置、日后调整而不碰主世界。</li>
 *   <li>不雕刻：高度场地形的域自己雕刻洞穴，雕刻器隧道可伸出起点 8 个区块，会切开别的群系里的崖壁或石柱；
 *       {@link #getUncarved} 去掉模板的雕刻器，保留或替换地物，凡以原版群系为底的都经它。</li>
 *   <li>原版主世界表层规则按键点名群系，复刻群系会失去地表（尖峭山峰的石头和方解石、风袭丘陵的裸石）；
 *       {@link #getSurfaceRules} 返回原版规则，并把复刻群系加在原群系旁。</li>
 *   <li>刷怪项按实体类型自身分类放（{@link #getSpawnsByCategory}）：原版竹林把豹猫放在 monster 下，占敌对生物上限，
 *       在模组群系还引发加载器警告；重建时连同刷怪消耗移到所属分类。</li>
 *   <li>{@link Template}：复刻键、原版群系、原版工厂（{@link Factory}，同原版签名），{@link #of} 按原版名起键。</li>
 *   <li>{@link #SHARED}：各域可放的模板（平原、河流、两种洞穴），{@link #BIOMES} 同序列键（语言表 provider 命名序）；
 *       仅一域放置的原版群系，模板留在该域包，与共用模板一并交给 {@link #registerBiomes}、{@link #getSurfaceRules}；
 *       两域都要的移来此处，因群系只注册一次。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class Replicas {

    public static final ResourceKey<Biome> PLAINS = key("plains");
    public static final ResourceKey<Biome> RIVER = key("river");
    public static final ResourceKey<Biome> LUSH_CAVES = key("lush_caves");
    public static final ResourceKey<Biome> DRIPSTONE_CAVES = key("dripstone_caves");
    public static final List<Template> SHARED = List.of(
            of(Biomes.PLAINS, (placed, carvers) -> OverworldBiomes.plains(placed, carvers, false, false, false)),
            of(Biomes.RIVER, (placed, carvers) -> OverworldBiomes.river(placed, carvers, false)),
            of(Biomes.LUSH_CAVES, OverworldBiomes::lushCaves),
            of(Biomes.DRIPSTONE_CAVES, OverworldBiomes::dripstoneCaves));
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
