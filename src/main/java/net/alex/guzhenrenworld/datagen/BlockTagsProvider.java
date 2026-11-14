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
 * 本模组全部方块标签的唯一 provider。
 *
 * <ul>
 *   <li>Terralith 移植地物所匹配的方块标签（{@link Ports#getBlockTags}）；每项为方块 ID，或 {@code #} 加标签 ID。</li>
 *   <li>⚠ 每次数据生成仅一个：标签 provider 以注册表与模组命名，再加一个即重名失败；新方块标签加在这里。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
