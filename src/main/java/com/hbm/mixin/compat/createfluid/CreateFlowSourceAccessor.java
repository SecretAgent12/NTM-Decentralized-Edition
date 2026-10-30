// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.compat.createfluid;

import com.simibubi.create.content.fluids.FlowSource;
import net.createmod.catnip.math.BlockFace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** backport-fix: BF-016 the face a Create pipe end is attached to (package-private in Create). */
@Mixin(value = FlowSource.class, remap = false)
public interface CreateFlowSourceAccessor {

    @Accessor("location")
    BlockFace hbm$location();
}
