// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.entity.Entities26;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.x com.hbm.backport.entity.Entities26.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS on 1.21.1 (see Entities26): a setBlock with
 * that bit removes the old block entity before the change (so onRemove / the preRemoveSideEffects
 * bridge drop nothing) and re-runs the call with the bit stripped.
 */
@Mixin(Level.class)
public abstract class EntitySkipBlockEntitySideEffectsMixin {

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void backport$skipBlockEntitySideEffects(
            BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if ((flags & Entities26.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS) == 0) return;
        Level level = (Level) (Object) this;
        if (!level.isOutsideBuildHeight(pos)) Entities26.removeBlockEntityQuietly(level, pos, state);
        cir.setReturnValue(level.setBlock(pos, state, Entities26.vanillaFlags(flags), recursionLeft));
    }
}
