<!--
SPDX-FileCopyrightText: 2026 SecretAgent12
SPDX-License-Identifier: LGPL-3.0-only
Additional terms under GPL-3.0 section 7 apply: see NOTICE.md
-->

# Compatibility

All of these are optional. NTM: DE runs fine on its own, and every integration switches on by itself when
the other mod is installed.

| Mod | Status | What you get |
|---|:---:|---|
| **Create: Aeronautics / Sable** | ✅ | Machines, cables, pipes and multiblocks work on airships and other physics builds |
| **Create** | ✅ | Plays nicely: NTM bundles the same Flywheel and Ponder that Create 6 uses. From 1.0.0-beta.5: Engineer's Goggles go into an NTM helmet (helmet or extra slot of the armor modification table) and work from there |
| **JEI** | ✅ | Every NTM recipe type |
| **Jade** | ✅ | Energy and fluid info; looking at any part of a multiblock shows its core |
| **The One Probe** | ✅ | Same as Jade |
| **WTHIT** | ✅ | Same as Jade (from 1.0.0-beta.4) |
| **KubeJS** | ✅ | From 1.0.0-beta.5. Typed machine recipes, replaceInput/replaceOutput, filters. See [KubeJS and datapacks](kubejs.md) |
| **CC: Tweaked** | ✅ | Peripherals for reactors, turbines, launch pads, storage and much more. See [ComputerCraft](computercraft/README.md) |
| **Curios** | ✅ | Radioactive or hazardous items in Curios slots affect you like items in your inventory |
| **Sophisticated Storage / Backpacks** | ✅ | Radiation of items stored inside counts |
| **Traveler's Backpack** | ✅ | Radiation of carried items counts (fluid tanks not yet) |
| **Sodium** | ✅ | Tested with 0.8.x |
| **Lithium** | ✅ | |
| **ImmediatelyFast, Gnetum** | ✅ | From 1.0.0-beta.4. Older versions: machine GUIs could turn dark or black |
| **ScalableLux** | ✅ | Recommended (0.3.x): relights nuke craters much faster. Not together with Create: Aeronautics, see below |
| **Iris / shaders** | ✅ | From 1.0.0-beta.10. Iris 1.8.14 or newer with Sodium 0.8. Install Colorwheel as well, see [Shaders](#shaders) |
| **REI, Compact Storage** | ❌ | Not planned for now |
| **Trinkets** | ❌ | No NeoForge 1.21.1 version exists |

## Shaders

NTM: DE works with **Iris** shader packs from 1.0.0-beta.10. With Sodium 0.8 you need **Iris 1.8.14** or newer:
older Iris builds are made for Sodium 0.6 and crash when you join a world.

**Install Colorwheel too.** NTM draws its machines with Flywheel, the same engine Create uses. Flywheel switches
itself off while a shader pack is active, so without help every machine falls back to the slow way of drawing.
Everything still shows up, but on a big base it costs a lot of FPS, and shaders draw the world a second time for
shadows. **[Colorwheel](https://modrinth.com/mod/colorwheel)** ([CurseForge](https://www.curseforge.com/minecraft/mc-mods/colorwheel)) keeps
Flywheel running under shaders. It isn't bundled with NTM because it needs Iris to run: install it yourself, next
to Iris.

- Packs with Colorwheel support (Complementary Reimagined and Unbound, Euphoria Patches, Solas) work right away.
  When it's working, the chat says *The Flywheel backend is now 'colorwheel:indirect'*.
- Other packs (BSL and the like): Colorwheel warns that the pack isn't compatible, and Flywheel stays off. Turn on
  its fallback mode with `/colorwheel enableFallbackMode on`. Tested with BSL v10.1.8.
- With some packs Colorwheel reports *broken shaders* once. For NTM that's harmless: it only means glowing effects
  don't cast shadows, which they shouldn't anyway. `/colorwheel alertBrokenPack off` hides the message.

With a shader pack on, a few NTM effects lose small touches: they no longer fade in and out, and fog applies to
them. Bullet tracers are drawn as simple boxes, the way the original 1.7.10 mod drew them.

## Tips

**Big explosions.** A nuke rebuilds lighting for a lot of chunks at once. With **ScalableLux** installed,
that's a lot faster, and the lag spike after a large blast is much shorter. One catch: Sable (the physics
behind Create: Aeronautics) refuses to start next to ScalableLux, so it's one or the other. If you fly
airships, skip ScalableLux: NTM works fine without it, the relight just takes longer.

**Airships.** When you assemble a ship with a machine on board, make sure the *whole* multiblock is part of
the build (Super Glue helps). If only one block of a multiblock gets picked up, the machine breaks apart.

**Performance flag.** Adding the JVM argument `--add-modules=jdk.incubator.vector` turns on faster SIMD code
for radiation and noise. Without it the mod falls back to plain Java, which works just as well, only slower
on huge setups.

## Found a problem?

If NTM: DE misbehaves together with another mod, [open an issue](https://github.com/SecretAgent12/NTM-Decentralized-Edition/issues)
with both mod versions and your `latest.log`.
