package net.alex.guzhenrenworld.biome;

import java.util.Locale;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 农夫乐事[Farmer's Delight]野生作物在各域中的生长气候分类枚举。
 * <ul>
 *   <li>农夫乐事原本通过自带的群系修改器，依据原版与通用标签（{@code #minecraft:is_overworld} 配温度区间、
 *       {@code #minecraft:is_beach}、{@code #c:is_wet/overworld}、{@code #c:is_hot/overworld}）投放野生作物；但本模组各
 *       域不生成原版要塞与埋藏的宝藏等结构，因此域群系不加入 {@code is_overworld} 或 {@code is_beach} 等标签。</li>
 *   <li>各域改为将自身群系归入本枚举的 5 类气候，由 {@link net.alex.guzhenrenworld.datagen.BiomeTagsProvider} 汇总写出
 *       为{@code guworld:farmersdelight/<climate>} 群系标签；
 *       {@code data/guworld/neoforge/biome_modifier/farmersdelight/} 下的手写群系修改器通过 {@code neoforge:conditions}
 *       仅在安装农夫乐事时加载，将对应作物丛注入这些标签。</li>
 *   <li>5 类气候分别对应不同野生作物：{@link #HOT} 产番茄（干热）、{@link #WET} 产水稻（湿润）、{@link #BEACH} 产卷心菜
 *       与甜菜根（海岸）、{@link #TEMPERATE} 产胡萝卜与洋葱（温带）、{@link #COLD} 产马铃薯（寒冷）。</li>
 *   <li>群系划分统一依据群系 JSON 的温度与降水量：温带为温度 0.4–0.9；寒冷为温度 0.1–0.3；干热为温度 ≥ 1.0 且降水量
 *       &lt; 0.5；湿润为降水量 ≥ 0.8 或名称含 {@code swamp}、{@code marsh}、{@code jungle}、{@code rainforest}、
 *       {@code oasis}；海岸为名称含 {@code beach} 或 {@code shore}；水域（海洋、河流）与洞穴不归入任何气候，陆地群系可
 *       归入多个气候或不属于任何气候。</li>
 * </ul>
 */

public enum WildCropClimate {
    HOT, WET, BEACH, TEMPERATE, COLD;

    private final TagKey<Biome> tag = TagKey.create(Registries.BIOME,
            GuWorld.id("farmersdelight/" + name().toLowerCase(Locale.ROOT)));

    public TagKey<Biome> tag() {
        return tag;
    }
}
