package net.alex.guzhenrenworld;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 本模组在世界生成中放置的主模组方块延迟引用表。
 * <ul>
 *   <li>由于本模组不建立对主模组的编译期依赖，所有主模组方块均通过指向其注册 ID 的 {@link DeferredHolder} 在方块注册表
 *       冻结后绑定；{@link #SPIRIT_SPRING} 为元泉[Spirit Spring]液体源方块（流体直接从其默认方块状态获取），
 *       {@link #INTIMATE_GRASS}为野生两格高知心草方块，{@link #SOULSUNDER_STONE} 为荡魂山[Soulsunder Mountain]整座山体唯
 *       一的荡魂山石。</li>
 *   <li>⚠ 各条目 ID 必须与主模组 {@code net.alex.guzhenren.registry.block.ModBlocks} 中的注册名严格一致；主模组改名时必
 *       须在同一任务中同步更新此处，否则元泉首次生成、知心草配置地物加载或荡魂山写入第一个区块时会因找不到方块而直接报
 *       错。</li>
 * </ul>
 */

public final class GuzhenrenBlocks {

    public static final DeferredHolder<Block, Block> SPIRIT_SPRING = DeferredHolder.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("guzhenren", "spirit_spring"));
    public static final DeferredHolder<Block, Block> INTIMATE_GRASS = DeferredHolder.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("guzhenren", "intimate_grass"));
    public static final DeferredHolder<Block, Block> SOULSUNDER_STONE = DeferredHolder.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("guzhenren", "soulsunder_stone"));

    private GuzhenrenBlocks() {}
}
