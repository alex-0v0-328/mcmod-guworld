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
 * The single provider for every block tag this mod writes.
 *
 * <p>Extends {@link TagsProvider} for {@link Block}. Writes the block tags the features ported from Terralith match
 * against ({@link Ports#getBlockTags}): each entry a block id, or a tag id after a {@code #}.
 *
 * <p>⚠ There can only be one per data run: a tags provider is named after its registry and mod, so a
 * second block tags provider fails datagen as a duplicate; add the next block tag here instead.
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
