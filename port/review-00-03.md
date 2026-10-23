# 可移植性回顾：模块 0–3

| # | 问题 | 改法（两端同改） |
|---|---|---|
| R1 | `DataResult.getOrThrow` 只在 DFU 8 | 根包 `DataResults.getOrThrow(result, 说明)`，只用 `result()` 和错误的 `message()`；两端同一份 |
| R2 | 引导上下文类名随版本变，7 个功能类因此不同 | 根包接口 `DatapackBootstrap`（`register`、`lookup`），`DatapackProvider.of` 包装原版上下文；`BiomeSettings` 不再用 `PlacementUtils.register` |
| R3 | `RegistryInfo.fromRegistryLookup` | 构造器 |
| R4 | `BiomeGenerationSettings` 构造器可见性 | `PlainBuilder` |
| R5 | `HolderSet.empty()` | `HolderSet.direct()` |
| R6 | `addAll` | 逐个 `add` |
| R7 | 移植 JSON 格式随版本变 | 接缝 `biome.PortFormat.adapt`，主线原样返回 |
| R8 | `RegionBorders` 把事件注解和边界逻辑写在一起 | 逻辑留 `RegionBorders`（两端相同），事件转交 `RegionBorderEvents` |
| R9 | `SpiritSpring` 混着加载器的注册、`DeferredHolder` 和生物群系修饰 | 加载器部分移到 `SpiritSpringRegistration`，`SpiritSpring` 两端相同 |
| R10 | 主线 `SpiritSpring` 类注释一行超 120 字符；`Replicas` 注释点名 NeoForge | 改行；改成中性的 "the loader" |

结果：主线 25 个类中 13 个与 Forge 端逐字相同；其余 12 个是接缝或胶水：`GuWorld`、`GuzhenrenBlocks`、`DatapackBootstrap`、`PortFormat`、`RegionBorderEvents`、`SpiritSpringRegistration`、`TreasureYellowHeavenEffects`、datagen 5 个（只差加载器包名与修饰键）。`src/ports` 逐字相同。测试只差 `@GameTestHolder` 导入、`skyAccess` 与 `fillFromNoise` 签名。
