package net.alex.guzhenrenworld.registry;

import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.datagen.DatapackProvider;
import net.alex.guzhenrenworld.feature.SpiritSpringFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The worldgen features [世界生成] this mod registers: the Spirit Spring [元泉] surface structure (its
 * underground cave variant was removed on 2026-10-02, Alex).
 *
 * <p>Placement data -- the configured and placed feature JSON and the biome modifiers -- is datagen'd
 * in {@link DatapackProvider}; this class only holds the {@code Feature} instances. The spring's
 * worldgen moved here from Guzhenren on 2026-10-02 (Alex), which keeps the block, fluid, item and
 * client rendering, so every worldgen id changed namespace from {@code guzhenren} to {@code guworld}.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public final class WorldFeatures {

    private WorldFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, GuWorld.MOD_ID);
    public static final DeferredHolder<Feature<?>, SpiritSpringFeature> SPIRIT_SPRING =
            FEATURES.register("spirit_spring",
                    SpiritSpringFeature::new);

    public static void register(IEventBus modEventBus) { FEATURES.register(modEventBus); }
}
