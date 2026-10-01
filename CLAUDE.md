# CLAUDE.md — gu-world

The project rules for gu-world, loaded at the start of every session. Collaboration rules live in the alex-constitution and i-have-adhd skills, which the user-level SessionStart hook injects and which outrank this file; Alex's chat instructions beat both. Guzhenren's rules file `../guzhenren/CLAUDE.md` (Boundaries, Language and style) and its `reference/` apply here too, read on demand; this file only adds what differs.

## What this is

- gu-world (mod id `gu_world`, package `net.alex.guzhenrenworld`, Minecraft 1.21.1, NeoForge 21.1.252) owns every dimension of Guzhenren and its terrain, the overworld terrain both new and vanilla-modified, and the biomes. Guzhenren (`../guzhenren`, mod id `guzhenren`) owns everything else, including travel into these dimensions, their guards and the structures. Ownership record: wiki《待定设计》(Alex, 2026-10-01).
- The full game needs both mods plus the pack; the split exists for design isolation.

## Boundaries

- The dependency exists at run time only: `neoforge.mods.toml` requires `guzhenren` and loads after it, and this project never compiles against Guzhenren. Guzhenren names each dimension by its level key alone (`ModDimensions` there), so renaming a dimension here means renaming it there in the same task.
- The package is `net.alex.guzhenrenworld`, never `net.alex.guzhenren.world`: Guzhenren owns that package, and a package split across two mod jars fails to load.
- Never add Claude as a contributor: no `Co-Authored-By` or generated-with line and no Claude identity in any commit (Alex, 2026-10-01). The remote is `https://github.com/alex-0v0-328/gu-world-mcmod`, branch `main`.
- Nothing gets backed up; Git is the only rollback. Temporary files live under `C:\workspace\Dev\Projects\_Temp\gu-world\` in a subfolder per task and are deleted before the task closes.
- The file system is case-insensitive: `guzhenrenworld.java` and `GuzhenrenWorld.java` are one file, so a case-only rename takes two moves.

## Build, run and check

- `.\gradlew.bat build` compiles and packs `build/libs/gu_world-<version>.jar`; GitHub Actions does the same and nothing more.
- `.\gradlew.bat runData` regenerates the committed `src/generated/resources`. It and every other run load Guzhenren at run time from `../guzhenren` (`-PguzhenrenDir` overrides): its built jar, Epic Fight and GeckoLib from its `run/mods`, and Curios. Build Guzhenren first. After a provider change, run it and review the diff; CI cannot check this drift.
- Both mods run together in Guzhenren: its `python tools/check.py` builds this project first, then runs the GameTests, the server smoke (which loads every dimension defined here) and the client. Visual and feel acceptance in `runClient` stays Alex's.
- Guzhenren's tools drive this repository too, from its folder: `python ../guzhenren/tools/check.py --project gu-world` runs `build`, then the `runData` drift check; `python ../guzhenren/tools/ship.py <plan> --project gu-world` commits and pushes through the same mirror and contributor gate (plan at `C:\workspace\Dev\Projects\_Temp\gu-world\ship-plan.json`, log under `_Temp\gu-world\logs\`). The flow, commit-message picking included, is Guzhenren's `commit-push` skill, section gu-world.

## Map

| Path                                        | What it is                                                                                                              |
|---------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| `src/main/java/net/alex/guzhenrenworld/`    | `GuWorld` (entry, `MOD_ID`), `GuWorldClient`, `registry/WorldDimensions`, `datagen/`, `client/dimension/` |
| `src/generated/resources/`                  | Committed datagen output                                                                                                |
| `../guzhenren/`                             | Guzhenren, where both mods run and are checked together                                                                 |
| `C:\workspace\Obsidian\guzhenren-mod-wiki\` | The shared wiki; dimensions under 开发向/维度                                                                           |
