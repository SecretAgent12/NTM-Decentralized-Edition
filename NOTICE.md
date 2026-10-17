# NTM: Decentralized Edition — backport of ntm-next to Minecraft 1.21.1

This is a **modified version** of [ntm-next](https://github.com/Warfactory-Official/ntm-next)
(NTM 1.0.3, commit `1b8c6c2`, NeoForge module, Minecraft 26.2; earlier DE releases up to 1.0.0-beta.10 were based on commit `3f9a261`, NTM 1.0.0) by movblock, itself a port of
[Hbm's Nuclear Tech Mod](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT) by HbmMods (The Bobcat)
and contributors.

Modified by **SecretAgent12**, 2026: backported to Minecraft 1.21.1 / NeoForge 21.1.
All source files under `src/main/java` and the resources under `src/main/resources` were changed
by the backport (mechanically by conversion tooling, and by hand); `docs/how-it-works.md` explains how,
and deliberate deviations from ntm-next are marked with `// backport:` comments in the code. Files that exist only in the backport carry
`SPDX-FileCopyrightText: 2026 SecretAgent12`; files from ntm-next keep their original copyright lines.

## Licenses

- The mod as a whole: **GNU LGPL-3.0-only** (`LICENSE.LESSER`, which incorporates `LICENSE` — GPL-3.0),
  the license of ntm-next and Hbm's Nuclear Tech Mod.
- `src/main/java/com/hbm/lib/neotransfer/`: derived from NeoForge, **LGPL-2.1-only** (`LICENSES/LGPL-2.1-only.txt`).
- `src/main/java/com/hbm/lib/crankshaft/`: derived from CrankShaft / Flywheel, **MIT** (`LICENSES/MIT.txt`).
- Other third-party code and assets: see `THIRD_PARTY_NOTICES` and `REUSE.toml`.


Whoever receives a built jar of this mod is entitled to its corresponding source code under the
terms of the LGPL.

## Additional terms (section 7 of the GNU GPL v3, as incorporated by the LGPL-3.0)

These additional terms apply to the files in this project whose copyright header names
SecretAgent12 as the only copyright holder (`SPDX-FileCopyrightText: 2026 SecretAgent12`),
including the documentation in `docs/` (the "Material"). Each of these files states that the terms apply.
They do not apply to code from NTM: NEXT, Hbm's Nuclear Tech Mod or third parties, or to
contributions by other people, which stay under their own terms. They apply to the releases
whose source code includes this section; earlier releases, up to and including 1.0.0-beta.11,
remain under the LGPL-3.0 alone.

An "unmodified release of NTM: DE" is a jar published by SecretAgent12, or a jar built without
changes from a release tag or a commit on a branch of
https://github.com/SecretAgent12/NTM-Decentralized-Edition, with its name, version and metadata
unchanged.

1. **Notices (7b).** Keep every copyright notice, SPDX line, license notice and author attribution
   in the Material intact, in the files where they appear. Moving this information into a separate
   file (such as REUSE.toml or a NOTICE inside the jar) and removing it from the files does not
   satisfy this term.

2. **Attribution in the work (7b).** A work that contains the Material, other than an unmodified
   release of NTM: DE or a work that includes one only as an unchanged, separate file (such as a
   modpack, a server pack or a mod that bundles it as a jar-in-jar dependency), must show in its
   Appropriate Legal Notices (for a Minecraft mod: the metadata shown in the in-game mod list,
   e.g. `neoforge.mods.toml`) the line
   "Based on NTM: Decentralized Edition by SecretAgent12 — https://github.com/SecretAgent12/NTM-Decentralized-Edition"
   and must list SecretAgent12 among its authors.

3. **Origin (7c).** Do not misrepresent the origin of the Material. In particular, do not present
   the Material, or a work that contains it, as your own original work or your own port.
   A work that contains the Material, other than an unmodified release of NTM: DE or a work that
   includes one only as an unchanged, separate file, must be marked as different from the original:
   - it must use a different name and a version number that cannot be confused with an NTM: DE
     release;
   - wherever it is published (project page, repository README, release page), its description
     must state prominently, at the beginning of the description and before the first image,
     video or banner, that it is based on NTM: Decentralized Edition by SecretAgent12, with a link
     to the original project. A mention only in a changelog, a credits list further down, or a
     file inside the download is not enough.

4. **Publicity (7d).** The names "SecretAgent12" and "NTM: Decentralized Edition" may not be used to
   promote or endorse a modified version, beyond the attribution required above and factual
   statements about its origin.

5. **Names and logo (7e).** No rights are granted to the names "NTM: Decentralized Edition",
   "NTM: DE", "NTM DE" and "Decentralized Edition", to names confusingly similar to them, or to the
   NTM: DE logo, except to refer to this project, to redistribute unmodified releases, and as
   allowed under "Addon names" below.

**Addon names.** An addon (a separate mod that requires NTM: DE to be installed and does not
contain the Material) may include the name of this project in its own name (for example
"NTM DE: Lasers" or "Lasers for NTM: DE"), as long as:
- its description states clearly, at the beginning, that it is a community addon, not made by
  SecretAgent12 and not affiliated with NTM: DE;
- it does not use the NTM: DE logo as its own icon or logo;
- it does not use the name of an official addon published by SecretAgent12.

These terms do not restrict:
- redistributing unmodified releases, on their own or as unchanged files inside other works
  (modpacks, server packs, jar-in-jar, mirrors);
- datapacks, KubeJS scripts, resource packs, configs and addons that do not contain the Material;
- contributions to this repository.

**Upstream projects.** The official repositories and releases of NTM: NEXT
(https://github.com/Warfactory-Official/ntm-next) and of Hbm's Nuclear Tech Mod
(https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT) may incorporate the Material without
following terms 2 and 3, as long as term 1 (keeping the file headers) is followed. This permission
covers only those projects themselves, not other works based on them.

If these terms are not followed, section 8 of the GNU GPL v3 applies: your rights under the
license end, and can be restored as described there (for example, by fixing the problem within
30 days of the first notice).
