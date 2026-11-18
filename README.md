# Dead’s Mod Requiem v3.3 — Forge 1.16.5

Universal client/server JAR. Mod ID remains `horrormod` to preserve save compatibility.

## Surface-generation fix

The original Dead Dimension is a **128-block Nether-like noise generator with an upper bedrock roof**. Previous v3.1/v3.2 versions used that heightmap or fixed Y, so the two unique dungeons could appear on the bedrock roof or be buried below it.

v3.3 changes only **new terrain generation in the Dead Dimension**:

* `data/horrormod/dimension/death_dim.json`: disables the bedrock roof (`bedrock_roof_position=-10`), sets `noise.top_slide.target=-10` to make an open surface, keeps the same seed, biomes, underground materials, dungeons and other settings.
* `data/horrormod/dimension_type/death_dim.json`: `has_ceiling=false`; remains dark (`has_skylight=false`) and otherwise unchanged.
* Replaces the dungeon generators’ heightmap use with `SurfacePlacement.findOpenSurface`: samples nine positions across the full 16x16 NBT footprint, rejects a solid roof and very steep terrain, chooses the highest natural ground in the footprint, and respects the exit generator’s extra +2 Y offset.
* Before placing the original templates (`keystructurenewrem.nbt`, 16x14x16; `exitdeadssss.nbt`, 16x12x16), fills a native-block foundation under voids and clears obstructions inside the model and a small access margin.
* If no exposed, safe site is available, **do not generate a dungeon at that candidate**, rather than bury it. Dungeon-frequency grid (20 chunks key, 24 chunks exit), stored locations and both compass modes are preserved. The previous cosmetic white outline code is unchanged.

### Save compatibility

**Back up your world first.** Terrain and structures in generated chunks remain unchanged; installing a new JAR does not move old buildings or open up an existing bedrock roof. For a clean test, create a **new world** with v3.3, or re-create just the Dead Dimension after backing up that dimension and its player data. Existing saves may preserve their dimension generator settings, in which case unexplored chunks can still use the old roof; no automatic migration is attempted. Terrain borders between an old and a new generator may look abrupt.

Use only this one `horrormod` JAR on each client/server. Requires Minecraft Forge 1.16.5 and Java 8 at runtime.

## Build

The repo contains only original patch source, not third-party mod audio. GitHub Actions reads **original.jar** from the GitHub Release tagged `original-input-v1` and validates its SHA-256 before building. Keep that release private or secure the rights to the audio before distributing it publicly. `.github/workflows/build.yml` also runs Java 8 mock smoke tests, parses the dimension JSON, generates the compass assets and extracts decompiled Java files via CFR.

`build_windows.bat` builds the modified JAR from the original user-provided JAR using JDK 21. The new JAR targets Java 8.

## Testing

Local: Java 8 compilation, mock tests for worldgen, compasses, safe spawn, renderer and new exposed-surface placement, ASM bytecode verification of both patched generator methods, ZIP integrity, and JSON consistency validation.

**NOT YET TESTED:** Actual launch of Minecraft Forge 1.16.5, dedicated server, or a real world. No guarantee about exact world terrain before that test.
