package net.alex.guzhenrenworld;

import java.util.OptionalLong;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Builds the dimension type of a Gu World dimension that lives under an open sky, as the overworld does.
 *
 * <p>{@link #create} fixes what every such dimension shares with the overworld's type: no fixed time, sky light and no
 * ceiling, not ultra-warm, natural (clocks, compasses and beds work, and a bed sets the spawn), coordinate scale 1,
 * logical height the whole height, the overworld's infiniburn tag and no ambient light. The caller names the rest:
 * the height range, whether respawn anchors work, the special-effects id and the monster settings.
 *
 * <p>Lives in the root package, beside {@link GuWorld}, so the regions under {@code dimension} share it
 * without one depending on another; it names nothing from the function packages.
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
