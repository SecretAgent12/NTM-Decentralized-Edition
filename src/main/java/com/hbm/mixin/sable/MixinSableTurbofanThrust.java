// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.sable;

import com.hbm.backport.SableTurbofanThrust;
import com.hbm.blockentity.machine.BlockEntityMachineTurbofan;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Sable compat (not in ntm-next): the turbofan becomes a physics actor, so a running one pushes the
 * build it stands on (SableTurbofanThrust). Applied only when Sable is present (SableMixinPlugin);
 * compiled against stubs (src/sableStubs).
 */
@Mixin(BlockEntityMachineTurbofan.class)
public abstract class MixinSableTurbofanThrust implements BlockEntitySubLevelActor {

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        SableTurbofanThrust.physicsTick((BlockEntityMachineTurbofan) (Object) this, subLevel, timeStep);
    }
}
