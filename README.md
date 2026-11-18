# Dead's Mod — Requiem (Forge 1.16.5)

**Status: experimental; 1.16.5 Forge client/server launches have not been performed.**

This repo contains a bytecode patcher and Java 8 runtime hooks for the user-provided
**horrormod 1.0.0** JAR. The client and server originals have identical ZIP entries,
so the same mod base is patched for both sides.

## Compass bug fix (v2)

- Fixes the ItemStack constructor: uses 1.16.5's \`IItemProvider\`, not \`Item\`.
- Issues one marked vanilla compass when entering \`horrormod:death_dim\`
  or when the player is already in it (server-side tick fallback).
- Does **not** grant endless duplicates after the compass is dropped;
  also finds the marked compass in the offhand.
- Captures the **actual BlockPos** immediately after the original key/exit
  NBT structure template placement call, rather than guessing where a
  structure should appear.
- Stores located placements in the dimension's \`WorldSavedData\`.
- The compass only points at coordinates that were recorded after real
  placement. If the registry is empty, it displays **"Компас мёртвых (цель не найдена)"**
  instead of giving a fake target.
- Keeps older worldgen-spacing changes and original mod mechanics.

**Important save compatibility limitation:** The new registry cannot automatically
discover already-generated key/exit dungeons in older chunks. Explore **new terrain**
after upgrading to register newly spawned structures. The compass will not have
a target until one actually generates. Structure registration is based on a
template-placement call, which does not independently validate placed blocks.

## Build via GitHub Actions

The original 60 MB JAR contains third-party sound files; it is **not published
in source control**. For legal/privacy reasons, switch this repository to
Private unless you have redistribution permission.

1. Create a GitHub Release tagged \`original-input-v1\`.
2. Upload **the original** \`Dead's Mod (Client).jar\` as asset **\`original.jar\`**
   (not a patched version and not the Server one).
3. Publish the release. The workflow \`.github/workflows/build.yml\` then builds.
4. Open **Actions → Build Dead's Mod Requiem → Artifacts**
   and download \`DeadsMod-Requiem-build\`.

The workflow checks the original SHA256, compiles Java 8 hooks against
*compile-only Minecraft API stubs* (not packaged in JAR), executes mock smoke
tests, patches the original JAR using ASM, and decompiles original classes into
a source archive using CFR.

For local Windows builds, install JDK 21, put the same \`original.jar\` in
the repository root, and run \`build_windows.bat\`. Mod runtime targets Java 8.

### Tests and limitations

Tests pass in a simulated Java 8 environment: automatic grant, absence of
phantom destination, actual template coordinates, persisted NBT encoding,
no duplicate on tick / drop, plus deterministic worldgen placements.

Minecraft Forge client and dedicated-server end-to-end tests are **not**
performed by these smoke tests. Back up an existing world before upgrading.
Use only one JAR containing \`modId=horrormod\` in each client/server \`mods\` folder.
