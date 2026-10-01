package net.alex.guzhenrenworld.registry;

import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.GuWorldClient;
import net.alex.guzhenrenworld.datagen.DatapackProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Resource keys for the datapack-owned pieces of this mod's dimensions.
 *
 * <p>Holders only: the actual dimension type, biome and level stem are written by
 * {@link DatapackProvider} at datagen time.
 * {@link #TREASURE_YELLOW_HEAVEN_EFFECTS} is the special-effects id the dimension type names and
 * {@link GuWorldClient} registers.
 *
 * <p>⚠ Every Treasure Yellow Heaven key shares the location of Guzhenren's level key
 * {@code net.alex.guzhenren.registry.world.ModDimensions#TREASURE_YELLOW_HEAVEN}; the level stem is
 * what turns that key into a loaded level, so a renamed key here leaves Guzhenren's
 * {@code /guworld enter} pointing at nothing until that key is renamed too.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WorldDimensions {

    private WorldDimensions() {}

    public static final ResourceKey<DimensionType> TREASURE_YELLOW_HEAVEN_TYPE = key(Registries.DIMENSION_TYPE);
    public static final ResourceKey<Biome> TREASURE_YELLOW_HEAVEN_BIOME = key(Registries.BIOME);
    public static final ResourceKey<LevelStem> TREASURE_YELLOW_HEAVEN_STEM = key(Registries.LEVEL_STEM);
    public static final ResourceLocation TREASURE_YELLOW_HEAVEN_EFFECTS = GuWorld.id("treasure_yellow_heaven");

    private static <T> ResourceKey<T> key(ResourceKey<Registry<T>> registry) {
        return ResourceKey.create(registry, GuWorld.id("treasure_yellow_heaven"));
    }
}
