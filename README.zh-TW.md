# 蠱界 Gu World 模組

[简体中文](README.md) | 繁體中文 | [English](README.en.md)

[蠱真人 Guzhenren](https://github.com/alex-0v0-328/mcmod-guzhenren) 的配套世界模組，負責全部維度、地形生成、水系、生態域與自然結構。必須與蠱真人主模組同時安裝。

> 目前處於開發階段，具體玩法與數值尚未定型，本文僅說明工程結構、執行環境與依賴關係。

本文譯自[簡體中文版](README.md)；各版本不一致時，以簡體中文版為準。

## 環境要求

| 項目 | 版本要求 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.265 及以上 |
| Java | JDK 21 |
| 模組版本 | 1.0.0 |

## 模組分工

本專案與主模組協同工作，透過註冊鍵與跨模組 ID 解耦，無直接編譯期依賴：

| 模組 | 儲存庫 | 職責範圍 |
|---|---|---|
| [蠱真人 Guzhenren](https://github.com/alex-0v0-328/mcmod-guzhenren) | `mcmod-guzhenren` | 修行體系、蠱蟲與煉蠱、方塊與流體定義、實體、維度旅行與守衛 |
| 蠱界 Gu World | `mcmod-guworld`（本儲存庫） | 全部維度、四域地形生成、水系、生態域、元泉世界生成與自然結構 |

## 前置依賴

Gu World 在載入期硬依賴主模組 `guzhenren`，執行時需一併安裝主模組的全部硬依賴：

| 模組 | 類型 | 版本範圍 | 說明 |
|---|---|---|---|
| [蠱真人 Guzhenren](https://github.com/alex-0v0-328/mcmod-guzhenren) | 硬依賴 | `[1.0.0,)` | 主模組，提供核心機制與方塊定義 |
| [Epic Fight](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) | 主模組硬依賴 | `[21.17.3, 21.18)` | 戰鬥與動作系統 |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | 主模組硬依賴 | `[4.9, 4.10)` | 實體骨骼動畫 |
| [Photon](https://www.curseforge.com/minecraft/mc-mods/photon) | 主模組硬依賴 | `[2.2.11, 2.3)` | 粒子特效執行庫（用戶端與伺服端均需安裝） |
| [LDLib2](https://www.curseforge.com/minecraft/mc-mods/ldlib) | 主模組硬依賴 | `[2.2.46, 2.3)` | Photon 基礎庫（用戶端與伺服端均需安裝） |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | 可選聯動 | `[19, 20)` | 物品與配方查看 |

## 安裝與建置

### 遊戲安裝

1. 安裝 Minecraft 1.21.1 與 NeoForge 21.1.265 及以上版本。
2. 安裝 `guzhenren` 及其全部硬依賴模組。
3. 將 `guworld` 放入同一 `mods/` 目錄。

### 原始碼建置

本儲存庫預設從同級目錄 `../mcmod-guzhenren`（可透過 `-PguzhenrenDir` 指定）載入主模組建置產物與執行期依賴。開發執行與測試前需先建置主模組：

```powershell
../mcmod-guzhenren/gradlew -p ../mcmod-guzhenren assemble
./gradlew build
./gradlew runData
./gradlew runGameTestServer
```
