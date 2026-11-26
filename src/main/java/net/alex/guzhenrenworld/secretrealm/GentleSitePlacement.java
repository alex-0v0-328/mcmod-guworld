package net.alex.guzhenrenworld.secretrealm;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandSite;
import net.alex.guzhenrenworld.dimension.blissfulland.BoundedNoiseGenerator;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 缓地结构放置（{@code guworld:gentle_site}）：每个维度只命中一个区块，即福地生成器挑出的荡魂山山心区块
 * （{@link BlissfulLandSite}）。
 * <ul>
 *   <li>结构状态只带 {@code RandomState}，经 {@link BoundedNoiseGenerator#of} 找回建它的福地生成器；别的生成器建的状态
 *       找不到，处处不命中，所以群系标签含共用复刻也只在福地生成。</li>
 *   <li>主模组按 ID 取本结构集的放置器，在原点 ±33 区块内扫 {@link #isStructureChunk} 找山心（{@code SecretRealms}），
 *       不引本类。</li>
 *   <li>原版 {@code /locate structure} 与探险地图只认同心环和随机散布两种放置，找不到荡魂山；只影响显示。</li>
 * </ul>
 */

public final class GentleSitePlacement extends StructurePlacement {

    public static final MapCodec<GentleSitePlacement> CODEC = RecordCodecBuilder.mapCodec(instance ->
            placementCodec(instance).apply(instance, GentleSitePlacement::new));

    @SuppressWarnings("deprecation")
    public GentleSitePlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency,
            int salt, Optional<ExclusionZone> exclusionZone) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
    }

    public GentleSitePlacement(int salt) {
        this(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, salt, Optional.empty());
    }

    public static @Nullable ChunkPos find(ChunkGeneratorStructureState state) {
        BoundedNoiseGenerator generator = BoundedNoiseGenerator.of(state.randomState());
        return generator == null ? null : generator.getSite(state.randomState()).chunk();
    }

    @Override
    protected boolean isPlacementChunk(@NotNull ChunkGeneratorStructureState state, int x, int z) {
        ChunkPos site = find(state);
        return site != null && site.x == x && site.z == z;
    }

    @Override
    public @NotNull StructurePlacementType<?> type() { return SoulsunderMountainRegistration.PLACEMENT_TYPE; }
}
