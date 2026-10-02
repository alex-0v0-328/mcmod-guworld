package net.alex.guzhenrenworld.registry;

import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.datagen.WorldBiomeTagsProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * The biome tags this mod declares.
 *
 * <p>Tag-key holder (not a DeferredRegister): {@link #SPIRIT_SPRING_GENERATES} is the land biomes the
 * Spirit Spring [元泉] structure generates in, filled by {@link WorldBiomeTagsProvider}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WorldBiomeTags {

    private WorldBiomeTags() {}

    public static final TagKey<Biome> SPIRIT_SPRING_GENERATES =
            TagKey.create(Registries.BIOME, GuWorld.id("spirit_spring_generates"));
}
