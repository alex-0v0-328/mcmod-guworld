package net.alex.guzhenrenworld.waterways;

import java.io.File;
import java.util.Arrays;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 区域维度存档的水系索引：维度文件夹下 {@code data/guworld_waterways.dat}。
 *
 * <ul>
 *   <li>规划每区域约 45 CPU 秒，存下后每个世界只建一次。</li>
 *   <li>连同依据存：键（{@link Waterways#PARAMETERS}、边长、海平面）、种子、地形指纹（几处固定列的地面与群系，
 *       {@link WaterwaysRuntime}）；{@link #get} 三者全对才返回，规划器、种子、地形或布局一变即重建。</li>
 *   <li>建造线程写入与服务端保存可能并发：写入、脏标记、整文件保存都走本对象锁。</li>
 * </ul>
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
