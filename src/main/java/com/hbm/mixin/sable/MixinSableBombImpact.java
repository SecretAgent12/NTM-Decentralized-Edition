// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.sable;

import com.hbm.backport.SableBombImpact;
import com.hbm.blocks.bomb.BombMulti;
import com.hbm.blocks.bomb.NukeBalefire;
import com.hbm.blocks.bomb.NukeBoy;
import com.hbm.blocks.bomb.NukeCustom;
import com.hbm.blocks.bomb.NukeMan;
import com.hbm.blocks.bomb.NukeSolinium;
import com.hbm.blocks.bomb.NukeTsar;
import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Sable compat (not in ntm-next): the bombs with a nose and tail get a contact fuze on physics
 * builds (SableBombImpact). Ground devices (Gadget, Ivy Mike, N2, F.L.E.I.J.A., prototype) have no
 * nose and stay redstone-only. Applied only when Sable is present
 * (SableMixinPlugin); compiled against stubs (src/sableStubs).
 */
@Mixin({
    NukeBoy.class,
    NukeMan.class,
    NukeTsar.class,
    NukeCustom.class,
    NukeSolinium.class,
    NukeBalefire.class,
    BombMulti.class
})
public abstract class MixinSableBombImpact implements BlockWithSubLevelCollisionCallback {

    @Override
    public BlockSubLevelCollisionCallback sable$getCallback() {
        return SableBombImpact.INSTANCE;
    }
}
