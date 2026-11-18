# Dead's Mod Requiem v3 — Forge 1.16.5

This mod is an **experimental patch of the supplied Dead's Mod JAR** (`horrormod`) built as a universal client/server binary. Back up existing saves before testing.

## New features

- **Right-click a special compass** to select between **Key Structure** and **Exit Structure**. Two independent types of targets and textures; state saved in the item NBT.
- The underlying vanilla compass retains its angle/lodestone behavior. Normal vanilla compasses keep their models. Special compasses use custom NBT model data 314159 (key) or 314160 (exit), with 32 directional pixel-art sprites in each mode.
- **White through-wall outline** of the currently selected NBT dungeon once within 128 blocks, using mesh edges calculated from the original key/exit structures. Rendering is client-only and targets the server-provided actual origin, never a guessed location.
- **Safe entry into the dead dimension:** attempt a natural, free-standing landing location nearby, otherwise build a small 5×5 obsidian safety platform and clear headroom. `/Secreat` keeps its existing separate pumpkin-platform logic.
- Compass auto-given once in the dead dimension, with existing v2 structure-position persistence.

## Build

The original input binary is not checked into this public repository because it contains third-party recorded audio. Unless you have permission to redistribute the audio, change the repository to private before attaching the original input to a release.

1. Open **Releases → Draft new release**, tag `original-input-v1`.
2. Attach the original client jar under the exact name `original.jar` (SHA256 `a933c66ce2fd8b34d6287a96144f07fc0718c76c899271f6aedb542a044389fa`).
3. Publish the release; the `.github/workflows/build.yml` workflow will run. Alternatively run it from the Actions tab.
4. Download the `DeadsMod-Requiem-v3-build` artifact after CI succeeds.

The workflow builds the JAR and a source bundle with **decompiled original classes** using CFR, plus all patch source code, asset generation scripts, tests, and original resources. It compiles our mod hooks against minimal **test-only API stubs**, which are **not** packaged in the generated JAR.

## Test scope / limitations

Java 8 compilation + mocked server/event/structure NBT tests and JAR ZIP integrity have been checked locally. **No full Minecraft Forge client or dedicated-server session has been run.** The outline rendering and teleport compatibility on real modpacks still require testing. Do not replace your only save with an experimental build.

Already-generated chunks from previous mod versions are not automatically indexed if they were not recorded. The white outline can only display for structures that were actually placed by the instrumented generator and saved to its index; it cannot scan old terrain without regeneration.

Try ` /give @s minecraft:compass{DeadsModStructureCompass:1b} 1 ` inside the dead dimension, and the server should update its marker/mode/model NBT on the next 80-tick compass refresh. Existing tagged compasses are upgraded in-place.
