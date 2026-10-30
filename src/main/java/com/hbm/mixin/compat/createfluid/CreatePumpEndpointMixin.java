// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.compat.createfluid;

import com.hbm.integration.create.CreateMultiblockCells;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * backport-fix: BF-016 a Create pump only runs when both ends of its pipe line reach something that
 * takes or gives fluid ({@code hasReachedValidEndpoint}). That check asks the block entity at the
 * end; with none it falls back to "is it an open end", which says no as soon as the block has a
 * fluid capability (CreateFluidPropagatorMixin makes an NTM cell say yes). So a pipe drew its
 * flange on a tank's port but the pump never moved anything; the core, with its block entity,
 * worked. For an NTM cell the block capability is asked directly, as for a block entity.
 */
@Mixin(value = PumpBlockEntity.class, remap = false)
public abstract class CreatePumpEndpointMixin {

    @Inject(method = "hasReachedValidEndpoint", at = @At("HEAD"), cancellable = true, require = 0)
    private void hbm$multiblockCell(
            LevelAccessor world, BlockFace face, boolean pull, CallbackInfoReturnable<Boolean> cir) {
        BlockPos cell = face.getConnectedPos();
        if (cell == null) return;
        if (!(world instanceof Level level) || !CreateMultiblockCells.isNtmCell(world, cell)) return;
        cir.setReturnValue(
                level.getCapability(Capabilities.FluidHandler.BLOCK, cell, face.getFace().getOpposite())
                        != null);
    }
}
