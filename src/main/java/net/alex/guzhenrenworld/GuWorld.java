package net.alex.guzhenrenworld;

import net.alex.guzhenrenworld.datagen.DatapackProvider;
import net.alex.guzhenrenworld.registry.WorldFeatures;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Entry point of Gu World, the companion mod of Guzhenren ({@code guzhenren}), built from its own
 * repository beside it.
 *
 * <p>This mod owns every dimension, terrain and biome, and the naturally generated unique structures
 * such as the Spirit Spring [元泉]. Guzhenren owns the player data, the anchored-dimension travel
 * ({@code /guworld}), the guards that act inside those dimensions, and the spring's block, fluid and
 * item. The two meet only at run time: this mod requires Guzhenren and loads after it, yet compiles
 * without it; Guzhenren names a dimension it relies on by its key alone
 * ({@code net.alex.guzhenren.registry.world.ModDimensions#TREASURE_YELLOW_HEAVEN}), and this mod names
 * a Guzhenren block by its id alone ({@link net.alex.guzhenrenworld.registry.GuzhenrenBlocks}). Holds
 * the {@code MOD_ID} constant and the {@link #id} helper. The constructor registers the worldgen
 * features ({@link WorldFeatures}); everything else is datapack-only ({@link DatapackProvider}).
 *
 * <p>⚠ The package is {@code net.alex.guzhenrenworld}, never {@code net.alex.guzhenren.world}:
 * Guzhenren already owns the {@code net.alex.guzhenren} packages, and NeoForge loads every mod as its
 * own module, so a package split across both jars fails to load.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mod(GuWorld.MOD_ID)
public class GuWorld {

    public static final String MOD_ID = "guworld";

    public GuWorld(IEventBus modEventBus) {
        WorldFeatures.register(modEventBus);
    }

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
}
