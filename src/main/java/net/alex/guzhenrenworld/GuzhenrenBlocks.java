package net.alex.guzhenrenworld;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 本模组放置的主模组方块，只按注册 ID 引用。
 * <ul>
 *   <li>不对主模组编译，故每个方块是指向其 ID 的 {@link DeferredHolder}，方块注册表填好后绑定；主模组为必需依赖，
 *       {@code get()} 只因 ID 写错抛错，不因缺模组；{@link #SPIRIT_SPRING} 是元泉[Spirit Spring]液体方块，流体取自方块
 *       状态，无需第二个 ID。</li>
 *   <li>⚠ 每个 ID 对应主模组 {@code net.alex.guzhenren.registry.block.ModBlocks} 的一个方块：那边改名须同一任务改这里，
 *       否则元泉结构首次生成即失败。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class GuzhenrenBlocks {

    public static final DeferredHolder<Block, Block> SPIRIT_SPRING = DeferredHolder.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("guzhenren", "spirit_spring"));

    private GuzhenrenBlocks() {}
}
