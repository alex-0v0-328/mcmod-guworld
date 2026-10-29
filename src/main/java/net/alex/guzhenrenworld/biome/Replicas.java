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
 * Gu World's replicas of vanilla overworld biomes: each one vanilla's biome under its vanilla name in this mod's
 * namespace ({@code guworld:plains} for {@code minecraft:plains}), with vanilla's climate, colors, music, mob spawns
 * and features in vanilla's order, so a region can place it and later tune it without touching the overworld.
 *
 * <p>Three things differ; the first two because the heightfield regions carve their own caves:
 * <ul>
 *   <li>No replica carves. A carver's tunnel reaches up to eight chunks from where it starts, so a tunnel from one
 *   biome cuts a cliff face or a pillar standing in another open. {@link #getUncarved} drops a template's carvers,
 *   keeping its features or taking the ones it is given, for every biome Gu World builds on a vanilla one.</li>
 *   <li>Vanilla's overworld surface rules name their biomes by key, so a replica would lose its biome's surface,
 *   stony peaks their stone and calcite, the windswept hills their bare stone. {@link #getSurfaceRules} returns
 *   vanilla's rules with every replica added beside the biome it copies.</li>
 *   <li>Every spawn sits under its own entity type's category ({@link #getSpawnsByCategory}). Vanilla's bamboo jungle
 *   lists the ocelot under monsters, which spends the monster cap on it and makes the loader warn for a modded biome;
 *   the rebuild moves such a spawn, with its spawn cost, to its type's category.</li>
 * </ul>
 *
 * <p>A {@link Template} pairs a replica's key with its vanilla biome and the vanilla factory that builds it
 * ({@link Factory}, the signature of vanilla's biome factories; {@link #of} names the key after the vanilla biome).
 * {@link #SHARED} is the templates every region may place, the plains, the river and the two caves;
 * {@link #BIOMES} lists their keys in that order, the order the language provider names them in. A region that
 * places a vanilla biome no other region does keeps that template in its own package and hands it to
 * {@link #registerBiomes} and {@link #getSurfaceRules} beside the shared ones; a template two regions both want
 * moves here, since a biome registers once.
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
