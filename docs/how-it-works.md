# How the backport works

*or: how to fit a 26.2 mod into 1.21.1 and not lose your mind (much)*

So. NTM: NEXT is written for Minecraft 26.2. Between 1.21.1 and 26.2 Mojang rewrote... ummm... a lot:
how block entities save their data, how items are described, how things get rendered, how models are loaded.
You can't just change a version number and hit build. I tried. The compiler did not like it. The compiler
had *opinions*. Thousands of them.

Then there's the other problem: NEXT is big and alive. Thousands of files, new commits every week. Port it
by hand, file by file, and the port is outdated before you're done. Then the next update lands and you get to
do it all over again. Forever. No thanks.

So I don't really *edit* NEXT. I **build** the 1.21.1 version out of it, like a very stubborn compiler
that speaks two dialects of Minecraft.

## Three layers

**1. Teaching 1.21.1 the new words.**
A lot of NEXT code talks to APIs that simply don't exist in 1.21.1. I didn't rewrite every place that uses
them. I just... made them exist. The 26.x way of saving block entity data, item properties, pieces of the
rendering and model system, the new transfer API for energy and fluids: all recreated on top of 1.21.1.
NEXT's code calls them exactly like on 26.2 and has no idea it's being lied to. Nobody tell it.
Most of this lives in `com.hbm.backport` and `com.hbm.lib.neotransfer`.

**2. Mechanical translation.**
Some differences are just renames or slightly different method calls. Boring stuff, so a script does it the
same way every time. Resources get the same treatment: recipes, loot tables, world generation, block states
and item models are converted from the 26.x JSON formats into something 1.21.1 will actually load.

**3. Hand-written parts.**
Some things no script can do for you. The rendering bridge, Create: Aeronautics / Sable support,
integrations with 1.21.1 mods and bug fixes are written by hand, like in the good old days. With tears.

## Keeping up with NEXT

When NEXT gets new commits, I run the whole conversion again on the new version and merge the result with
my tree, git-style: what changed upstream vs what I changed myself. The hand-written parts survive, the
new upstream code arrives already converted, and a human only has to look at places where both sides touched
the same lines. Usually that's a handful. Usually.

## Reading the code

- `// backport:` means "yes, this is different from NEXT on purpose". 1.21.1 works differently here, and
  the comment says what and why. If it looks weird, there's probably a reason. If it still looks weird after
  reading the reason... open an issue, let's talk.
- `// backport-fix:` is a bug I fixed that NEXT still has (or had, at the time of writing).
- `com.hbm.backport.*` is the bridge between the 26.x code and 1.21.1. Here be dragons. Friendly ones,
  mostly.
- `com.hbm.lib.crankshaft` is the Flywheel-based instanced rendering that NEXT uses (MIT, see `NOTICE.md`).
  It's why a hundred turbines don't turn your GPU into a space heater.

## Contributing

The conversion tooling isn't in this repository. It's a pile of scripts that only makes sense next to a NEXT
checkout, and it looks about as pretty as you'd expect. Pull requests against `src/` are very welcome anyway:
I carry them forward through every upstream update, by hand if I have to.

Found a bug? Open an issue with your `latest.log`, the steps to reproduce and your mod list. And please report
it **here**, not to NEXT or HbmMods. They have enough on their plate, and the bug is probably mine anyway.
