# Gu World Mod

[简体中文](README.md) | [繁體中文](README.zh-TW.md) | English

The companion world mod for [Guzhenren](https://github.com/alex-0v0-328/mcmod-guzhenren), providing all dimensions, terrain generation, waterways, biomes, and natural structures. Must be installed together with Guzhenren.

> Currently in development. Gameplay mechanics and balance are not yet final; this document covers project structure, environment requirements, and dependencies only.

This is a translation of [README.md](README.md); where the versions differ, the Simplified Chinese one prevails.

## Requirements

| Component | Requirement |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.265 or newer |
| Java | JDK 21 |
| Mod Version | 1.0.0 |

## Module Split

Gu World works alongside Guzhenren and decouples cross-mod references via registry keys and IDs without a compile-time dependency:

| Mod | Repository | Scope |
|---|---|---|
| [Guzhenren](https://github.com/alex-0v0-328/mcmod-guzhenren) | `mcmod-guzhenren` | Cultivation systems, Gu worms and refinement,<br>block and fluid definitions, entities, dimension travel, and guards |
| Gu World | `mcmod-guworld` (this repo) | All dimensions, regional terrain generation, waterways,<br>biomes, primeval spring worldgen, and natural structures |

## Dependencies

Gu World requires `guzhenren` at load time and runs alongside all required dependencies of Guzhenren:

| Mod | Type | Version Range | Purpose |
|---|---|---|---|
| [Guzhenren](https://github.com/alex-0v0-328/mcmod-guzhenren) | Required | `[1.0.0,)` | Main mod providing core mechanics and block definitions |
| [Epic Fight](https://www.curseforge.com/minecraft/mc-mods/epic-fight-mod) | Required by Guzhenren | `[21.17.3, 21.18)` | Combat and animation system |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | Required by Guzhenren | `[4.9, 4.10)` | Entity skeletal animations |
| [Photon](https://www.curseforge.com/minecraft/mc-mods/photon) | Required by Guzhenren | `[2.2.11, 2.3)` | Particle effects runtime (required on both client and server) |
| [LDLib2](https://www.curseforge.com/minecraft/mc-mods/ldlib) | Required by Guzhenren | `[2.2.46, 2.3)` | Core library for Photon (required on both client and server) |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | Optional | `[19, 20)` | Item and recipe viewing |

## Installation & Building

### Player Installation

1. Install Minecraft 1.21.1 with NeoForge 21.1.265 or newer.
2. Install `guzhenren` and all of its required dependency mods.
3. Place `guworld` into the same `mods/` folder.

### Building from Source

By default, runs and GameTests load the built `guzhenren` jar and runtime dependencies from the sibling directory `../mcmod-guzhenren` (configurable via `-PguzhenrenDir`). Build Guzhenren first before running data generation or tests:

```powershell
../mcmod-guzhenren/gradlew -p ../mcmod-guzhenren assemble
./gradlew build
./gradlew runData
./gradlew runGameTestServer
```
