package net.alex.guzhenrenworld;

import com.mojang.serialization.DataResult;

/**
 * Reads the value out of a codec's {@link DataResult}, or fails with the codec's message.
 *
 * <p>{@link #getOrThrow} names what was decoded or encoded in its message, so a broken port file or rule says which
 * one. It reads only what every DataFixerUpper version this mod builds against offers, {@code result()} and the
 * error's {@code message()}, so the same call compiles on each Minecraft version the mod targets.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class DataResults {

    private DataResults() {}

    public static <T> T getOrThrow(DataResult<T> result, String subject) {
        return result.result().orElseThrow(() -> new IllegalStateException(
                subject + ": " + result.error().map(error -> error.message()).orElse("no value")));
    }
}
