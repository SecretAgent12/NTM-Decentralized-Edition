// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.sable;

import com.hbm.backport.SubLevelMoves;
import com.hbm.backport.compat.BlockCompat;
import com.hbm.backport.compat.DirectionalBlockCompat;
import com.hbm.backport.compat.HorizontalDirectionalBlockCompat;
import com.hbm.backport.compat.RotatedPillarBlockCompat;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * backport: Sable compat (not in ntm-next). Makes the hbm block bases Sable's
 * BlockSubLevelAssemblyListener, so a block moved into / out of a physics sub-level carries its
 * graph node and multiblock index along (see SubLevelMoves). Applied only when Sable is present
 * (SableMixinPlugin); compiled against a stub of the interface (src/sableStubs).
 */
@Mixin({BlockCompat.class, HorizontalDirectionalBlockCompat.class, DirectionalBlockCompat.class,
        RotatedPillarBlockCompat.class})
public abstract class MixinSableAssemblyListener implements BlockSubLevelAssemblyListener {

    @Override
    public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState state,
            BlockPos oldPos, BlockPos newPos) {
        SubLevelMoves.moved(originLevel, resultingLevel, state, oldPos, newPos);
    }
}
