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
 * 本模组数据生成阶段全部 Provider 的统一挂载入口。
 * <ul>
 *   <li>在 {@link GatherDataEvent} 中注册 {@code datagen/} 包下的全部生成器；由于 NeoForge 要求每种注册表或数据包生成器
 *       在单次运行中仅注册一个实例（语言表按语言各一个），因此集中在此处从 {@code biome}、{@code dimension}、
 *       {@code feature} 等功能包汇总数据。</li>
 *   <li>生成产物通过 {@code runData} 写入入库源集 {@code src/generated/resources}；运行期虽会一并加载依赖的主模组，但
 *       NeoForge 仅执行 {@code --mod} 指定模组的生成器，不会触发主模组的 Provider。</li>
 *   <li>⚠ 生成产物直接提交进版本库：修改任何 Provider 后若未重新执行 {@code runData}，常规编译仍会通过，但构建出的 Jar
 *       会带着旧数据。</li>
 * </ul>
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
