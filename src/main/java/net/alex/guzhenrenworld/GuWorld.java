package net.alex.guzhenrenworld;

import net.alex.guzhenrenworld.datagen.DatapackProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;

/**
 * Entry point of gu-world, the companion mod of Guzhenren ({@code guzhenren}), built from its own
 * repository beside it.
 *
 * <p>This mod owns every dimension, terrain and biome. Guzhenren owns the player data, the
 * anchored-dimension travel ({@code /guworld}) and the guards that act inside those dimensions. The
 * two meet only at run time: this mod requires Guzhenren and loads after it, yet compiles without it,
 * and Guzhenren names a dimension it relies on by its key alone
 * ({@code net.alex.guzhenren.registry.world.ModDimensions#TREASURE_YELLOW_HEAVEN}). Holds the
 * {@code MOD_ID} constant and the {@link #id} helper. Nothing registers on the mod bus yet: the
 * Treasure Yellow Heaven is datapack-only ({@link DatapackProvider}).
 *
 * <p>⚠ The package is {@code net.alex.guzhenrenworld}, never {@code net.alex.guzhenren.world}:
 * Guzhenren already owns that package, and NeoForge loads every mod as its own module, so a package
 * split across both jars fails to load.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mod(GuWorld.MOD_ID)
public class GuWorld {

    public static final String MOD_ID = "gu_world";

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
}
