package net.alex.guzhenrenworld.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Guzhenren blocks this mod places, named by registry id alone.
 *
 * <p>This mod never compiles against Guzhenren, so each block is a {@link DeferredHolder} on
 * Guzhenren's id that binds once the block registry is filled. Guzhenren is a required mod, so
 * {@code get()} throws only on a wrong id, never on a missing mod. {@link #SPIRIT_SPRING} is the Spirit
 * Spring [元泉] liquid block; its fluid is read off the block's own state, so it needs no second id.
 *
 * <p>⚠ Each id mirrors a block in Guzhenren's {@code net.alex.guzhenren.registry.block.ModBlocks}:
 * renaming the block there means renaming it here in the same task, or the spring structure fails when
 * it first generates.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class GuzhenrenBlocks {

    private GuzhenrenBlocks() {}

    public static final DeferredHolder<Block, Block> SPIRIT_SPRING = DeferredHolder.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("guzhenren", "spirit_spring"));
}
