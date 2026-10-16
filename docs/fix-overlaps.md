# Bug fixes shared with NTM: NEXT

NTM: DE is a backport of NTM: NEXT, so the two mods share most of their code and, naturally, most of their bugs.
While porting, I fix the bugs I run into and give each fix a number (`BF-NNN`). Sometimes NEXT fixes the same bug
too: sometimes before me, sometimes after, sometimes the same way and sometimes differently.

When that happens, I don't just take NEXT's version and quietly drop mine. I look at both, decide which one DE
keeps (or keep both), and write it down here. That way the history of every fix stays readable, and nobody's work
disappears from it, mine or NEXT's.

## How to read the table

- **Times are in UTC.** Both repositories record commit times in their authors' time zones (DE in +03:00, NEXT in
  +11:00), so they are converted here to make them comparable.
- **DE**: the public commit in the [DE repository](https://github.com/SecretAgent12/NTM-Decentralized-Edition)
  that fixed the bug.
- **NEXT**: the public release in the [NEXT repository](https://github.com/Warfactory-Official/ntm-next) that
  contains NEXT's fix. NEXT publishes each version as a single commit, so its fix may have existed privately
  before that.
- **In DE now**: which fix DE uses since the sync with NEXT 1.0.3. In the code, every one of these places carries a
  `// backport-fix: BF-NNN` comment with the date and commit of my fix and a note about NEXT's.

NEXT releases referenced below:

| Version | Published (UTC) | Commit |
|---|---|---|
| NEXT 1.0.1 | 2026-10-04 13:54 | [`1d0273ff`](https://github.com/Warfactory-Official/ntm-next/commit/1d0273ff8f712ccda33b7a703a5e506e8f078686) |
| NEXT 1.0.2 | 2026-10-07 20:28 | [`1fa24e27`](https://github.com/Warfactory-Official/ntm-next/commit/1fa24e2750bdc2f5f3a43dbd88d3abaa5cf0cfc6) |
| NEXT 1.0.3 | 2026-10-11 13:36 | [`1b8c6c2d`](https://github.com/Warfactory-Official/ntm-next/commit/1b8c6c2d0936de9d9f85926293f7666a1e6aac0a) |

## The fixes

| Fix | The bug | DE (UTC) | NEXT | First | In DE now |
|---|---|---|---|---|---|
| BF-005 | Gibs, bones and ashes froze in mid-air after an explosion | 2026-09-29 16:46, [`20b7ed09`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/20b7ed09261bc250f5293d0c93395971b6779ee3) | 1.0.1 | DE | NEXT's: a shared base class fixes it for every particle, not just these three |
| BF-011 | Factories and other land structures spawned in water | 2026-09-29 17:50, [`e7f8730d`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/e7f8730da8d7e2322dcf766ce0bc4bd17e6985ea) | 1.0.3 | DE | Both: NEXT's biome check, plus my check for flooded ground |
| BF-021 | The assembler's second upgrade slot sat off to the right of the slot drawn on the GUI | 2026-10-01 12:27, [`5a20727b`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/5a20727b07a3dcd7090bada2d61b5a372bd5a9dd) | 1.0.1 | DE | Same code in both |
| BF-023 | An interceptor without a radar target chased the last missile it saw, not the closest one | 2026-10-01 15:28, [`6d7649de`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/6d7649de74f8417169af8a9c290be8892150f329) | 1.0.1 | DE | Same code in both |
| BF-029 | A forged pipe colour request could make the server load chunks on demand | 2026-10-02 19:15, [`4a19c959`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/4a19c95917a78d54794bfb144ee1f61eb1d010d1) | 1.0.1 | DE | NEXT's: it limits requests to the chunks the player actually sees, which is stricter |
| BF-037 | Energy and fluid networks stopped working after a teleport or login until the world was reloaded | 2026-10-04 18:02, [`deff40fb`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/deff40fbf470462c816e999ad4cdbe3b9cd14a46) | 1.0.3 | DE | Both: NEXT's as the main path, mine as a safety net one tick later |
| BF-038 | NTM recipes added or edited with KubeJS failed to load | 2026-10-04 18:02, [`deff40fb`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/deff40fbf470462c816e999ad4cdbe3b9cd14a46) | 1.0.2 | DE | Same code in both |
| BF-039 | The meteorite and protection charms fit only helmets, not chestplates as in the original | 2026-10-04 18:10, [`fa14490d`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/fa14490d30639af0c81951849fb80f16906fcfc9) | 1.0.1 | NEXT, by about 4 hours | Same code in both |
| BF-052 | The foundry channel network failed to load from a save | 2026-10-05 12:17, [`b6365845`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/b6365845e3bcb4be24023f17eafc4bd6a2d13956) | 1.0.1 | NEXT, by about 22 hours | NEXT's; the save format is the same |
| BF-062 | The compressor's piston made no sound | 2026-10-05 18:56, [`2fd0c45b`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/2fd0c45b41ebb7becce4bd7d12244932a7676243) | 1.0.3 | DE | NEXT's: it follows the machine volume settings |
| BF-063 | JEI showed no crafting recipe for fluid identifiers | 2026-10-05 18:56, [`fa96b84d`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/fa96b84d8bb3c8b53895060ae33d5196484449df) | 1.0.3 | DE | Mine; NEXT's lookup would show the same recipes twice |
| BF-071 | The tool ability key could not be rebound | 2026-10-07 17:36, [`5282e6f8`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/5282e6f817defce3d960e641359f52728d5124ba) | 1.0.1 | NEXT | NEXT's: key handling was rewritten there |
| BF-083 | With a health armor mod and servos, health never regenerated past the normal 20 points while food kept draining | 2026-10-10 18:13, [`fd71212e`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/fd71212ee4ef65f987dd43f9338ee48153a316a4) | 1.0.1 | NEXT | NEXT's: it works the same way |
| BF-084 | Holding a stack of fluid cells crashed the client | 2026-10-10 18:23, [`55668394`](https://github.com/SecretAgent12/NTM-Decentralized-Edition/commit/556683949a4811a6c5a272d1453d8b184e0ac46d) | 1.0.3 | DE | Mine: NEXT's version can overflow on very large amounts |

In short: of these 14 bugs, DE fixed 10 first and NEXT fixed 4 first.

## A note on matching fixes

Where both fixes look alike, that alone says nothing about who took what from whom. Most of these are small changes
to the same code, and there is often only one sensible way to write them. Both mods are LGPL-3.0, so either side is
free to use the other's fixes. This page exists so the record stays complete: what was broken, who fixed it and
when, and which fix DE ships today.

I'll keep this page updated with every sync.
