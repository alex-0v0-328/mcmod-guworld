package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Gu World's replicas of vanilla overworld biomes: each one vanilla's biome under its vanilla name in this mod's
 * namespace ({@code guworld:plains} for {@code minecraft:plains}), with vanilla's climate, colors, music, mob spawns
 * and features in vanilla's order, so a region can place it and later tune it without touching the overworld.
 *
 * <p>Two things differ, both because the heightfield regions carve their own caves:
 * <ul>
 *   <li>No replica carves. A carver's tunnel reaches up to eight chunks from where it starts, so a tunnel from one
 *   biome cuts a cliff face or a pillar standing in another open. {@link #getUncarved} drops a template's carvers,
 *   keeping its features or taking the ones it is given, for every biome Gu World builds on a vanilla one.</li>
 *   <li>Vanilla's overworld surface rules name their biomes by key, so a replica would lose its biome's surface,
 *   stony peaks their stone and calcite, the windswept hills their bare stone. {@link #getSurfaceRules} returns
 *   vanilla's rules with every replica added beside the biome it copies.</li>
 * </ul>
 *
 * <p>{@link #TEMPLATES} pairs each replica with its vanilla biome; {@link #BIOMES} lists the replicas in that order,
 * the order the language providers name them in.
 *
 * @author Alex
 * @version 1.0.0
 * @see BiomeTable
 * @since 1.0.0
 */

public final class Replicas {

    public static final ResourceKey<Biome> PLAINS = key("plains");
    public static final ResourceKey<Biome> FOREST = key("forest");
    public static final ResourceKey<Biome> BIRCH_FOREST = key("birch_forest");
    public static final ResourceKey<Biome> DARK_FOREST = key("dark_forest");
    public static final ResourceKey<Biome> BAMBOO_JUNGLE = key("bamboo_jungle");
    public static final ResourceKey<Biome> MEADOW = key("meadow");
    public static final ResourceKey<Biome> TAIGA = key("taiga");
    public static final ResourceKey<Biome> WINDSWEPT_HILLS = key("windswept_hills");
    public static final ResourceKey<Biome> STONY_PEAKS = key("stony_peaks");
    public static final ResourceKey<Biome> RIVER = key("river");
    public static final ResourceKey<Biome> LUSH_CAVES = key("lush_caves");
    public static final ResourceKey<Biome> DRIPSTONE_CAVES = key("dripstone_caves");
    public static final List<Map.Entry<ResourceKey<Biome>, ResourceKey<Biome>>> TEMPLATES = List.of(
            Map.entry(PLAINS, Biomes.PLAINS), Map.entry(FOREST, Biomes.FOREST),
            Map.entry(BIRCH_FOREST, Biomes.BIRCH_FOREST), Map.entry(DARK_FOREST, Biomes.DARK_FOREST),
            Map.entry(BAMBOO_JUNGLE, Biomes.BAMBOO_JUNGLE), Map.entry(MEADOW, Biomes.MEADOW),
            Map.entry(TAIGA, Biomes.TAIGA), Map.entry(WINDSWEPT_HILLS, Biomes.WINDSWEPT_HILLS),
            Map.entry(STONY_PEAKS, Biomes.STONY_PEAKS), Map.entry(RIVER, Biomes.RIVER),
            Map.entry(LUSH_CAVES, Biomes.LUSH_CAVES), Map.entry(DRIPSTONE_CAVES, Biomes.DRIPSTONE_CAVES));
    public static final List<ResourceKey<Biome>> BIOMES = TEMPLATES.stream().map(Map.Entry::getKey).toList();

    private Replicas() {}

    public static void registerBiomes(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        register(context, PLAINS, OverworldBiomes.plains(placed, carvers, false, false, false));
        register(context, FOREST, OverworldBiomes.forest(placed, carvers, false, false, false));
        register(context, BIRCH_FOREST, OverworldBiomes.forest(placed, carvers, true, false, false));
        register(context, DARK_FOREST, OverworldBiomes.darkForest(placed, carvers));
        register(context, BAMBOO_JUNGLE, OverworldBiomes.bambooJungle(placed, carvers));
        register(context, MEADOW, OverworldBiomes.meadowOrCherryGrove(placed, carvers, false));
        register(context, TAIGA, OverworldBiomes.taiga(placed, carvers, false));
        register(context, WINDSWEPT_HILLS, OverworldBiomes.windsweptHills(placed, carvers, false));
        register(context, STONY_PEAKS, OverworldBiomes.stonyPeaks(placed, carvers));
        register(context, RIVER, OverworldBiomes.river(placed, carvers, false));
        register(context, LUSH_CAVES, OverworldBiomes.lushCaves(placed, carvers));
        register(context, DRIPSTONE_CAVES, OverworldBiomes.dripstoneCaves(placed, carvers));
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
                .mobSpawnSettings(template.getMobSettings())
                .generationSettings(new BiomeGenerationSettings(Map.of(), features))
                .build();
    }

    public static SurfaceRules.RuleSource getSurfaceRules() {
        JsonElement vanilla = SurfaceRules.RuleSource.CODEC.encodeStart(JsonOps.INSTANCE, SurfaceRuleData.overworld())
                .getOrThrow();
        addReplicas(vanilla);
        return SurfaceRules.RuleSource.CODEC.parse(JsonOps.INSTANCE, vanilla).getOrThrow();
    }

    private static void register(BootstrapContext<Biome> context, ResourceKey<Biome> key, Biome template) {
        context.register(key, getUncarved(template));
    }

    private static void addReplicas(JsonElement rule) {
        if (rule.isJsonArray()) {
            rule.getAsJsonArray().forEach(Replicas::addReplicas);
            return;
        }
        if (!rule.isJsonObject()) return;
        JsonObject object = rule.getAsJsonObject();
        if (object.has("biome_is")) {
            JsonArray biomes = object.getAsJsonArray("biome_is");
            for (Map.Entry<ResourceKey<Biome>, ResourceKey<Biome>> template : TEMPLATES) {
                if (biomes.contains(new JsonPrimitive(template.getValue().location().toString()))) {
                    biomes.add(template.getKey().location().toString());
                }
            }
        }
        object.entrySet().forEach(entry -> addReplicas(entry.getValue()));
    }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, GuWorld.id(name));
    }
}
