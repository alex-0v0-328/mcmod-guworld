package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * 原版表层规则（{@link SurfaceRules.RuleSource}）与 Gson {@link JsonElement} 之间的编解码工具，供同包类将表层规则作为纯
 * JSON 数据改写。{@link Replicas} 注入复刻群系与 {@link SurfacePruning} 按域名单裁剪规则时，均先调用 {@link #encode} 转
 * 为JSON 修改，再经 {@link #decode} 还原为规则对象；{@link Ports} 也通过本类解码移植的表层规则文件，并在失败异常信息中
 * 附带调用方传入的描述名。
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
