package net.alex.guzhenrenworld.datagen;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.datagen.lang.EnUsLanguageProvider;
import net.alex.guzhenrenworld.datagen.lang.ZhCnLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Wires every generator of this mod that runs at datagen time.
 *
 * <p>Annotated {@code @EventBusSubscriber}. On {@code GatherDataEvent} it adds every provider under
 * {@code datagen/}. The output is the committed source set {@code src/generated/resources}, written by
 * {@code runData}. The data run loads Guzhenren too, as a required mod, but NeoForge only executes the
 * generators of the mod named by {@code --mod}, so Guzhenren's providers write nothing here.
 *
 * <p>⚠ What they write is a committed source set, so a provider changed without regenerating ships a
 * stale jar while the build stays perfectly green.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
public final class DataGenerators {

    private DataGenerators() {}

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new EnUsLanguageProvider(packOutput));
        generator.addProvider(event.includeClient(), new ZhCnLanguageProvider(packOutput));

        DatapackProvider datapackProvider = generator.addProvider(event.includeServer(),
                new DatapackProvider(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), new WorldBiomeTagsProvider(packOutput,
                datapackProvider.getRegistryProvider(), existingFileHelper));
    }
}
