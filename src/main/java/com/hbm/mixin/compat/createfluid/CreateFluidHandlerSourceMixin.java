// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.compat.createfluid;

import com.hbm.integration.create.CreateMultiblockCells;
import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport-fix: BF-016 a Create pump moves fluid in and out of an NTM multiblock cell. The pipe
 * end's source ({@code FlowSource$FluidHandler.manageSource}) builds its capability cache only
 * from the block entity in front of it, so a cell without one gave no source and the pump moved
 * nothing. For an NTM cell the cache is built on the cell's position, the way Create builds it for
 * a block entity: it is dropped when the cell's capabilities are invalidated (the cells do that when
 * a multiblock is built or broken) and rebuilt on the next pass. Only on the server, as Create;
 * the ponder level is left to Create.
 */
@Mixin(targets = "com.simibubi.create.content.fluids.FlowSource$FluidHandler", remap = false)
public abstract class CreateFluidHandlerSourceMixin {

    @Shadow ICapabilityProvider<IFluidHandler> fluidHandlerCache;

    @Inject(method = "manageSource", at = @At("HEAD"), cancellable = true, require = 0)
    private void hbm$multiblockCell(Level world, BlockEntity networkBE, CallbackInfo ci) {
        if (fluidHandlerCache != null || !(world instanceof ServerLevel server)) return;
        BlockFace location = ((CreateFlowSourceAccessor) (Object) this).hbm$location();
        BlockPos cell = location.getConnectedPos();
        if (!CreateMultiblockCells.isNtmCell(world, cell)) return;
        Direction face = location.getOppositeFace();
        fluidHandlerCache =
                ICapabilityProvider.of(
                        (Runnable invalidate) ->
                                BlockCapabilityCache.create(
                                        Capabilities.FluidHandler.BLOCK,
                                        server,
                                        cell,
                                        face,
                                        () -> !networkBE.isRemoved(),
                                        () -> {
                                            fluidHandlerCache = null;
                                            invalidate.run();
                                        }));
        ci.cancel();
    }
}
