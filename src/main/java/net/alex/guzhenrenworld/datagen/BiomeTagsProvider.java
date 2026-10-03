package net.alex.guzhenrenworld.datagen;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.feature.SpiritSpring;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The single provider for every biome tag this mod writes.
 *
 * <p>Extends {@link TagsProvider} for {@link Biome}. Fills the Spirit Spring [元泉] generation tag
 * {@link SpiritSpring#BIOMES} from {@link SpiritSpring#LAND_BIOMES}, the list the spring's package owns.
 *
 * <p>⚠ There can only be one per data run: a tags provider is named after its registry and mod, so a
 * second biome tags provider fails datagen as a duplicate; add the next biome tag here instead.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class BiomeTagsProvider extends TagsProvider<Biome> {

    public BiomeTagsProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(packOutput, Registries.BIOME, lookupProvider, GuWorld.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider registries) {
        tag(SpiritSpring.BIOMES).addAll(SpiritSpring.LAND_BIOMES);
    }
}
