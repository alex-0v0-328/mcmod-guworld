package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * 表层规则的 codec 形式与纯 JSON 互转，供本包把规则当数据改写。{@link Replicas} 加复刻群系、{@link SurfacePruning}
 * 按名单裁剪，都改 {@link #encode} 给的 JSON 再经 {@link #decode} 读回，{@link Ports} 同样解码移植规则；每次调用给规则
 * 命名，写进失败消息。
 *
 * @author Alex
 * @version 1.0.0
 * @see SurfacePruning
 * @since 1.0.0
 */

final class SurfaceRuleJson {

    private SurfaceRuleJson() {}

    static JsonElement encode(SurfaceRules.RuleSource rule, String subject) {
        return SurfaceRules.RuleSource.CODEC.encodeStart(JsonOps.INSTANCE, rule)
                .getOrThrow(message -> new IllegalStateException(subject + ": " + message));
    }

    static SurfaceRules.RuleSource decode(JsonElement json, String subject) {
        return SurfaceRules.RuleSource.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(message -> new IllegalStateException(subject + ": " + message));
    }
}
