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
 * 域表层规则的群系裁剪器，将完整的组合表层规则树裁剪至当前域实际放置的群系集合。
 * <ul>
 *   <li>各域的表层规则由移植规则与原版主世界规则（{@link Replicas#getSurfaceRules}）拼接而成，包含数十个不在本域生成的
 *       群系分支；由于原版表层系统会对地表附近的每个方块逐一评估规则树，未出现的群系分支会在每个方块上造成无意义的群系
 *       判定开销。</li>
 *   <li>{@link #getPruned} 会遍历规则 JSON，将群系条件（{@code minecraft:biome}）中的群系列表裁剪为本域名单的交集，并递
 *       归移除交集为空的条件分支与变空的 {@code sequence} 节点；由于域内生成的群系必然属于本域名单，裁剪不会改变任何实
 *       际地表方块。</li>
 *   <li>⚠ 传入的群系名单必须涵盖本域群系源可能返回的全部群系（不处理 {@code not} 取反条件下的群系判断），遗漏任何群系
 *       都会导致该群系丢失表层方块规则。</li>
 * </ul>
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
