package net.alex.guzhenrenworld.biome;

import com.google.gson.JsonElement;

/**
 * Turns the ported Terralith and Tectonic files, written in Minecraft 1.21.1's data format, into the format this
 * version's codecs read, before {@link Ports} decodes them.
 *
 * <p>The files under {@code src/ports} are the same on every Minecraft version the mod targets; only this class
 * differs. On 1.21.1 the files already are this version's format, so {@link #adapt} hands each one back unchanged;
 * the Forge 1.20.1 port rewrites the providers, ids and tags that version reads differently here.
 *
 * @author Alex
 * @version 1.0.0
 * @see Ports
 * @since 1.0.0
 */

public final class PortFormat {

    private PortFormat() {}

    public static JsonElement adapt(JsonElement json) { return json; }
}
