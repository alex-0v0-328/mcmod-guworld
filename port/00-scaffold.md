# 00 脚手架

目标：空的 `guworld` 在 Forge 1.20.1-47.4.10 上随 guzhenren Forge 端启动；后续模块往里搬代码。

## 模块划分（整个移植）

主线 22 个类、约 2.2k 行，外加 `src/ports`（Terralith / Tectonic 的 JSON）和 `src/generated`。

| 模块 | 内容 |
|---|---|
| 0 | 构建、`mods.toml`、入口 `GuWorld`、`GuzhenrenBlocks` |
| 1 | 宝黄天维度、客户端天空、`RegionBorders`、datagen 骨架（`DataGenerators`、`DatapackProvider`、语言表） |
| 2 | 共用生物群系（`biome/`）、南疆（`dimension/southernborder/`）、移植的 Terralith / Tectonic JSON、标签 |
| 3 | 元泉（`feature/`）、生物群系修饰；L2 JUnit 与 L3 GameTest |

每个模块后做可移植性回顾，主线与 Forge 端一起改。

## 问题

| # | 问题 |
|---|---|
| P1 | 主线 ModDevGradle + Java 21；Forge 端 ForgeGradle 6 + Parchment + Java 17（同 guzhenren 端） |
| P2 | guzhenren 是运行时依赖；Forge 端它的 jar 是 SRG 名，要 `fg.deobf` 重映射后才能在开发环境加载 |
| P3 | `DeferredHolder.create` → `RegistryObject.create` |

## 规格

| 项 | 取值 |
|---|---|
| 构建 | 照 `mcmod-guzhenren-forge`：ForgeGradle `[6.0,6.2)`、Parchment `2023.09.03-1.20.1`、工具链 17，不用 Mixin |
| guzhenren | `-PguzhenrenDir`（默认 `../mcmod-guzhenren-forge`）的 `build/libs/guzhenren-*.jar` 经 `flatDir` + `fg.deobf("blank:<名>:0")` 只进运行时；Epic Fight、GeckoLib 从它的 `pack/mods` 同样取；Curios `5.14.1+1.20.1` 走 maven |
| 本地检查用的 guzhenren | `git archive` 导出 guzhenren Forge 端已提交的 HEAD 到 `Temps/guworld/guzhenren-forge-head`（`pack` 链接回原处）再 `assemble`，不碰另一个会话的工作区 |
| `mods.toml` | forge `[47,)`、minecraft `[1.20.1,1.21)`、guzhenren `[1.0.0,)` 必需且 `AFTER` |
| run | `client`、`server`（`-PserverGameDir`）、`gameTestServer`（`run-gametest`）、`data`（`run-data`，`guworld.ports` 系统属性） |
| 测试 | L2 `test` 读 `src/test/modded`（ForgeGradle 没有 MDG 单元测试运行时，按普通 JUnit 跑），L3 `gametest` 读 `src/test/game` |
| Git | 本地仓库 `main`，`.gitignore` 照主线，加 `/port/`、`/tools/` |
| 冒烟 | `tools/smoke.py`（本地）：临时目录起 `runServer`，到 `Done` 后执行命令、`stop` |

## 验收结果

| 项 | 结果 |
|---|---|
| `build` | 通过 |
| `runServer` | guzhenren（HEAD `9c6de4b`）+ guworld 加载，`Done`，`stop` 后三个原版维度存盘 |
