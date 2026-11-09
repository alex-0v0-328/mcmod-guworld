package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
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
import org.jetbrains.annotations.NotNull;

/**
 * Worldgen ported from Terralith 2.5.8 (Stardust Labs) and Tectonic 3.0.28 (Apollo; Alex holds both authors'
 * permission to use and modify them), renamed into this mod's namespace and kept as datagen input under
 * {@code src/ports}, which the data run names in the system property {@value #SOURCE_PROPERTY}: Terralith's biomes,
 * features, surfaces and biome table in {@code terralith/}, the terrain in {@code tectonic/}.
 *
 * <p>The ports keep the mods' JSON as data instead of rewriting it in Java: each port's index files list its files
 * by kind, and each bootstrap here decodes them with the registry's own codec, against the registries this run
 * builds and vanilla's built-in ones, the latter through a lookup that creates a named tag on demand, since no tag is
 * bound while datagen runs, and registers them, so they reach {@code src/generated} like any other entry. Every
 * {@code terralith:} id became {@code guworld:}. A port holds one index per region that ported from it:
 * {@code index.json} is the Southern Border's [南疆], the first region, and {@code index.<region>.json} another
 * region's; {@link #getIndex} reads them all as one list, a file two regions both ported counted once. The biomes
 * took the names of {@link #BIOMES} and each region's own list ({@link #biome} makes the key), and their surface
 * rules, the subtrees of Terralith's overworld rule that name them, sit in {@code surface_rule.json} and
 * {@code surface_rule.<region>.json}, which {@link #getSurfaceRules} runs in sequence. Terralith's overworld
 * multi-noise list keeps its source biome ids untouched ({@link #getBiomeParameters}).
 *
 * <p>The terrain is Tectonic's overworld noise router as Tectonic runs it beside Terralith, with the density
 * functions and noises it reaches from either mod under {@code guworld:terrain/<namespace>/}, and Tectonic's config
 * baked into vanilla density functions. Eleven of the functions read hooks, {@code terrain/hook/*}, nine of them,
 * that each region sets its own way, so each region registers its own copy of the ported functions and of the
 * router, under {@code guworld:terrain/<region>/}: {@link #registerDensityFunctions} takes the region's name and
 * its patch, handed each ported function's name with its JSON, for what it changes in one, then renames every
 * function reference into the region ({@link #getRegional}), the hook references with them, which the region
 * registers at {@link #getHookKey}; {@link #getNoiseRouter} does the same for the router.
 *
 * <p>A region registers only what its router reaches ({@link Reachable}): the ported functions the router names,
 * directly or through another, plus the ported functions the region's hooks point back at ({@code hooked}, the
 * features it switches on), and the ported noises those functions read, all found in the patched JSON. The ported
 * files behind a hook the region fills with a constant, or a feature it switches off, stay out of its datapack, and
 * switching a feature on brings its files back without a change here.
 *
 * <p>Each region also registers its own copy of every noise its terrain reads, since a random state derives each
 * noise from the world seed and the noise's id, and every region runs on the world seed: two regions that read one
 * id read one noise field, the same continents, rivers and ridges.
 * {@link #registerNoises(BootstrapContext, String, BiFunction, Set)} registers the reached ported noises under the
 * region's prefix and the vanilla noises the terrain reads under
 * {@code guworld:terrain/<region>/minecraft/}, with vanilla's parameters, and {@link #registerDensityFunctions} adds
 * the region's copy of each vanilla density function that reads a noise, such as {@code minecraft:shift_x}, at the
 * same place; {@link #getRegional} renames a noise reference like any other, and a vanilla reference only when it
 * names one of those copies. The nested scan finds them once, from vanilla's own registries, and encodes the copied
 * functions back to JSON. Still shared: {@code minecraft:overworld/base_3d_noise}, whose blended noise seeds itself
 * from vanilla's {@code minecraft:terrain} whatever its id, the surface rules' noises, and a random state's aquifer
 * and ore positions.
 *
 * <p>{@link #registerBiomes} fails when the indexes and the expected keys name different biomes, so a biome a port
 * script adds cannot register without a key the tables, translations and tags can name.
 *
 * <p>Ported biomes keep Terralith's climate, colors, mob spawns and features; like the replicas they do not carve
 * ({@link Replicas#getUncarved}). The block tags go out through the datagen tag provider ({@link #getBlockTags}).
 *
 * <p>⚠ The files are ported from Terralith and Tectonic, never edited by hand; a port script is written when a region
 * needs more of them or an upstream update needs one, then a fresh port, then {@code runData}.
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
    private static final String TERRAIN = "terrain/";
    private static final String TERRAIN_ID_PREFIX = GuWorld.MOD_ID + ":" + TERRAIN;
    private static final String HOOK = "hook/";
    private static final String VANILLA = "minecraft/";
    private static final String TYPE = "type";
    private static final Set<String> NOISE_ARGUMENT_TYPES = Set.of("shift", "shift_a", "shift_b");
    private static final BiFunction<String, JsonElement, JsonElement> UNPATCHED = (name, json) -> json;

    private Ports() {}

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context) {
        register(context, Registries.NOISE, NormalNoise.NoiseParameters.DIRECT_CODEC, TERRALITH, "noise", UNPATCHED);
    }

    public static void registerNoises(BootstrapContext<NormalNoise.NoiseParameters> context, String region,
            BiFunction<String, JsonElement, JsonElement> patch, Set<String> hooked) {
        RegistryOps<JsonElement> registryOps = getOps(context);
        Reachable reachable = Reachable.of(patch, hooked);
        for (String name : getIndex(TECTONIC, "noise")) {
            if (!reachable.noises().contains(name)) continue;
            JsonElement json = read(TECTONIC, "noise/" + name);
            context.register(GuWorld.key(Registries.NOISE, getRegionalPath(region, name)),
                    decode(NormalNoise.NoiseParameters.DIRECT_CODEC, registryOps, "noise", name, json));
        }
        HolderLookup.RegistryLookup<NormalNoise.NoiseParameters> vanilla =
                Vanilla.LOOKUP.lookupOrThrow(Registries.NOISE);
        for (ResourceLocation noise : Vanilla.NOISES) {
            context.register(GuWorld.key(Registries.NOISE, getRegionalPath(region, getVanillaName(noise))),
                    vanilla.getOrThrow(ResourceKey.create(Registries.NOISE, noise)).value());
        }
    }

    public static void registerConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        register(context, Registries.CONFIGURED_FEATURE, ConfiguredFeature.DIRECT_CODEC, TERRALITH,
                "configured_feature", UNPATCHED);
    }

    public static void registerPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        register(context, Registries.PLACED_FEATURE, PlacedFeature.DIRECT_CODEC, TERRALITH, "placed_feature",
                UNPATCHED);
    }

    public static void registerDensityFunctions(BootstrapContext<DensityFunction> context, String region,
            BiFunction<String, JsonElement, JsonElement> patch, Set<String> hooked) {
        RegistryOps<JsonElement> registryOps = getOps(context);
        Reachable reachable = Reachable.of(patch, hooked);
        for (String name : getIndex(TECTONIC, "density_function")) {
            if (!reachable.functions().contains(name)) continue;
            JsonElement json = getRegional(region, patch.apply(name, read(TECTONIC, "density_function/" + name)));
            context.register(getRegionalKey(region, name),
                    decode(DensityFunction.DIRECT_CODEC, registryOps, "density_function", name, json));
        }
        for (Map.Entry<ResourceLocation, JsonElement> copied : Vanilla.FUNCTIONS.entrySet()) {
            String name = getVanillaName(copied.getKey());
            context.register(getRegionalKey(region, name), decode(DensityFunction.DIRECT_CODEC, registryOps,
                    "density_function", name, getRegional(region, copied.getValue())));
        }
    }

    public static NoiseRouter getNoiseRouter(BootstrapContext<?> context, String region) {
        JsonElement router = getRegional(region, read(TECTONIC, "noise_router"));
        return NoiseRouter.CODEC.parse(getOps(context), router)
                .getOrThrow(message -> new IllegalStateException("ported noise router: " + message));
    }

    public static ResourceKey<DensityFunction> getRegionalKey(String region, String name) {
        return GuWorld.key(Registries.DENSITY_FUNCTION, getRegionalPath(region, name));
    }

    public static ResourceKey<DensityFunction> getHookKey(String region, String hook) {
        return getRegionalKey(region, TERRAIN + HOOK + hook);
    }

    private static String getRegionalPath(String region, String name) {
        if (!name.startsWith(TERRAIN)) throw new IllegalArgumentException(name + " is not a ported terrain entry");
        return TERRAIN + region + "/" + name.substring(TERRAIN.length());
    }

    private static String getVanillaName(ResourceLocation vanilla) {
        return TERRAIN + VANILLA + vanilla.getPath();
    }

    private static JsonElement getRegional(String region, JsonElement json) {
        return getRegional(region, json, false);
    }

    private static JsonElement getRegional(String region, JsonElement json, boolean noise) {
        if (json.isJsonArray()) {
            JsonArray renamed = new JsonArray();
            json.getAsJsonArray().forEach(element -> renamed.add(getRegional(region, element, noise)));
            return renamed;
        }
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            String id = json.getAsString();
            if (id.startsWith(TERRAIN_ID_PREFIX)) {
                return new JsonPrimitive(GuWorld.MOD_ID + ":" + getRegionalPath(region,
                        id.substring(GuWorld.MOD_ID.length() + 1)));
            }
            ResourceLocation vanilla = getVanilla(id);
            boolean copied = vanilla != null
                    && (noise ? Vanilla.NOISES.contains(vanilla) : Vanilla.FUNCTIONS.containsKey(vanilla));
            if (!copied) return json;
            return new JsonPrimitive(GuWorld.MOD_ID + ":" + getRegionalPath(region, getVanillaName(vanilla)));
        }
        if (!json.isJsonObject()) return json;
        JsonObject function = json.getAsJsonObject();
        boolean noiseArgument = isNoiseArgument(function);
        JsonObject renamed = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : function.entrySet()) {
            boolean type = entry.getKey().equals(TYPE);
            renamed.add(entry.getKey(), type ? entry.getValue()
                    : getRegional(region, entry.getValue(), isNoise(entry.getKey(), noiseArgument)));
        }
        return renamed;
    }

    private static boolean isNoiseArgument(JsonObject function) {
        return function.has(TYPE)
                && NOISE_ARGUMENT_TYPES.contains(function.get(TYPE).getAsString().replace("minecraft:", ""));
    }

    private static boolean isNoise(String field, boolean noiseArgument) {
        return field.equals("noise") || noiseArgument && field.equals("argument");
    }

    private static void forEachId(JsonElement json, boolean noise, BiConsumer<String, Boolean> ids) {
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) forEachId(element, noise, ids);
            return;
        }
        if (json.isJsonObject()) {
            JsonObject function = json.getAsJsonObject();
            boolean noiseArgument = isNoiseArgument(function);
            for (Map.Entry<String, JsonElement> entry : function.entrySet()) {
                if (!entry.getKey().equals(TYPE)) {
                    forEachId(entry.getValue(), isNoise(entry.getKey(), noiseArgument), ids);
                }
            }
            return;
        }
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) ids.accept(json.getAsString(), noise);
    }

    private static ResourceLocation getVanilla(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location != null && location.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)
                ? location : null;
    }

    public static List<Map.Entry<Climate.ParameterPoint, ResourceLocation>> getBiomeParameters() {
        List<Map.Entry<Climate.ParameterPoint, ResourceLocation>> entries = new ArrayList<>();
        for (JsonElement entry : read(TERRALITH, "biome_parameters").getAsJsonArray()) {
            Climate.ParameterPoint point = Climate.ParameterPoint.CODEC
                    .parse(JsonOps.INSTANCE, entry.getAsJsonObject().get("parameters"))
                    .getOrThrow(message -> new IllegalStateException("ported biome parameters: " + message));
            entries.add(Map.entry(point, ResourceLocation.parse(entry.getAsJsonObject().get("biome").getAsString())));
        }
        return entries;
    }

    public static void registerBiomes(BootstrapContext<Biome> context, List<ResourceKey<Biome>> expected) {
        RegistryOps<JsonElement> registryOps = getOps(context);
        List<String> names = getIndex(TERRALITH, "biome");
        List<String> keys = expected.stream().map(key -> key.location().getPath()).toList();
        if (!new HashSet<>(names).equals(new HashSet<>(keys))) {
            throw new IllegalStateException("ported biomes " + names + " differ from the keys " + keys);
        }
        for (String name : names) {
            Biome ported = decode(Biome.DIRECT_CODEC, registryOps, "biome", name, read(TERRALITH, "biome/" + name));
            context.register(biome(name), Replicas.getUncarved(ported));
        }
    }

    public static SurfaceRules.RuleSource getSurfaceRules() {
        List<SurfaceRules.RuleSource> rules = new ArrayList<>();
        for (Path file : getFiles(TERRALITH, "surface_rule")) {
            rules.add(SurfaceRuleJson.decode(readFile(file), "ported surface rule " + file.getFileName()));
        }
        return rules.size() == 1 ? rules.getFirst()
                : SurfaceRules.sequence(rules.toArray(SurfaceRules.RuleSource[]::new));
    }

    public static ResourceKey<Biome> biome(String name) {
        return GuWorld.key(Registries.BIOME, name);
    }

    public static Map<ResourceLocation, List<String>> getBlockTags() {
        Map<ResourceLocation, List<String>> tags = new LinkedHashMap<>();
        for (String name : getIndex(TERRALITH, "block_tag")) {
            JsonArray values = read(TERRALITH, "block_tag/" + name).getAsJsonObject().getAsJsonArray("values");
            tags.put(GuWorld.id(name), getStrings(values));
        }
        return tags;
    }

    private static <T> void register(BootstrapContext<T> context, ResourceKey<? extends Registry<T>> registry,
            Codec<T> codec, String port, String kind, BiFunction<String, JsonElement, JsonElement> patch) {
        RegistryOps<JsonElement> registryOps = getOps(context);
        for (String name : getIndex(port, kind)) {
            JsonElement json = patch.apply(name, read(port, kind + "/" + name));
            context.register(GuWorld.key(registry, name),
                    decode(codec, registryOps, kind, name, json));
        }
    }

    private static <T> T decode(Codec<T> codec, RegistryOps<JsonElement> registryOps, String kind, String name,
            JsonElement json) {
        return codec.parse(registryOps, json)
                .getOrThrow(message -> new IllegalStateException("ported " + kind + " " + name + ": " + message));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static RegistryOps<JsonElement> getOps(BootstrapContext<?> context) {
        return RegistryOps.create(JsonOps.INSTANCE, new RegistryOps.RegistryInfoLookup() {
            @Override
            public <T> @NotNull Optional<RegistryOps.RegistryInfo<T>> lookup(
                    @NotNull ResourceKey<? extends Registry<? extends T>> registryKey) {
                Registry<T> builtIn = (Registry<T>) BuiltInRegistries.REGISTRY.get(registryKey.location());
                if (builtIn != null) {
                    return Optional.of(RegistryOps.RegistryInfo.fromRegistryLookup(builtIn.asTagAddingLookup()));
                }
                return Optional.of(new RegistryOps.RegistryInfo<>(new HolderOwner<T>() {},
                        context.lookup((ResourceKey) registryKey), Lifecycle.stable()));
            }
        });
    }

    private static List<String> getIndex(String port, String kind) {
        Set<String> names = new LinkedHashSet<>();
        for (Path file : getFiles(port, "index")) {
            JsonArray listed = readFile(file).getAsJsonObject().getAsJsonArray(kind);
            if (listed != null) names.addAll(getStrings(listed));
        }
        return List.copyOf(names);
    }

    private static List<Path> getFiles(String port, String stem) {
        Path folder = Path.of(getRoot(), port);
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.getFileName().toString().matches(stem + "(\\..+)?\\.json"))
                    .sorted().toList();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot list " + folder, exception);
        }
    }

    private static List<String> getStrings(JsonArray array) {
        List<String> strings = new ArrayList<>();
        array.forEach(element -> strings.add(element.getAsString()));
        return strings;
    }

    private static JsonElement read(String port, String name) {
        return readFile(Path.of(getRoot(), port, name + ".json"));
    }

    private static JsonElement readFile(Path file) {
        try {
            return JsonParser.parseString(Files.readString(file));
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read " + file, exception);
        }
    }

    private static String getRoot() {
        String root = System.getProperty(SOURCE_PROPERTY);
        if (root == null) throw new IllegalStateException("system property " + SOURCE_PROPERTY + " is not set");
        return root;
    }

    private static ResourceKey<Biome> key(String name) {
        return biome(name);
    }

    record Reachable(Set<String> functions, Set<String> noises) {

        static Reachable of(BiFunction<String, JsonElement, JsonElement> patch, Set<String> hooked) {
            Set<String> indexed = Set.copyOf(getIndex(TECTONIC, "density_function"));
            Set<String> functions = new LinkedHashSet<>();
            Set<String> noises = new LinkedHashSet<>();
            Deque<String> pending = new ArrayDeque<>();
            Consumer<String> reach = name -> {
                if (indexed.contains(name) && functions.add(name)) pending.add(name);
            };
            for (String name : hooked) {
                if (!indexed.contains(name)) throw new IllegalArgumentException(name + " is not a ported function");
                reach.accept(name);
            }
            BiConsumer<String, Boolean> ids = (id, noise) -> {
                if (!id.startsWith(TERRAIN_ID_PREFIX)) return;
                String name = id.substring(GuWorld.MOD_ID.length() + 1);
                if (noise) noises.add(name);
                else reach.accept(name);
            };
            forEachId(read(TECTONIC, "noise_router"), false, ids);
            while (!pending.isEmpty()) {
                String name = pending.remove();
                forEachId(patch.apply(name, read(TECTONIC, "density_function/" + name)), false, ids);
            }
            return new Reachable(Set.copyOf(functions), Set.copyOf(noises));
        }
    }

    private static final class Vanilla {

        private static final HolderLookup.Provider LOOKUP = VanillaRegistries.createLookup();
        private static final Map<ResourceLocation, JsonElement> FUNCTIONS = new LinkedHashMap<>();
        private static final Set<ResourceLocation> NOISES = new LinkedHashSet<>();
        private static final Set<ResourceLocation> NOISELESS = new HashSet<>();

        static {
            for (String name : getIndex(TECTONIC, "density_function")) {
                scan(read(TECTONIC, "density_function/" + name), false);
            }
            scan(read(TECTONIC, "noise_router"), false);
        }

        private Vanilla() {}

        private static boolean scan(JsonElement json, boolean noise) {
            boolean[] reads = { false };
            forEachId(json, noise, (id, noiseId) -> {
                ResourceLocation vanilla = getVanilla(id);
                if (vanilla == null) return;
                if (noiseId) NOISES.add(vanilla);
                reads[0] |= noiseId || isCopied(vanilla);
            });
            return reads[0];
        }

        private static boolean isCopied(ResourceLocation function) {
            if (FUNCTIONS.containsKey(function)) return true;
            if (NOISELESS.contains(function)) return false;
            Optional<JsonElement> encoded = encode(function);
            if (encoded.isEmpty() || !scan(encoded.get(), false)) {
                NOISELESS.add(function);
                return false;
            }
            FUNCTIONS.put(function, encoded.get());
            return true;
        }

        private static Optional<JsonElement> encode(ResourceLocation function) {
            return LOOKUP.lookupOrThrow(Registries.DENSITY_FUNCTION)
                    .get(ResourceKey.create(Registries.DENSITY_FUNCTION, function))
                    .map(holder -> DensityFunction.DIRECT_CODEC.encodeStart(
                            LOOKUP.createSerializationContext(JsonOps.INSTANCE), holder.value())
                            .getOrThrow(message -> new IllegalStateException("vanilla density function " + function
                                    + ": " + message)));
        }
    }
}
