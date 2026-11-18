# Dead's Mod Requiem v3.1 (Forge 1.16.5)

Experimental compatibility update: more discoverable key/exit dungeons in the Dead Dimension.

## Dungeon frequency fix
- Key: 96 x 96 chunk candidate region in v3 -> **20 x 20** in v3.1.
- Exit: 112 x 112 in v3 -> **24 x 24** in v3.1.
- Both dungeon types can now generate in either `horrormod:death` or `horrormod:dead_woods_biome` (the actual dead-dimension biomes).
- The original generator rejected almost all candidates by asking for a specific custom block at a random altitude. The patched generator uses the already-verified Dead Dimension and biome checks instead and chooses height near the terrain surface.
- Overworld and other structure spacing unchanged.

**IMPORTANT:** Already-generated chunks do NOT regenerate. If your save's Dead Dimension has been explored, visit *new* chunks after installing the update. The compass only knows about structures whose template placement was recorded by the patch; it will not falsely target a calculated but ungenerated dungeon. Candidate grids do not guarantee one structure per region, because the biome can reject a candidate.

## Compass and gameplay
Preserves v3's right-click key/exit selection, two compass item models, white contour outline, and safe Dead Dimension entrance. The white contour appears only after the destination has actually been recorded.

## Build (GitHub Actions)
Original source jar is not committed, since it includes third-party audio. To trigger Actions, create release `original-input-v1` with asset named `original.jar` containing the **original** Dead's Mod Client JAR. Prefer making the repo private before uploading such a binary. This workflow checks its SHA256, generates the derived resources and emits the JAR plus a source ZIP. See `.github/workflows/build.yml`.

## Testing and risk
Java 8 compatibility tests (using test-only Minecraft stubs), ASM bytecode inspection, and ZIP integrity have been checked locally. A full Minecraft Forge client and dedicated-server startup test **has not** been performed; make a backup of any existing world before installing.
