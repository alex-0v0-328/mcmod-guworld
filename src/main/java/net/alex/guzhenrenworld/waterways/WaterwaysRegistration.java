package net.alex.guzhenrenworld.waterways;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 水系系统的注册、数据包引导与服务端维度生命周期事件入口。
 * <ul>
 *   <li>{@link #onRegister} 在模组总线上注册密度函数类型 {@code guworld:waterways}（{@link WaterwaysDensity#MAP_CODEC}
 *       ）与同名地物 {@link WaterwaysFeature}（数据生成阶段同样触发，以供数据包生成器编码）；服务端维度加载、卸载与停服
 *       事件则转发给{@link WaterwaysRuntime} 管理雕刻器实例。</li>
 *   <li>三个 {@code register*} 引导方法由 {@code datagen.DatapackProvider} 调用：将无放置修饰器（每区块固定执行一次）的
 *       水系地物在 {@code RAW_GENERATION} 阶段挂载到传入的四域全量群系名册上。</li>
 * </ul>
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
public final class WaterwaysRegistration {

    private static final ResourceLocation ID = GuWorld.id(WaterwaysFeature.ID);
    public static final DeferredHolder<Feature<?>, WaterwaysFeature> FEATURE =
            DeferredHolder.create(Registries.FEATURE, ID);
    private static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_FEATURE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ID);
    private static final ResourceKey<PlacedFeature> PLACED_FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, ID);
    private static final ResourceKey<BiomeModifier> BIOME_MODIFIER =
            ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ID);

    private WaterwaysRegistration() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.DENSITY_FUNCTION_TYPE, GuWorld.id(WaterwaysDensity.ID),
                () -> WaterwaysDensity.MAP_CODEC);
        event.register(Registries.FEATURE, ID, WaterwaysFeature::new);
    }

    public static void registerConfiguredFeature(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(CONFIGURED_FEATURE, new ConfiguredFeature<>(FEATURE.get(), NoneFeatureConfiguration.INSTANCE));
    }

    public static void registerPlacedFeature(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);
        context.register(PLACED_FEATURE,
                new PlacedFeature(configuredFeatures.getOrThrow(CONFIGURED_FEATURE), List.of()));
    }

    public static void registerBiomeModifier(BootstrapContext<BiomeModifier> context,
            Collection<ResourceKey<Biome>> biomes) {
        HolderGetter<Biome> lookup = context.lookup(Registries.BIOME);
        context.register(BIOME_MODIFIER, new BiomeModifiers.AddFeaturesBiomeModifier(
                HolderSet.direct(biomes.stream().distinct().sorted(Comparator.comparing(ResourceKey::location))
                        .map(lookup::getOrThrow).toList()),
                HolderSet.direct(context.lookup(Registries.PLACED_FEATURE).getOrThrow(PLACED_FEATURE)),
                GenerationStep.Decoration.RAW_GENERATION));
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) WaterwaysRuntime.onLevelLoad(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) WaterwaysRuntime.onLevelUnload(level);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WaterwaysRuntime.clear();
    }
}
