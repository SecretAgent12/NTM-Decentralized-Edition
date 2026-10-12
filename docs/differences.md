# Differences from NTM: NEXT

The goal is simple: the same mod, just on 1.21.1. Every machine, item, recipe and mechanic of NTM: NEXT is
here. What follows is the short list of places where things are not *exactly* the same.

## Added

- **Create: Aeronautics / Sable support.** Machines keep working on physics-based sub-levels: GUIs open and
  sync, energy and fluid networks survive assembly and disassembly, and multiblocks move as a whole.
- **Integrations with 1.21.1 mods.** Lithium, ScalableLux and Traveler's Backpack are on top of what NEXT
  supports. See [Compatibility](compatibility.md).
- **Ponder scenes** *(work in progress)*. Animated in-game explanations, the way Create does it. The first
  ones cover the firebox with the boiler and with the steel furnace.
- **Complete Russian and Ukrainian translations.**

## Fixed

Bugs I ran into that are present in NEXT itself:

- Damaged turrets (found in ruins) drained their energy buffer below zero. Worlds saved with a negative buffer
  get fixed on load.
- The *static sandwich* and the *damaged holotape* lost their animation.
- Single-piece structures (like the factory) could spawn half under water near rivers and swamps.
- The root advancement had no background, and a few advancements were granted for the wrong equipment.
- Spawn eggs and a few tinted items had broken colours, and several item icons were cropped or oversized.
- Gibs, skeleton bits and ashes froze in mid-air after touching the ground once.
- The steel furnace's ore bonus now also recognizes raw ores and ores from other mods through the common
  `c:` tags.

## Not there yet

- **Sky and fog after a big impact.** The darkening of the world after a large impact event isn't shown yet.
- **Traveler's Backpack fluid tanks** don't count towards radiation yet. The items inside the backpack do.
- **REI, Trinkets, Compact Storage.** Either there are no NeoForge 1.21.1 versions of these mods, or
  they're not a priority right now. JEI, Jade and The One Probe cover the same ground.

## Things 1.21.1 simply does differently

Small stuff, all marked with `// backport:` comments in the code:

- **Shaders.** NEXT draws its machines under Iris by itself, through its own Flywheel port. On 1.21.1 that's the
  job of a separate mod, [Colorwheel](https://modrinth.com/mod/colorwheel); without it everything is still there, just slower. With a pack on,
  NTM effects lose their fade and no-fog touches, and bullet tracers are drawn as in 1.7.10. See
  [Shaders](compatibility.md#shaders).
- There's no `tntExplodes` game rule in 1.21.1, so TNT always explodes.
- 1.21.1 has one fog range, so NTM's soot fog replaces the normal terrain fog instead of blending with it.
- Bundles have no selected slot in 1.21.1.
- A few eating and drinking sounds that 26.2 plays per entity use the default sounds.
- Jigsaw structures have no separate vertical distance limit in 1.21.1.

None of these change how you play the mod.
