# Compatibility

All of these are optional. NTM: DE runs fine on its own, and every integration switches on by itself when
the other mod is installed.

| Mod | Status | What you get |
|---|:---:|---|
| **Create: Aeronautics / Sable** | ✅ | Machines, cables, pipes and multiblocks work on airships and other physics builds |
| **Create** | ✅ | Plays nicely: NTM bundles the same Flywheel and Ponder that Create 6 uses |
| **JEI** | ✅ | Every NTM recipe type |
| **Jade** | ✅ | Energy and fluid info; looking at any part of a multiblock shows its core |
| **The One Probe** | ✅ | Same as Jade |
| **WTHIT** | ✅ | Same as Jade (from 1.0.0-beta.4) |
| **CC: Tweaked** | ✅ | Peripherals for reactors, turbines, launch pads, storage and much more. See [ComputerCraft](computercraft/README.md) |
| **Curios** | ✅ | Radioactive or hazardous items in Curios slots affect you like items in your inventory |
| **Sophisticated Storage / Backpacks** | ✅ | Radiation of items stored inside counts |
| **Traveler's Backpack** | ✅ | Radiation of carried items counts (fluid tanks not yet) |
| **Sodium** | ✅ | Tested with 0.8.x |
| **Lithium** | ✅ | |
| **ImmediatelyFast, Gnetum** | ✅ | From 1.0.0-beta.4. Older versions: machine GUIs could turn dark or black |
| **ScalableLux** | ✅ | Recommended (0.3.x): relights nuke craters much faster. Not together with Create: Aeronautics, see below |
| **Iris / shaders** | ❌ | Planned |
| **REI, Compact Storage** | ❌ | Not planned for now |
| **Trinkets** | ❌ | No NeoForge 1.21.1 version exists |

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
