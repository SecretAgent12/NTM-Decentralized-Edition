// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.sable;

import com.hbm.backport.SableTntImpact;
import com.hbm.blocks.bomb.BlockTNTBase;
import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import org.spongepowered.asm.mixin.Mixin;

/**
 * backport-fix: BF-085 Sable compat (not in ntm-next). Sable gives every TntBlock a collision
 * callback that spawns vanilla primed TNT; NTM explosives override it with one that primes their
 * own charge (SableTntImpact). Applied only when Sable is present (SableMixinPlugin); compiled
 * against stubs (src/sableStubs).
 */
@Mixin(BlockTNTBase.class)
public abstract class MixinSableTntImpact implements BlockWithSubLevelCollisionCallback {

    @Override
    public BlockSubLevelCollisionCallback sable$getCallback() {
        return SableTntImpact.INSTANCE;
    }
}
