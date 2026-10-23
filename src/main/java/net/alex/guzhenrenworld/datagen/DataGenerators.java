package net.alex.guzhenrenworld.datagen;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.datagen.lang.Translations;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Wires every generator of this mod that runs at datagen time.
 *
 * <p>Annotated {@code @Mod.EventBusSubscriber}. On {@code GatherDataEvent} it adds every provider under
 * {@code datagen/}. Each is one Forge allows only once per run (the language table once per language), so it
 * lives here and collects its
 * entries from the function packages ({@code biome}, {@code dimension}, {@code feature}). The output is the committed
 * source set {@code src/generated/resources}, written by {@code runData}. The data run loads Guzhenren
 * too, as a required mod, but Forge only executes the generators of the mod named by {@code --mod},
 * so Guzhenren's providers write nothing here.
 *
 * <p>⚠ What they write is a committed source set, so a provider changed without regenerating ships a
 * stale jar while the build stays perfectly green.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@Mod.EventBusSubscriber(modid = GuWorld.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {

    private DataGenerators() {}

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        for (Translations.Language language : Translations.Language.values()) {
            generator.addProvider(event.includeClient(), new Translations(packOutput, language));
        }

        DatapackProvider datapackProvider = generator.addProvider(event.includeServer(),
                new DatapackProvider(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new BiomeTagsProvider(packOutput,
                datapackProvider.getRegistryProvider(), existingFileHelper));
        generator.addProvider(event.includeServer(), new BlockTagsProvider(packOutput, lookupProvider,
                existingFileHelper));
    }
}
