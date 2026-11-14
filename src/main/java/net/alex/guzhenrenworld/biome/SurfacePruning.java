package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.jetbrains.annotations.Nullable;

/**
 * 把域的表层规则裁到本域放置的群系。
 * <ul>
 *   <li>域规则由整棵规则树拼成，含原版主世界规则（{@link Replicas#getSurfaceRules}），点名几十个本域不放的群系；表层系
 *       统对地表附近每个方块都走规则，这些分支每方块白做一次群系判断。</li>
 *   <li>{@link #getPruned} 从 {@code sequence} 删去群系判断不含名单群系的 {@code condition}，删去空 {@code sequence}
 *       与无事可做的 {@code condition}，其余群系列表裁到名单；方块下群系总在名单内，被删分支本不生效，地表不变。</li>
 *   <li>⚠ 名单须含群系源可能返回的每个群系，漏掉的失去表层规则；{@code not} 下的群系判断不动。</li>
 * </ul>
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
        JsonElement live = getLive(SurfaceRuleJson.encode(rule, "surface rule"), names);
        if (live == null) throw new IllegalStateException("no surface rule applies to " + names);
        return SurfaceRuleJson.decode(live, "pruned surface rule");
    }

    private static @Nullable JsonElement getLive(JsonElement rule, Set<String> roster) {
        if (!rule.isJsonObject()) return rule;
        JsonObject object = rule.getAsJsonObject();
        String type = getType(object);
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
        if (!getType(condition).equals("minecraft:biome")) return true;
        JsonArray kept = new JsonArray();
        condition.getAsJsonArray("biome_is").forEach(biome -> {
            if (roster.contains(biome.getAsString())) kept.add(biome);
        });
        if (kept.isEmpty()) return false;
        condition.add("biome_is", kept);
        return true;
    }

    private static String getType(JsonObject rule) { return rule.has("type") ? rule.get("type").getAsString() : ""; }
}
