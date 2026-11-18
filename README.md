# 蛊界 Gu World 模组

简体中文 | [繁體中文](README.zh-TW.md) | [English](README.en.md)

[蛊真人 Guzhenren](https://github.com/alex-0v0-328/mcmod-guzhenren) 的配套世界模组，负责全部维度、地形生成、水系、生物群系与自然结构。必须与蛊真人主模组同时安装。

> 当前处于开发阶段，具体玩法与数值尚未定型，本文仅说明工程结构、运行环境与依赖关系。

## 环境要求

| 项目 | 版本要求 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.265 及以上 |
| Java | JDK 21 |
| 模组版本 | 1.0.0 |

## 模组分工

本项目与主模组协同工作，通过注册键与跨模组 ID 解耦，无直接编译期依赖：

| 模组 | 仓库 | 职责范围 |
|---|---|---|
| [蛊真人 Guzhenren](https://github.com/alex-0v0-328/mcmod-guzhenren) | `mcmod-guzhenren` | 修行体系、蛊虫与炼蛊、方块与流体定义、实体、维度旅行与守卫 |
| 蛊界 Gu World | `mcmod-guworld`（本仓库） | 全部维度、四域地形生成、水系、生物群系、元泉世界生成与自然结构 |

## 前置依赖

Gu World 在加载期硬依赖主模组 `guzhenren`，运行时需一并安装主模组的全部硬依赖：

| 模组 | 类型 | 版本范围 | 说明 |
|---|---|---|---|
| [蛊真人 Guzhenren](https://github.com/alex-0v0-328/mcmod-guzhenren) | 硬依赖 | `[1.0.0,)` | 主模组，提供核心机制与方块定义 |
| [Epic Fight](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) | 主模组硬依赖 | `[21.17.3, 21.18)` | 战斗与动作系统 |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | 主模组硬依赖 | `[4.9, 4.10)` | 实体骨骼动画 |
| [Photon](https://www.curseforge.com/minecraft/mc-mods/photon) | 主模组硬依赖 | `[2.2.11, 2.3)` | 粒子特效运行库（客户端与服务端均需安装） |
| [LDLib2](https://www.curseforge.com/minecraft/mc-mods/ldlib) | 主模组硬依赖 | `[2.2.46, 2.3)` | Photon 基础库（客户端与服务端均需安装） |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | 可选联动 | `[19, 20)` | 物品与配方查看 |

## 安装与构建

### 游戏安装

1. 安装 Minecraft 1.21.1 与 NeoForge 21.1.265 及以上版本。
2. 安装 `guzhenren` 及其全部硬依赖模组。
3. 将 `guworld` 放入同一 `mods/` 目录。

### 源码构建

本仓库默认从同级目录 `../mcmod-guzhenren`（可通过 `-PguzhenrenDir` 指定）加载主模组构建产物与运行期依赖。开发运行与测试前需先构建主模组：

```powershell
../mcmod-guzhenren/gradlew -p ../mcmod-guzhenren assemble
./gradlew build
./gradlew runData
./gradlew runGameTestServer
```
