package net.alex.guzhenrenworld.waterways;

import java.io.File;
import java.util.Arrays;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 域维度水系线段索引的持久化存档数据（保存在维度目录下的 {@code data/guworld_waterways.dat}）。
 * <ul>
 *   <li>单个域的完整水系规划约耗费 45 CPU 秒，持久化后同一世界存档在后续启动时可直接加载，无需重复规划。</li>
 *   <li>存档同时记录参数版本键（{@link Waterways#PARAMETERS}、边界尺寸与海平面）、世界种子以及采样点地形与群系指纹（
 *       {@link WaterwaysRuntime}）；{@link #get} 仅在三者完全匹配且索引数据有限合法时才返回缓存索引，一旦规划参数、世界
 *       种子、底层地形或群系布局发生变更即自动失效并触发重建。</li>
 *   <li>⚠ 后台构建线程调用 {@link #put} 写入索引与主线程自动保存存档可能并发发生，因此所有读写、脏标记与文件保存方法均
 *       使用{@code synchronized} 在当前实例上同步。</li>
 * </ul>
 */

public final class WaterwaysData extends SavedData {

    public static final String NAME = "guworld_waterways";

    private String key = "";
    private long seed;
    private int[] fingerprint = new int[0];
    private CompoundTag index = new CompoundTag();

    public static Factory<WaterwaysData> factory() {
        return new Factory<>(WaterwaysData::new, (tag, registries) -> load(tag));
    }

    private static WaterwaysData load(CompoundTag tag) {
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
