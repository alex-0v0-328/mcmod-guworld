package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import net.alex.guzhenrenworld.DataResults;
import net.alex.guzhenrenworld.DatapackBootstrap;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Worldgen ported from Terralith 2.5.8 (Stardust Labs) and Tectonic 3.0.28 (Apollo; Alex holds both authors'
 * permission to use and modify them), renamed into this mod's namespace and kept as datagen input under
 * {@code src/ports}, which the data run names in the system property {@value #SOURCE_PROPERTY}: Terralith's biomes,
 * features, surfaces and biome table in {@code terralith/}, the terrain in {@code tectonic/}.
 *
 * <p>The ports keep the mods' JSON as data instead of rewriting it in Java: each port's {@code index.json} lists its
 * files by kind, and each bootstrap here decodes them with the registry's own codec, against the registries this run
 * builds and vanilla's built-in ones, the latter through a lookup that creates a named tag on demand, since no tag is
 * bound while datagen runs, and registers them, so they reach {@code src/generated} like any other entry. Every
 * {@code terralith:} id became {@code guworld:}; the biomes took the names of {@link #BIOMES}, and their surface
 * rules, the subtrees of Terralith's overworld rule that name them, sit in {@code surface_rule.json}
 * ({@link #getSurfaceRules}). Terralith's overworld multi-noise list keeps its source biome ids untouched
 * ({@link #getBiomeParameters}).
 *
 * <p>The terrain is Tectonic's overworld noise router as Tectonic runs it beside Terralith ({@link #getNoiseRouter}),
 * with the density functions and noises it reaches from either mod under {@code guworld:terrain/<namespace>/}, and
 * Tectonic's config baked into vanilla density functions. The router reads a few hooks,
 * {@code guworld:terrain/hook/*}, that the region using it registers itself; {@link #registerDensityFunctions} takes
 * that region's patch, handed each ported function's name with its JSON, for what it changes in one.
 *
 * <p>{@link #registerBiomes} fails when the index and {@link #BIOMES} name different biomes, so a biome the port script
 * adds cannot register without a key the tables, translations and tags can name.
 *
 * <p>Ported biomes keep Terralith's climate, colors, mob spawns and features; like the replicas they do not carve
 * ({@link Replicas#getUncarved}). The block tags go out through the datagen tag provider ({@link #getBlockTags}).
 *
 * <p>⚠ The files come from the mods through Guzhenren's {@code tools/port_terralith.py} and
 * {@code tools/port_tectonic.py}, never edited by hand: a change goes into a script's lists, then a fresh port, then
 * {@code runData}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class Ports {

    public static final String SOURCE_PROPERTY = "guworld.ports";
    public static final ResourceKey<Biome> PLATEAU = key("plateau");
    public static final ResourceKey<Biome> FORESTED_PLATEAU = key("forested_plateau");
    public static final ResourceKey<Biome> CLOUD_FOREST = key("cloud_forest");
    public static final ResourceKey<Biome> MISTY_MOUNTAINS = key("misty_mountains");
    public static final ResourceKey<Biome> RAINFOREST = key("rainforest");
    public static final ResourceKey<Biome> HOT_SPRINGS = key("hot_springs");
    public static final ResourceKey<Biome> ORCHID_SWAMP = key("orchid_swamp");
    public static final ResourceKey<Biome> PEACH_GROVE = key("peach_grove");
    public static final ResourceKey<Biome> ROCKY_PEAKS = key("rocky_peaks");
    public static final ResourceKey<Biome> EMERALD_PEAKS = key("emerald_peaks");
    public static final ResourceKey<Biome> JUNGLE_MOUNTAINS = key("jungle_mountains");
    public static final ResourceKey<Biome> PAINTED_MOUNTAINS = key("painted_mountains");
    public static final ResourceKey<Biome> GRANITE_CLIFFS = key("granite_cliffs");
    public static final ResourceKey<Biome> ALPINE_MEADOW = key("alpine_meadow");
    public static final ResourceKey<Biome> ZHANGSHIYAN_CLIFFS = key("zhangshiyan_cliffs");
    public static final ResourceKey<Biome> KARST_PEAK_FOREST = key("karst_peak_forest");
    public static final List<ResourceKey<Biome>> BIOMES = List.of(PLATEAU, FORESTED_PLATEAU, CLOUD_FOREST,
            MISTY_MOUNTAINS, RAINFOREST, HOT_SPRINGS, ORCHID_SWAMP, PEACH_GROVE, ROCKY_PEAKS, EMERALD_PEAKS,
            JUNGLE_MOUNTAINS, PAINTED_MOUNTAINS, GRANITE_CLIFFS, ALPINE_MEADOW, ZHANGSHIYAN_CLIFFS, KARST_PEAK_FOREST);
    private static final String TERRALITH = "terralith";
    private static final String TECTONIC = "tectonic";
    private static final BiFunction<String, JsonElement, JsonElement> UNPATCHED = (name, json) -> json;

    private Ports() {}

    public static void registerNoises(DatapackBootstrap<NormalNoise.NoiseParameters> context) {
        register(context, Registries.NOISE, NormalNoise.NoiseParameters.DIRECT_CODEC, TERRALITH, "noise", UNPATCHED);
        register(context, Registries.NOISE, NormalNoise.NoiseParameters.DIRECT_CODEC, TECTONIC, "noise", UNPATCHED);
    }

    public static void registerConfiguredFeatures(DatapackBootstrap<ConfiguredFeature<?, ?>> context) {
        register(context, Registries.CONFIGURED_FEATURE, ConfiguredFeature.DIRECT_CODEC, TERRALITH,
                "configured_feature", UNPATCHED);
    }

    public static void registerPlacedFeatures(DatapackBootstrap<PlacedFeature> context) {
        register(context, Registries.PLACED_FEATURE, PlacedFeature.DIRECT_CODEC, TERRALITH, "placed_feature",
                UNPATCHED);
    }

    public static void registerDensityFunctions(DatapackBootstrap<DensityFunction> context,
            BiFunction<String, JsonElement, JsonElement> patch) {
        register(context, Registries.DENSITY_FUNCTION, DensityFunction.DIRECT_CODEC, TECTONIC, "density_function",
                patch);
    }

    public static NoiseRouter getNoiseRouter(DatapackBootstrap<?> context) {
        return DataResults.getOrThrow(NoiseRouter.CODEC.parse(getOps(context), read(TECTONIC, "noise_router")),
                "ported noise router");
    }

    public static List<Map.Entry<Climate.ParameterPoint, ResourceLocation>> getBiomeParameters() {
        List<Map.Entry<Climate.ParameterPoint, ResourceLocation>> entries = new ArrayList<>();
        for (JsonElement entry : read(TERRALITH, "biome_parameters").getAsJsonArray()) {
            Climate.ParameterPoint point = DataResults.getOrThrow(Climate.ParameterPoint.CODEC.parse(JsonOps.INSTANCE,
                    entry.getAsJsonObject().get("parameters")), "ported biome parameters");
            entries.add(Map.entry(point, ResourceLocation.parse(entry.getAsJsonObject().get("biome").getAsString())));
        }
        return entries;
    }

    public static void registerBiomes(DatapackBootstrap<Biome> context) {
        RegistryOps<JsonElement> registryOps = getOps(context);
        List<String> names = getIndex(TERRALITH, "biome");
        List<String> keys = BIOMES.stream().map(key -> key.location().getPath()).toList();
        if (!new HashSet<>(names).equals(new HashSet<>(keys))) {
            throw new IllegalStateException("ported biomes " + names + " differ from the keys " + keys);
        }
        for (String name : names) {
            Biome ported = decode(Biome.DIRECT_CODEC, registryOps, "biome", name, read(TERRALITH, "biome/" + name));
            context.register(key(name), Replicas.getUncarved(ported));
        }
    }

    public static SurfaceRules.RuleSource getSurfaceRules() {
        return SurfaceRuleJson.decode(read(TERRALITH, "surface_rule"), "ported surface rule");
    }

    public static Map<ResourceLocation, List<String>> getBlockTags() {
        Map<ResourceLocation, List<String>> tags = new LinkedHashMap<>();
        for (String name : getIndex(TERRALITH, "block_tag")) {
            JsonArray values = read(TERRALITH, "block_tag/" + name).getAsJsonObject().getAsJsonArray("values");
            tags.put(GuWorld.id(name), getStrings(values));
        }
        return tags;
    }

    private static <T> void register(DatapackBootstrap<T> context, ResourceKey<? extends Registry<T>> registry,
            Codec<T> codec, String port, String kind, BiFunction<String, JsonElement, JsonElement> patch) {
        RegistryOps<JsonElement> registryOps = getOps(context);
        for (String name : getIndex(port, kind)) {
            JsonElement json = patch.apply(name, read(port, kind + "/" + name));
            context.register(ResourceKey.create(registry, GuWorld.id(name)),
                    decode(codec, registryOps, kind, name, json));
        }
    }

    private static <T> T decode(Codec<T> codec, RegistryOps<JsonElement> registryOps, String kind, String name,
            JsonElement json) {
        return DataResults.getOrThrow(codec.parse(registryOps, json), "ported " + kind + " " + name);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static RegistryOps<JsonElement> getOps(DatapackBootstrap<?> context) {
        return RegistryOps.create(JsonOps.INSTANCE, new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> registryKey) {
                Registry<T> builtIn = (Registry<T>) BuiltInRegistries.REGISTRY.get(registryKey.location());
                if (builtIn != null) {
                    HolderLookup.RegistryLookup<T> lookup = builtIn.asTagAddingLookup();
                    return Optional.of(new RegistryOps.RegistryInfo<>(lookup, lookup, lookup.registryLifecycle()));
                }
                return Optional.of(new RegistryOps.RegistryInfo<T>(new HolderOwner<T>() {},
                        context.lookup((ResourceKey) registryKey), Lifecycle.stable()));
            }
        });
    }

    private static List<String> getIndex(String port, String kind) {
        return getStrings(read(port, "index").getAsJsonObject().getAsJsonArray(kind));
    }

    private static List<String> getStrings(JsonArray array) {
        List<String> strings = new ArrayList<>();
        array.forEach(element -> strings.add(element.getAsString()));
        return strings;
    }

    private static JsonElement read(String port, String name) {
        String root = System.getProperty(SOURCE_PROPERTY);
        if (root == null) throw new IllegalStateException("system property " + SOURCE_PROPERTY + " is not set");
        Path file = Path.of(root, port, name + ".json");
        try {
            return PortFormat.adapt(JsonParser.parseString(Files.readString(file)));
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read " + file, exception);
        }
    }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, GuWorld.id(name));
    }
}
