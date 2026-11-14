package net.alex.guzhenrenworld;

import java.util.OptionalLong;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * 露天 Gu World 维度的维度类型，同主世界。
 * <ul>
 *   <li>{@link #create} 固定与主世界相同的字段：无固定时间、有天空光照、无顶棚、非极热、自然维度（时钟、指南针、床可
 *       用，床设重生点）、坐标缩放 1、逻辑高度即总高度、{@code #minecraft:infiniburn_overworld}、无环境光；调用方给高度
 *       范围、重生锚是否可用、天空效果 ID、刷怪设置。</li>
 *   <li>放根包、{@link GuWorld} 旁，{@code dimension} 下各域共用而互不依赖；不引用功能包。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class SurfaceDimensionTypes {

    private static final boolean HAS_SKY_LIGHT = true;
    private static final boolean HAS_CEILING = false;
    private static final boolean ULTRA_WARM = false;
    private static final boolean NATURAL = true;
    private static final double COORDINATE_SCALE = 1.0;
    private static final boolean BED_WORKS = true;
    private static final float AMBIENT_LIGHT = 0.0F;

    private SurfaceDimensionTypes() {}

    public static DimensionType create(int minY, int height, boolean respawnAnchorWorks, ResourceLocation effects,
            DimensionType.MonsterSettings monsters) {
        return new DimensionType(OptionalLong.empty(), HAS_SKY_LIGHT, HAS_CEILING, ULTRA_WARM, NATURAL,
                COORDINATE_SCALE, BED_WORKS, respawnAnchorWorks, minY, height, height, BlockTags.INFINIBURN_OVERWORLD,
                effects, AMBIENT_LIGHT, monsters);
    }
}
