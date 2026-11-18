# Dead's Mod Requiem v3.2 — Minecraft Forge 1.16.5

**Experimental client/server JAR** based on user-provided original Dead's Mod. Keep only one `horrormod` JAR installed on each side, and back up your world before updating.

## Fixes in v3.2

**Dungeons on bedrock:** The Dead Dimension has a 128-block Nether-like noise generator and an upper bedrock ceiling. The v3.1 `OCEAN_FLOOR_WG - 4` height strategy incorrectly placed key/exit dungeons on that roof. v3.2 confines **new** key and exit structures to Y=20..112 (including their NBT heights/offsets), below the roof and above the lower bedrock. Existing structures remain unchanged; test in *new chunks*.

**White silhouette not visible:** Replaced direct OpenGL immediate-mode lines with Forge 1.16.5 RenderWorldLastEvent + MatrixStack + Tessellator POSITION_COLOR buffer. The renderer disables depth test so the outline can be drawn through terrain, restores render state, and submits the original NBT-derived contour only when a marked compass is held and its actual target is within 128 blocks. Also fixed a potentially ambiguous reflective Forge event-bus registration of static handlers.

**Safe spawning:** natural safe landing and obsidian fallback are now constrained below the dimension's upper roof (separate `/Secreat` platform retained).

Preserved: right-click **key ↔ exit** modes, two compass sprite sets, automatic compass issue, persisted actual structure records, worldgen spacing 20x20 key / 24x24 exit chunks.

## Files and building

- `src/main/java/net/mcreator/horrormod/unified/`: patch source (WorldgenPolicy, DeadHooks, CompassEvents, ClientOutline, StructureRecords).
- `tools/PatchJar.java`: ASM patcher against the **original** Forge 1.16.5 mod JAR.
- `tools/structure_mesh.py`, `tools/generate_compass_assets.py`: generate outlines and sprites from NBT/resource definitions.
- `tests/`: mock-only Java 8 smoke tests, not real Minecraft test runs.
- `.github/workflows/build.yml`: reconstructs original Java with CFR, compiles, patches and packages JAR + sources.

**GitHub Actions:** To build, create Release `original-input-v1` with original client JAR renamed to `original.jar`. This public repository does not upload the original binary, which contains third-party audio. Prefer changing repository visibility to Private before distributing it.

## Test status and limitations

Passed locally: JDK 21 compiler targeting Java 8; four mock tests for worldgen position, compass state, event subscription and outline vertex submission; ASM patch; archive integrity. **Not yet verified in a live Forge 1.16.5 client or dedicated server** or with shaders. If the outline remains missing, send `.minecraft/logs/latest.log` containing `[Requiem]`, and test holding the marked compass with a *newly registered* dungeon in range. Old unindexed structures do not gain contour targets automatically.
