package net.alex.guzhenrenworld.datagen;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.WildCropClimate;
import net.alex.guzhenrenworld.dimension.Regions;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.feature.SpiritSpring;
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
 * 本模组全部群系标签的唯一 provider。
 *
 * <ul>
 *   <li>元泉[Spirit Spring]生成标签 {@link SpiritSpring#BIOMES}：{@link SpiritSpring#LAND_BIOMES}（元泉包管的主世界名
 *       单）加各域陆地群系；{@link WildCropClimate} 野生作物气候标签取 {@link Regions#ALL} 各域作物气候表；两域共有的群
 *       系（雨林）每标签只记一次。</li>
 *   <li>⚠ 每次数据生成仅一个：标签 provider 以注册表与模组命名，再加一个即重名失败；新群系标签加在这里。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
        for (WildCropClimate climate : WildCropClimate.values()) {
            Set<ResourceKey<Biome>> biomes = new LinkedHashSet<>();
            for (Region region : Regions.ALL) biomes.addAll(region.biomes().crops().getOrDefault(climate, List.of()));
            TagAppender<Biome> appender = tag(climate.tag());
            biomes.forEach(appender::add);
        }
    }
}
