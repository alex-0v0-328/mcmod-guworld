# gu-world

[简体中文](README.md) | English

The companion mod of [Guzhenren](https://github.com/alex-0v0-328/mcmod-guzhenren): every dimension, terrain and biome lives here. It must be installed together with Guzhenren.

> In development. Gameplay is not final, so this file lists no content.

This is a translation of [README.md](README.md); where the two differ, the Chinese version prevails.

## Requirements

|                  |                                        |
|------------------|----------------------------------------|
| Minecraft        | `1.21.1`                               |
| NeoForge         | `21.1.x`                               |
| Java             | `21`                                   |
| mod id / package | `gu_world` · `net.alex.guzhenrenworld` |
| Required         | Guzhenren (`guzhenren`)                |

Exact versions live in `gradle.properties` and `build.gradle`.

## Build and run

This project compiles without Guzhenren. `runData` and the dev client load it at run time from the sibling `../guzhenren`: its built jar, plus the Epic Fight and GeckoLib jars in its `run/mods/`, so build it there first. Both mods are tested together in the Guzhenren project.

```text
gradlew.bat build              # compile, jar
gradlew.bat runData            # regenerate data
```

Use `./gradlew` on other systems. `runData` writes `src/generated/resources`, which is a source set: regenerate and commit it after any provider change.

## License

All rights reserved. `LICENSE.txt` is the MIT license inherited from the NeoForge MDK template and **does not cover the mod's code**.
