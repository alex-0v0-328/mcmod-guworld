package net.alex.guzhenrenworld.datagen;

import java.util.concurrent.CompletableFuture;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.datagen.lang.Translations;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * 本模组数据生成期全部生成器的接线。
 *
 * <ul>
 *   <li>{@code GatherDataEvent} 时加入 {@code datagen/} 下全部 provider；NeoForge 每次运行每种只许一个（语言表每语言一
 *       个），故都在此，条目取自功能包 {@code biome}、{@code dimension}、{@code feature}。</li>
 *   <li>产物为入库源集 {@code src/generated/resources}，由 {@code runData} 写出；也加载必需的主模组，但 NeoForge
 *       只执行 {@code --mod} 所指模组的生成器，主模组 provider 不写入。</li>
 *   <li>⚠ 产物入库：改 provider 不重新生成，构建照样全绿，jar 却带旧数据。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
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
