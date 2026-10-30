// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.compat.createfluid;

import com.hbm.integration.create.CreateMultiblockCells;
import com.simibubi.create.content.fluids.FluidPropagator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * backport-fix: BF-016 Create's pipes connect to an NTM multiblock cell (a tank's side, a machine's
 * fluid port). {@code hasFluidCapability} decides whether a pipe draws a connection, keeps flow
 * going and treats an open end as attached (FluidPipeBlock, FluidTransportBehaviour,
 * PipeConnection, FluidPropagator); it gives up when the cell has no block entity. For an NTM cell
 * the block capability is asked directly. Applied only with Create installed (ClientCompatPlugin);
 * {@code require = 0} lets it fall away on a Create that renamed the method.
 */
@Mixin(value = FluidPropagator.class, remap = false)
public abstract class CreateFluidPropagatorMixin {

    @Inject(method = "hasFluidCapability", at = @At("HEAD"), cancellable = true, require = 0)
    private static void hbm$multiblockCell(
            BlockGetter world, BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (!(world instanceof Level level) || !CreateMultiblockCells.isNtmCell(world, pos)) return;
        cir.setReturnValue(level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null);
    }
}
