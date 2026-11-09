package net.alex.guzhenrenworld.waterways;

import java.io.File;
import java.util.Arrays;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A region dimension's saved waterways [水系] index, {@code data/guworld_waterways.dat} in the dimension's folder, so
 * the plan, about 45 CPU seconds a region, is built once per world instead of at every start.
 *
 * <p>The index is kept with what it was built from: a key of the plan's and the profile's numbers
 * ({@link Waterways#PARAMETERS}) and the region's size and sea level, the seed, and a fingerprint of the terrain, the
 * ground and biome at a few fixed columns ({@link WaterwaysRuntime}). {@link #get} hands it back only when all three
 * match, so a changed planner, seed, terrain or layout rebuilds it. The builder thread writes it while the server
 * saves it, so the writes, the dirty flag and the whole file save go through this object's lock.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WaterwaysData extends SavedData {

    public static final String NAME = "guworld_waterways";

    private String key = "";
    private long seed;
    private int[] fingerprint = new int[0];
    private CompoundTag index = new CompoundTag();

    public static Factory<WaterwaysData> factory() {
        return new Factory<>(WaterwaysData::new, WaterwaysData::load);
    }

    private static WaterwaysData load(CompoundTag tag, HolderLookup.Provider registries) {
        WaterwaysData data = new WaterwaysData();
        data.key = tag.getString("key");
        data.seed = tag.getLong("seed");
        data.fingerprint = tag.getIntArray("fingerprint");
        data.index = tag.getCompound("index");
        return data;
    }

    public @Nullable WaterwaysIndex get(String key, Waterways.Region region, long seed, int[] fingerprint) {
        CompoundTag saved;
        synchronized (this) {
            if (!this.key.equals(key) || this.seed != seed || !Arrays.equals(this.fingerprint, fingerprint)) {
                return null;
            }
            saved = index;
        }
        try {
            return WaterwaysIndex.load(saved, region);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public synchronized void put(String key, long seed, int[] fingerprint, WaterwaysIndex index) {
        this.key = key;
        this.seed = seed;
        this.fingerprint = fingerprint.clone();
        this.index = index.save();
        setDirty();
    }

    @Override
    public synchronized void save(@NotNull File file, HolderLookup.@NotNull Provider registries) {
        super.save(file, registries);
    }

    @Override
    public synchronized void setDirty(boolean dirty) {
        super.setDirty(dirty);
    }

    @Override
    public synchronized boolean isDirty() {
        return super.isDirty();
    }

    @Override
    public synchronized @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        tag.putString("key", key);
        tag.putLong("seed", seed);
        tag.putIntArray("fingerprint", fingerprint);
        tag.put("index", index.copy());
        return tag;
    }
}
