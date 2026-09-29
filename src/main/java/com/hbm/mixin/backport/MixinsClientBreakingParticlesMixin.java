// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * CLIENT-ONLY. The 26.x MixinClientLevel#hbm$hitParticlesFollowTheMachine: 26.x
 * ClientLevel.addBreakingBlockEffect(pos, direction[, hitResult]) spawns the block-hit particles;
 * 1.21.1 does it in ParticleEngine#crack(pos, direction) and NeoForge's
 * ParticleEngine#addBlockHitEffects(pos, hitResult) (which asks the block's client extensions,
 * then calls crack). The block state both read is replaced by the multiblock owner's state, so hit
 * particles on a machine's dummy blocks look like the machine.
 */
@Mixin(ParticleEngine.class)
public abstract class MixinsClientBreakingParticlesMixin {

    @Shadow protected ClientLevel level;

    @ModifyExpressionValue(
            method = {
                "crack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V",
                "addBlockHitEffects(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)V"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState hbm$hitParticlesFollowTheMachine(
            BlockState original, @Local(argsOnly = true) BlockPos pos) {
        BlockState owner = MultiblockSurface.particleState(level, pos, original);
        return owner == null ? original : owner;
    }
}
