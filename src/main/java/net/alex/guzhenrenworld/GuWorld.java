package net.alex.guzhenrenworld;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;

/**
 * Gu World 入口类：主模组（{@code guzhenren}）的配套模组，在其旁的独立仓库构建。
 * <ul>
 *   <li>本模组管全部维度、地形、群系及元泉[Spirit Spring]等自然生成的独特结构；主模组管玩家数据、锚定维度传送
 *       （{@code /guworld}）、维度内的守卫及元泉的方块、流体、物品。</li>
 *   <li>只在运行时相接：本模组依赖主模组、后于它加载，编译不依赖它；主模组只按键引用维度
 *       （{@code net.alex.guzhenren.registry.world.ModDimensions#TREASURE_YELLOW_HEAVEN}），本模组只按 ID 引用其方块
 *       （{@link GuzhenrenBlocks}）。</li>
 *   <li>本类只有 {@code MOD_ID} 与 {@link #id}、{@link #key}；功能各占一包（{@code biome}；{@code dimension}，每域一子
 *       包；{@code feature}；仅客户端的绘制在 {@code client}），经 {@code @EventBusSubscriber} 或 {@code datagen}
 *       provider 自行接线，根包不依赖它们，包图无环。</li>
 *   <li>⚠ 包名是 {@code net.alex.guzhenrenworld}，不可用 {@code net.alex.guzhenren.world}：主模组已占
 *       {@code net.alex.guzhenren}，NeoForge 每个模组一个模块，同一包拆在两个 jar 里无法加载。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mod(GuWorld.MOD_ID)
public class GuWorld {

    public static final String MOD_ID = "guworld";

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }

    public static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry, String path) {
        return ResourceKey.create(registry, id(path));
    }
}
