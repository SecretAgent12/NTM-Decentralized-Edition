# NTM: Decentralized Edition — backport of ntm-next to Minecraft 1.21.1

This is a **modified version** of [ntm-next](https://github.com/Warfactory-Official/ntm-next)
(commit `3f9a261`, NeoForge module, Minecraft 26.2) by movblock, itself a port of
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

ntm-next's `native/` module (All Rights Reserved) is **not** part of this backport; the mod uses
ntm-next's Java fallback instead.

Whoever receives a built jar of this mod is entitled to its corresponding source code under the
terms of the LGPL.
