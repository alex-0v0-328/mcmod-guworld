package net.alex.guzhenrenworld.datagen;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.WildCropClimate;
import net.alex.guzhenrenworld.dimension.Regions;
import net.alex.guzhenrenworld.dimension.blissfulland.FoxImmortalBlissfulLand;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.feature.IntimateGrass;
import net.alex.guzhenrenworld.feature.SpiritSpring;
import net.alex.guzhenrenworld.secretrealm.SoulsunderMountain;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组生物群系标签的唯一数据生成器。
 * <ul>
 *   <li>元泉生成群系标签（{@link SpiritSpring#BIOMES}）汇总主世界陆地群系名单（{@link SpiritSpring#LAND_BIOMES}）与所有
 *       域的陆地群系；野生知心草生成群系标签（{@link IntimateGrass#BIOMES}）仅包含同一份主世界名单与南疆陆地群系；农夫
 *       乐事野生作物气候标签（{@link WildCropClimate}）则汇总 {@link Regions#ALL} 各域的作物气候映射表，多域共用的群系
 *       （如雨林）通过有序集合去重。</li>
 *   <li>荡魂山的结构群系标签（{@link SoulsunderMountain#BIOMES}）只含狐仙福地的群系（
 *       {@link FoxImmortalBlissfulLand#BIOME}），所以只有福地生成它。</li>
 *   <li>⚠ 每个注册表在同一模组的数据生成周期内只能注册一个标签 Provider，否则会因输出任务重名而报错；新增群系标签必须统
 *       一在{@link #addTags} 中追加。</li>
 * </ul>
 */

public class BiomeTagsProvider extends TagsProvider<Biome> {

    public BiomeTagsProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, Registries.BIOME, lookupProvider, GuWorld.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider registries) {
        TagAppender<Biome> spiritSpring = tag(SpiritSpring.BIOMES);
        Set<ResourceKey<Biome>> land = new LinkedHashSet<>(SpiritSpring.LAND_BIOMES);
        Regions.ALL.forEach(region -> land.addAll(region.biomes().land()));
        land.forEach(spiritSpring::add);
        TagAppender<Biome> intimateGrass = tag(IntimateGrass.BIOMES);
        Set<ResourceKey<Biome>> grassland = new LinkedHashSet<>(SpiritSpring.LAND_BIOMES);
        grassland.addAll(SouthernBorder.REGION.biomes().land());
        grassland.forEach(intimateGrass::add);
        tag(SoulsunderMountain.BIOMES).add(FoxImmortalBlissfulLand.BIOME);
        for (WildCropClimate climate : WildCropClimate.values()) {
            Set<ResourceKey<Biome>> biomes = new LinkedHashSet<>();
            for (Region region : Regions.ALL) biomes.addAll(region.biomes().crops().getOrDefault(climate, List.of()));
            TagAppender<Biome> appender = tag(climate.tag());
            biomes.forEach(appender::add);
        }
    }
}
