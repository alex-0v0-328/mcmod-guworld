package net.alex.guzhenrenworld.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.Regions;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.alex.guzhenrenworld.dimension.southernborder.BiomeSettings;
import net.alex.guzhenrenworld.feature.SpiritSpring;
import net.alex.guzhenrenworld.feature.SpiritSpringRegistration;
import net.alex.guzhenrenworld.waterways.Waterways;
import net.alex.guzhenrenworld.waterways.WaterwaysRegistration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 本模组全部数据包注册表的唯一 provider。
 *
 * <ul>
 *   <li>宝黄天[Treasure Yellow Heaven]的维度类型、群系、层级（{@link TreasureYellowHeaven}）；南疆竹海及其竹子
 *       （{@link BiomeSettings}）；元泉[Spirit Spring]配置地物、放置地物（{@link SpiritSpring}）与群系修改器
 *       （{@link SpiritSpringRegistration}）。</li>
 *   <li>四域共用的维度类型；每域（{@link Regions#ALL}）的密度函数（Tectonic 路由加本域钩子）、噪声设置、
 *       层级（{@link RegionBootstraps}），及本域路由够到的移植地形副本，挂本域前缀下。</li>
 *   <li>群系：原版复刻（{@link Replicas#SHARED} 加各域的）、Terralith 移植群系连同地物（{@link Ports#BIOMES}
 *       加各域的）；两份移植共读的噪声（{@link Ports}）。</li>
 *   <li>两维度共填的注册表只用一个引导，内含两边；标签 provider 取本实例 {@code getRegistryProvider()} 而非普通查找，
 *       才看得到本次生成的群系。</li>
 *   <li>⚠ 每次数据生成只能一个：内置条目 provider 名字固定，第二个实例直接失败；新注册表加进本实例构建器。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class DatapackProvider extends DatapackBuiltinEntriesProvider {

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, context -> {
                TreasureYellowHeaven.registerDimensionType(context);
                RegionBootstraps.registerDimensionType(context);
            })
            .add(Registries.BIOME, context -> {
                TreasureYellowHeaven.registerBiome(context);
                Replicas.registerBiomes(context, getJoined(Replicas.SHARED, Region.Biomes::replicas));
                Ports.registerBiomes(context, getJoined(Ports.BIOMES, Region.Biomes::ported));
                BiomeSettings.registerBiomes(context);
            })
            .add(Registries.NOISE, context -> {
                Ports.registerNoises(context);
                Regions.ALL.forEach(region -> RegionBootstraps.registerNoises(context, region));
                Waterways.registerNoises(context);
            })
            .add(Registries.DENSITY_FUNCTION, context -> {
                Regions.ALL.forEach(region -> RegionBootstraps.registerDensityFunctions(context, region));
            })
            .add(Registries.NOISE_SETTINGS, context -> {
                Regions.ALL.forEach(region -> RegionBootstraps.registerNoiseSettings(context, region));
            })
            .add(Registries.LEVEL_STEM, context -> {
                TreasureYellowHeaven.registerLevelStem(context);
                Regions.ALL.forEach(region -> RegionBootstraps.registerLevelStem(context, region));
            })
            .add(Registries.CONFIGURED_FEATURE, context -> {
                SpiritSpring.registerConfiguredFeature(context);
                WaterwaysRegistration.registerConfiguredFeature(context);
                Ports.registerConfiguredFeatures(context);
            })
            .add(Registries.PLACED_FEATURE, context -> {
                SpiritSpring.registerPlacedFeature(context);
                WaterwaysRegistration.registerPlacedFeature(context);
                Ports.registerPlacedFeatures(context);
                BiomeSettings.registerPlacedFeatures(context);
            })
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                SpiritSpringRegistration.registerBiomeModifier(context);
                WaterwaysRegistration.registerBiomeModifier(context, Regions.ALL.stream()
                        .flatMap(region -> region.biomes().roster().stream()).toList());
            });

    public DatapackProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider, BUILDER, Set.of(GuWorld.MOD_ID));
    }

    private static <T> List<T> getJoined(List<T> shared, Function<Region.Biomes, List<T>> part) {
        List<T> joined = new ArrayList<>(shared);
        Regions.ALL.forEach(region -> joined.addAll(part.apply(region.biomes())));
        return List.copyOf(joined);
    }
}
