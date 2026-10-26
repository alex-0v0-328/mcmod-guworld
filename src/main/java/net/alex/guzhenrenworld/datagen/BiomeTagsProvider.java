package net.alex.guzhenrenworld.datagen;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.WildCropClimate;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSeaLayout;
import net.alex.guzhenrenworld.dimension.easternsea.EasternSeaRoster;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlainsLayout;
import net.alex.guzhenrenworld.dimension.northernplains.NorthernPlainsRoster;
import net.alex.guzhenrenworld.dimension.southernborder.BiomeLayout;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesertLayout;
import net.alex.guzhenrenworld.dimension.westerndesert.WesternDesertRoster;
import net.alex.guzhenrenworld.feature.SpiritSpring;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The single provider for every biome tag this mod writes.
 *
 * <p>Extends {@link TagsProvider} for {@link Biome}. Fills the Spirit Spring [元泉] generation tag
 * {@link SpiritSpring#BIOMES} from {@link SpiritSpring#LAND_BIOMES}, the overworld list the spring's package owns,
 * and each region's land biomes ({@link BiomeLayout#LAND} and the other regions' layouts), and the wild crop climate
 * tags of {@link WildCropClimate} from each region's crop map; a biome two regions both place, the rainforest, enters
 * each tag once.
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
        TagAppender<Biome> spiritSpring = tag(SpiritSpring.BIOMES);
        Set<ResourceKey<Biome>> land = new LinkedHashSet<>(SpiritSpring.LAND_BIOMES);
        land.addAll(BiomeLayout.LAND);
        land.addAll(WesternDesertLayout.LAND);
        land.addAll(EasternSeaLayout.LAND);
        land.addAll(NorthernPlainsLayout.LAND);
        land.forEach(spiritSpring::add);
        for (WildCropClimate climate : WildCropClimate.values()) {
            Set<ResourceKey<Biome>> biomes = new LinkedHashSet<>();
            for (Map<WildCropClimate, List<ResourceKey<Biome>>> crops : List.of(BiomeLayout.CROPS,
                    WesternDesertRoster.CROPS, EasternSeaRoster.CROPS, NorthernPlainsRoster.CROPS)) {
                biomes.addAll(crops.getOrDefault(climate, List.of()));
            }
            TagAppender<Biome> appender = tag(climate.tag());
            biomes.forEach(appender::add);
        }
    }
}
