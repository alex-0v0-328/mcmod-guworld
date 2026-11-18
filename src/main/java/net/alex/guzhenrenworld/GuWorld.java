package net.alex.guzhenrenworld;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;

/**
 * Gu World 模组主类，作为主模组（{@code guzhenren}）的独立世界生成配套模组运行。
 * <ul>
 *   <li>职责边界：本模组负责全部自定义维度、地形、生物群系、水系以及元泉[Spirit Spring]、野生知心草等自然生成的结构与地
 *       物；主模组负责玩家数据、跨维度旅行指令（{@code /guworld}）、维度守卫以及元泉与知心草的方块、流体、物品与渲染。
 *       </li>
 *   <li>跨模组解耦：本模组在运行期依赖主模组并在其后加载，但无编译期依赖；主模组仅按维度键引用本模组的维度（
 *       {@code net.alex.guzhenren.registry.world.ModDimensions}），本模组仅按注册 ID 引用主模组的方块（
 *       {@link GuzhenrenBlocks}）。</li>
 *   <li>本类仅提供 {@link #MOD_ID} 及资源标识构造工具（{@link #id}、{@link #key}）；各功能子包（{@code biome}、
 *       {@code dimension}、{@code feature}、{@code waterways}、{@code client}、{@code datagen}）通过各自的
 *       {@code @EventBusSubscriber} 或数据生成器独立挂载，根包不反向依赖子包。</li>
 *   <li>⚠ 根包名固定为 {@code net.alex.guzhenrenworld}，严禁改为 {@code net.alex.guzhenren.world}：主模组已占用
 *       {@code net.alex.guzhenren} 包前缀，而 Java 模块系统禁止两个模组 Jar 拆分同一个包（Split Package）。</li>
 * </ul>
 */

@Mod(GuWorld.MOD_ID)
public class GuWorld {

    public static final String MOD_ID = "guworld";

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }

    public static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry, String path) {
        return ResourceKey.create(registry, id(path));
    }
}
