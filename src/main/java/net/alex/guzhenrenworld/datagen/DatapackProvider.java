package net.alex.guzhenrenworld.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.biome.Ports;
import net.alex.guzhenrenworld.biome.Replicas;
import net.alex.guzhenrenworld.dimension.Regions;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandBiomes;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandFoxImmortal;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.region.RegionBootstraps;
import net.alex.guzhenrenworld.dimension.southernborder.BambooSeaRegistration;
import net.alex.guzhenrenworld.feature.IntimateGrass;
import net.alex.guzhenrenworld.feature.SpiritSpring;
import net.alex.guzhenrenworld.feature.SpiritSpringRegistration;
import net.alex.guzhenrenworld.secretrealm.SoulsunderMountain;
import net.alex.guzhenrenworld.waterways.Waterways;
import net.alex.guzhenrenworld.waterways.WaterwaysRegistration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 本模组全部数据包内置注册表条目的唯一生成器。
 * <ul>
 *   <li>汇总注册宝黄天的维度类型、群系与维度层级（{@link TreasureYellowHeaven}），南疆自制的竹海群系与稀疏竹子放置地物
 *       （{@link BambooSeaRegistration}），元泉配置地物、放置地物（{@link SpiritSpring}）与群系修改器
 *       （{@link SpiritSpringRegistration}），野生知心草地物与群系修改器（{@link IntimateGrass}），以及四域水系地物与修
 *       改器（{@link WaterwaysRegistration}，福地的群系也在内）；另有狐仙福地的维度类型、群系与复刻
 *       （{@link BlissfulLandBiomes}）、噪声、Tectonic 副本、噪声设置与维度层级（{@link BlissfulLandFoxImmortal}），以
 *       及荡魂山的结构与缓地放置的结构集（{@link SoulsunderMountain}）。</li>
 *   <li>注册四域共用的维度类型，并按 {@link Regions#ALL} 逐域注册各自的密度函数（Tectonic 路由与域钩子）、噪声生成设
 *       置、维度层级（{@link RegionBootstraps}）以及挂载在各域前缀下的可达移植地形副本。</li>
 *   <li>汇总注册原版复刻群系（{@link Replicas#SHARED} 加各域独有复刻）、Terralith 移植群系及地物（{@link Ports#BIOMES}
 *       加各域独有移植），以及移植与水系共用的噪声参数（{@link Ports}、{@link Waterways}）。</li>
 *   <li>多个维度共用的注册表统一在同一个 Bootstrap 回调内完成注册；下游群系标签生成器必须读取本实例的
 *       {@link #getRegistryProvider()}，才能解析本次运行新生成的群系键。</li>
 *   <li>⚠ {@link DatapackBuiltinEntriesProvider} 的任务名称固定，单次数据生成只能注册一个实例；新增数据包注册表必须并
 *       入 {@link #BUILDER}。</li>
 * </ul>
 */

public class DatapackProvider extends DatapackBuiltinEntriesProvider {

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, context -> {
                TreasureYellowHeaven.registerDimensionType(context);
                RegionBootstraps.registerDimensionType(context);
                BlissfulLandFoxImmortal.registerDimensionType(context);
            })
            .add(Registries.BIOME, context -> {
                TreasureYellowHeaven.registerBiome(context);
                Replicas.registerBiomes(context, getJoined(Replicas.SHARED, Region.Biomes::replicas));
                Ports.registerBiomes(context, getJoined(Ports.BIOMES, Region.Biomes::ported));
                BambooSeaRegistration.registerBiomes(context);
                Replicas.registerBiomes(context, BlissfulLandBiomes.REPLICAS);
            })
            .add(Registries.NOISE, context -> {
                Ports.registerNoises(context);
                Regions.ALL.forEach(region -> RegionBootstraps.registerNoises(context, region));
                Waterways.registerNoises(context);
                BlissfulLandFoxImmortal.registerNoises(context);
            })
            .add(Registries.DENSITY_FUNCTION, context -> {
                Regions.ALL.forEach(region -> RegionBootstraps.registerDensityFunctions(context, region));
                BlissfulLandFoxImmortal.registerDensityFunctions(context);
            })
            .add(Registries.NOISE_SETTINGS, context -> {
                Regions.ALL.forEach(region -> RegionBootstraps.registerNoiseSettings(context, region));
                BlissfulLandFoxImmortal.registerNoiseSettings(context);
            })
            .add(Registries.LEVEL_STEM, context -> {
                TreasureYellowHeaven.registerLevelStem(context);
                Regions.ALL.forEach(region -> RegionBootstraps.registerLevelStem(context, region));
                BlissfulLandFoxImmortal.registerLevelStem(context);
            })
            .add(Registries.CONFIGURED_FEATURE, context -> {
                SpiritSpring.registerConfiguredFeature(context);
                IntimateGrass.registerConfiguredFeature(context);
                WaterwaysRegistration.registerConfiguredFeature(context);
                Ports.registerConfiguredFeatures(context);
            })
            .add(Registries.PLACED_FEATURE, context -> {
                SpiritSpring.registerPlacedFeature(context);
                IntimateGrass.registerPlacedFeature(context);
                WaterwaysRegistration.registerPlacedFeature(context);
                Ports.registerPlacedFeatures(context);
                BambooSeaRegistration.registerPlacedFeatures(context);
            })
            .add(Registries.STRUCTURE, SoulsunderMountain::registerStructure)
            .add(Registries.STRUCTURE_SET, SoulsunderMountain::registerStructureSet)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, context -> {
                SpiritSpringRegistration.registerBiomeModifier(context);
                IntimateGrass.registerBiomeModifier(context);
                WaterwaysRegistration.registerBiomeModifier(context, Stream.concat(Regions.ALL.stream()
                        .flatMap(region -> region.biomes().roster().stream()),
                        BlissfulLandBiomes.ROSTER.stream()).toList());
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
