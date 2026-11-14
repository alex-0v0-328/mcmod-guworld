package net.alex.guzhenrenworld.biome;

import java.util.Locale;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 农夫乐事野生作物在各域的生长气候。
 * <ul>
 *   <li>农夫乐事用自己的群系修改器放作物丛，按原版与约定标签选群系（{@code #minecraft:is_overworld} 加温度区间、
 *       {@code #minecraft:is_beach}、{@code #c:is_wet/overworld}、{@code #c:is_hot/overworld}）；Gu World 群系不入这些
 *       标签：{@code is_overworld} 是原版要塞的群系表，{@code is_beach} 是埋藏的宝藏的，而域内不生成原版结构。</li>
 *   <li>改由各域把群系分入 5 种气候，标签 provider 每种写一个 {@code guworld:farmersdelight/<climate>}；
 *       {@code data/guworld/neoforge/biome_modifier/farmersdelight/} 下的手写修改器仅在装了农夫乐事时加载
 *       （{@code neoforge:conditions}），照该模组的方式把作物丛加到标签。</li>
 *   <li>{@link #HOT} 番茄（热而干）；{@link #WET} 水稻（湿润）；{@link #BEACH} 卷心菜、甜菜根（海岸）；
 *       {@link #TEMPERATE} 胡萝卜、洋葱；{@link #COLD} 马铃薯。</li>
 *   <li>统一按群系 JSON 的温度、降水量分：温带 0.4–0.9；寒冷 0.1–0.3；炎热为温度 1.0 起且降水量低于 0.5；湿润为降水量
 *       0.8 起，或名含 swamp、marsh、jungle、rainforest、oasis；海岸名含 beach 或 shore；水域（海洋、河流）与洞穴不属任
 *       何气候，陆地群系可属多种或一种不属。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public enum WildCropClimate {
    HOT, WET, BEACH, TEMPERATE, COLD;

    private final TagKey<Biome> tag = TagKey.create(Registries.BIOME,
            GuWorld.id("farmersdelight/" + name().toLowerCase(Locale.ROOT)));

    public TagKey<Biome> tag() {
        return tag;
    }
}
