package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.jetbrains.annotations.Nullable;

/**
 * Cuts a region's surface rule down to the biomes the region can place.
 *
 * <p>A region's rule is built from whole rule trees, vanilla's overworld rule ({@link Replicas#getSurfaceRules})
 * among them, and those name dozens of biomes the region never places. The surface system walks the rule for every
 * block near the surface, so each such branch costs a biome test per block for nothing. {@link #getPruned} drops
 * every {@code condition} whose biome test names no biome of the roster from its {@code sequence}, drops a sequence
 * left empty and a condition left with nothing to run, and trims every other biome list to the roster. The biome
 * under a block is always one the region places, so a dropped branch could never apply and no block's surface
 * changes.
 *
 * <p>⚠ The roster must hold every biome the region's biome source can return: a biome left out loses its surface
 * rules. A biome test under {@code not} stays as it is.
 *
 * @author Alex
 * @version 1.0.0
 * @see Replicas
 * @since 1.0.0
 */

public final class SurfacePruning {

    private SurfacePruning() {}

    public static SurfaceRules.RuleSource getPruned(SurfaceRules.RuleSource rule,
            Collection<ResourceKey<Biome>> roster) {
        Set<String> names = roster.stream().map(key -> key.location().toString()).collect(Collectors.toSet());
        JsonElement live = getLive(SurfaceRules.RuleSource.CODEC.encodeStart(JsonOps.INSTANCE, rule).getOrThrow(),
                names);
        if (live == null) throw new IllegalStateException("no surface rule applies to " + names);
        return SurfaceRules.RuleSource.CODEC.parse(JsonOps.INSTANCE, live).getOrThrow();
    }

    private static @Nullable JsonElement getLive(JsonElement rule, Set<String> roster) {
        if (!rule.isJsonObject()) return rule;
        JsonObject object = rule.getAsJsonObject();
        String type = object.has("type") ? object.get("type").getAsString() : "";
        if (type.equals("minecraft:sequence")) {
            JsonArray live = new JsonArray();
            for (JsonElement child : object.getAsJsonArray("sequence")) {
                JsonElement kept = getLive(child, roster);
                if (kept != null) live.add(kept);
            }
            if (live.isEmpty()) return null;
            object.add("sequence", live);
        } else if (type.equals("minecraft:condition")) {
            if (!isLive(object.getAsJsonObject("if_true"), roster)) return null;
            JsonElement then = getLive(object.get("then_run"), roster);
            if (then == null) return null;
            object.add("then_run", then);
        }
        return object;
    }

    private static boolean isLive(JsonObject condition, Set<String> roster) {
        if (!condition.has("type") || !condition.get("type").getAsString().equals("minecraft:biome")) return true;
        JsonArray kept = new JsonArray();
        condition.getAsJsonArray("biome_is").forEach(biome -> {
            if (roster.contains(biome.getAsString())) kept.add(biome);
        });
        if (kept.isEmpty()) return false;
        condition.add("biome_is", kept);
        return true;
    }
}
