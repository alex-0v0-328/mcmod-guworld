# 01 Java 与数据生成

范围：主线全部 22 个类（维度、群系、地形、元泉、datagen、客户端），连同 `src/ports` 原样搬入。模块划分按实际依赖调整：语言表和 `DatapackProvider` 引用全部内容，拆不开，所以 Java 一次搬完；模块 2 是移植 JSON 的格式兼容，模块 3 是测试。

## 问题

| # | 1.21.1 主线 | 1.20.1 Forge | 处理 |
|---|---|---|---|
| A1 | `BootstrapContext` | `BootstapContext`（原版拼写） | 接缝 `DatapackBootstrap`（回顾 R2） |
| A2 | `DataResult.getOrThrow()` / `getOrThrow(Function)`（DFU 8） | DFU 6 没有 | 接缝 `DataResults.getOrThrow(result, 说明)`（R1） |
| A3 | `RegistryInfo.fromRegistryLookup` | 没有 | 两版都有的构造器（R3） |
| A4 | `new BiomeGenerationSettings(Map, List)` 公开 | 包内可见 | `PlainBuilder` 逐步加地物（R4）；所有群系最后一步非空，产物不变 |
| A5 | `HolderSet.empty()` | 没有 | `HolderSet.direct()`（R5） |
| A6 | `TagAppender.addAll(List)` | 没有 | 逐个 `add`（R6） |
| A7 | `@EventBusSubscriber` 自动分总线、`DeferredHolder`、`BiomeModifiers`、`NeoForgeRegistries` | `@Mod.EventBusSubscriber(bus = MOD)`、`RegistryObject`、`ForgeBiomeModifiers`、`ForgeRegistries.Keys` | 加载器胶水（R9、R10） |
| A8 | `getRegistryProvider()` | 原版 `RegistriesDatapackGenerator` 同名方法 | 不变 |

## 验收结果

| 项 | 结果 |
|---|---|
| `build` | 通过 |
| `runData` | 504 个文件，与主线同路径（`tags/block`→`tags/blocks`、`neoforge/`→`forge/` 除外）；内容差异只有模块 2 的格式规则与 1.20.1 原版群系少中型钻石矿 |
| 服务端冒烟 | 两个维度加载并生成区块，南疆边界 8192，`locate biome` 找到竹海、喀斯特峰林 |

# 02 移植 JSON 的格式兼容

`src/ports` 两版共用（逐字相同），Forge 端 `biome.PortFormat.adapt` 在解码前把 1.21 格式改成 1.20.1 格式；主线同名类原样返回。

| 规则 | 依据 |
|---|---|
| 整数 / 浮点分布 `uniform`、`biased_to_bottom`、`clamped`、`clamped_normal`、`trapezoid` 的字段嵌进 `value`；字段是对象（垂直锚点，即高度分布）时不动 | 1.20.1 这些类型的编解码器不是 MapCodec，DFU 6 分派时包一层 `value`；Terralith 1.20 版 2.5.4 的 JSON 印证 |
| `minecraft:short_grass` → `minecraft:grass` | 1.20.3 改名 |
| `#minecraft:air` → `air`、`void_air`、`cave_air` 三项 | 方块标签 1.20.5 才有 |
| 去掉犰狳、沼骸的刷怪项与刷怪消耗，去掉 `ore_diamond_medium` | 1.20.5 / 1.21 才有 |

发现方法：datagen 报错只给最外层（Forge 的 `HolderSetCodec` 把列表分支的错误吞掉），用一次性 L2 探针逐层解码定位；再扫描全部移植文件的 `type` 与 ID，对照 1.20.1 源码里的注册名。

# 03 测试

| 层 | 内容 | 差异 |
|---|---|---|
| L2 | `SpiritSpringFeatureTest` 3 条 | 加 `TestBootstrap`（照 guzhenren 端 `PureTestBootstrap`：引导原版，放过 `NetworkHooks.init` 的失败）；`junit-platform-launcher` 进 `testImplementation` |
| L3 | `WorldGameTests` 4、`SouthernBorderGameTests` 2、可选 `SouthernBorderNoiseBenchmark` | `@GameTestHolder` 导入；去掉 `skyAccess`（1.20.1 测试结构没有屏障墙和顶）；`fillFromNoise` 多一个 `Executor`；模板放 `structures/` |
| 世界 | Forge 1.20.1 的 GameTest 服务端按 `server.properties` 建世界，没有文件就是普通世界，测试结构埋在地下 | `build.gradle` 在 `runGameTestServer` 前写 `level-type=minecraft:flat` |

结果：L2 3/3；GameTest 6/6；基准 64 区块指纹 `ba1b23368fad0079`，与主线同一指纹（地表高度逐列相同），约 32 ms/区块（主线 29）。
