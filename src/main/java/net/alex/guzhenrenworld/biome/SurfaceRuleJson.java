package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.alex.guzhenrenworld.DataResults;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Moves a surface rule between its codec form and plain JSON, for the classes here that rewrite rules as data.
 *
 * <p>{@link Replicas} adds its replicas to vanilla's rule and {@link SurfacePruning} cuts a rule to a roster by
 * editing the JSON {@link #encode} gives and reading the result back through {@link #decode}; {@link Ports} decodes
 * Terralith's ported rule the same way. Each call names its rule, which a failure's message carries
 * ({@link DataResults#getOrThrow}).
 *
 * @author Alex
 * @version 1.0.0
 * @see SurfacePruning
 * @since 1.0.0
 */

final class SurfaceRuleJson {

    private SurfaceRuleJson() {}

    static JsonElement encode(SurfaceRules.RuleSource rule, String subject) {
        return DataResults.getOrThrow(SurfaceRules.RuleSource.CODEC.encodeStart(JsonOps.INSTANCE, rule), subject);
    }

    static SurfaceRules.RuleSource decode(JsonElement json, String subject) {
        return DataResults.getOrThrow(SurfaceRules.RuleSource.CODEC.parse(JsonOps.INSTANCE, json), subject);
    }
}
