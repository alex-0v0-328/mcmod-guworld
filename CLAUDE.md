# CLAUDE.md — gu-world

The project rules for gu-world, loaded at the start of every session opened here, local or in the cloud. Collaboration rules live in the alex-constitution and i-have-adhd skills, which outrank this file; Alex's chat instructions beat both. Local sessions get those skills from `~/.claude` through the user-level SessionStart hook; cloud sessions get the tracked copies in `.claude/skills/`, injected by the hook in `.claude/settings.json`. Those copies come from `../guzhenren/.claude/`, the source, through `python .claude/hooks/session_start.py --deploy` run there; never edit them here. Guzhenren's rules file `../guzhenren/CLAUDE.md` (Boundaries, Language and style) and its local `reference/` apply here too when checked out beside this one, read on demand; this file only adds what differs, and carries the code conventions itself for cloud sessions.

## What this is

- gu-world (mod id `gu_world`, package `net.alex.guzhenrenworld`, Minecraft 1.21.1, NeoForge 21.1.252) owns every dimension of Guzhenren and its terrain, the overworld terrain both new and vanilla-modified, and the biomes. Guzhenren (`../guzhenren`, mod id `guzhenren`) owns everything else, including travel into these dimensions, their guards and the structures. Ownership record: wiki《待定设计》(Alex, 2026-10-01).
- The full game needs both mods plus the pack; the split exists for design isolation.

## Boundaries

- The dependency exists at run time only: `neoforge.mods.toml` requires `guzhenren` and loads after it, and this project never compiles against Guzhenren. Guzhenren names each dimension by its level key alone (`ModDimensions` there), so renaming a dimension here means renaming it there in the same task.
- The package is `net.alex.guzhenrenworld`, never `net.alex.guzhenren.world`: Guzhenren owns that package, and a package split across two mod jars fails to load.
- Never add Claude as a contributor: no `Co-Authored-By` or generated-with line and no Claude identity in any commit (Alex, 2026-10-01). The remote is `https://github.com/alex-0v0-328/mcmod-guworld` (renamed 2026-10-01; the earlier `gu-world-mcmod` redirects), branch `main`. The local folder is `C:\workspace\Dev\Projects\Minecraft-ModDev\guworld` (renamed from `gu-world` on 2026-10-01, Alex).
- Nothing gets backed up; Git is the only rollback. Temporary files live under `C:\workspace\Dev\Projects\_Temp\guworld\` in a subfolder per task and are deleted before the task closes. `.idea/`, `run/`, `src/test/` and `.claude/settings.local.json` sit outside Git; tests, when there are any, stay local-only as in Guzhenren.
- The file system is case-insensitive: `guzhenrenworld.java` and `GuzhenrenWorld.java` are one file, so a case-only rename takes two moves.

## Build, run and check

- `.\gradlew.bat build` compiles and packs `build/libs/gu_world-<version>.jar`; GitHub Actions does the same and nothing more.
- `.\gradlew.bat runData` regenerates the committed `src/generated/resources`. It and every other run load Guzhenren at run time from `../guzhenren` (`-PguzhenrenDir` overrides): its built jar, Epic Fight and GeckoLib from its `run/mods`, and Curios. Build Guzhenren first. After a provider change, run it and review the diff; CI cannot check this drift.
- Both mods run together in Guzhenren: its `python tools/check.py` builds this project first, then runs the GameTests, the server smoke (which loads every dimension defined here) and the client. Visual and feel acceptance in `runClient` stays Alex's.
- Guzhenren's tools drive this repository too, from its folder: `python ../guzhenren/tools/check.py --project guworld` runs `build`, then the `runData` drift check; `python ../guzhenren/tools/ship.py <plan> --project guworld` commits and pushes through the same mirror and contributor gate (plan at `C:\workspace\Dev\Projects\_Temp\guworld\ship-plan.json`, log under `_Temp\guworld\logs\`). The flow, commit-message picking included, is Guzhenren's `commit-push` skill, section Sibling repositories.
- A cloud session has no Guzhenren checkout beside it: it can run `./gradlew build` but neither `runData` nor the tools, and leaves the drift check and shipping to a local session.

## Map

| Path                                        | What it is                                                                                                |
|---------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `src/main/java/net/alex/guzhenrenworld/`    | `GuWorld` (entry, `MOD_ID`), `GuWorldClient`, `registry/WorldDimensions`, `datagen/`, `client/dimension/` |
| `src/generated/resources/`                  | Committed datagen output                                                                                  |
| `.claude/`                                  | Cloud-session hook and the always-on skills, copies synced from Guzhenren; `settings.local.json` local    |
| `../guzhenren/`                             | Guzhenren, where both mods run and are checked together                                                   |
| `C:\workspace\Obsidian\guzhenren-mod-wiki\` | The shared wiki; dimensions under 开发向/维度                                                             |

## Language and style

- Chat in Chinese (zh-CN); code, comments and commit messages in American English; this file in English. Terms come from the language providers and the wiki《原著词汇 与命名》, never invented bilingual pairs.
- The Guzhenren code conventions apply. `.editorconfig`, a copy of Guzhenren's, keeps the automatable part; the rest, condensed from Guzhenren's reference《写作与命名》:
  - Comments live only in the top-level class Javadoc: no `//` in method bodies and no Javadoc on fields, methods, constructors or nested types; the class Javadoc points at a member with `{@link #member}`. Only `//region` labels and TODO anchors stay in place, and a `//region` label must match its code. Explain non-obvious reasons, contracts and limits; never restate the code. The Javadoc ends with `@author Alex`, `@version 1.0.0`, optional `@see`, `@since 1.0.0`.
  - Blank lines: one after a type's opening brace, none before its closing brace, none at the top of a method or block body; exactly one between members, placed above Javadoc and annotations; consecutive fields form one block with no blank lines; one between the top-level class Javadoc and the declaration; one before `//region` and after `//endregion`, none inside them; at most one between logical steps inside a method.
  - Braces: an empty body is `{}` on one line (`private X() {}`); a one-line brace pair with content keeps a space inside (`{ return x; }`, `new int[] { 1, 2 }`).
  - Imports form one block in plain ASCII order (`ModContainer` before `common.Mod`), static imports in their own block first, no wildcards. Constants are `UPPER_SNAKE` (`MOD_ID`). No hand-aligned argument columns; lines stay within 120 characters.
- New code matches its neighbors' comment density, naming and idiom. Markdown tables stay aligned: after touching one, `python ../guzhenren/tools/md_tables.py <file>` must report nothing.
