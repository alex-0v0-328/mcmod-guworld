package net.alex.guzhenrenworld;

import java.util.OptionalLong;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * 本模组露天维度（四域与宝黄天）共用的 {@link DimensionType} 构建工具。{@link #create} 将露天维度的公共属性固定为与原版
 * 主世界一致（无固定时间、有天空光照、无基岩顶棚、非极热、自然维度使钟与指南针及床正常生效、坐标缩放为 1、逻辑高度等于
 * 总高度、使用主世界无限燃烧标签、无额外环境光），仅由调用方指定垂直高度范围、重生锚是否可用、客户端天空效果 ID 与怪物
 * 生成设置。
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
