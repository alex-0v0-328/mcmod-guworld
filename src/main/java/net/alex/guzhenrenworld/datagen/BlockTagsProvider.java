package net.alex.guzhenrenworld.datagen;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 本模组方块标签的唯一数据生成器。
 * <ul>
 *   <li>负责写出 Terralith 移植地物所匹配的方块标签（{@link Ports#getBlockTags}），每条目可为直接方块 ID 或以 {@code #}
 *       开头的子标签引用。</li>
 *   <li>⚠ 每个注册表在同一模组的数据生成周期内只能注册一个标签 Provider，否则会因输出任务重名而报错；新增方块标签必须统
 *       一在{@link #addTags} 中追加。</li>
 * </ul>
 */

public class BlockTagsProvider extends TagsProvider<Block> {

    public BlockTagsProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, Registries.BLOCK, lookupProvider, GuWorld.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider registries) {
        for (Map.Entry<ResourceLocation, List<String>> tag : Ports.getBlockTags().entrySet()) {
            TagAppender<Block> appender = tag(TagKey.create(Registries.BLOCK, tag.getKey()));
            for (String value : tag.getValue()) {
                if (value.startsWith("#")) {
                    appender.addTag(TagKey.create(Registries.BLOCK, ResourceLocation.parse(value.substring(1))));
                } else {
                    appender.add(ResourceKey.create(Registries.BLOCK, ResourceLocation.parse(value)));
                }
            }
        }
    }
}
