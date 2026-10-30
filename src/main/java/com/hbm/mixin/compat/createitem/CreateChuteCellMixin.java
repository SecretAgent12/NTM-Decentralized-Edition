// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.compat.createitem;

import com.hbm.integration.create.CreateMultiblockCells;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * backport-fix: BF-118 a Create chute on a physics build reaches an NTM multiblock cell. The chute
 * keeps the capability of its neighbour in a NeoForge cache, and Sable wraps that lookup with its
 * own search through builds and the world; for a cell (no block entity) the chute on a build got
 * nothing, while the core and single blocks worked. For an NTM cell the capability is asked fresh
 * every time, as a vanilla hopper does (which works there). Applied only with Create installed.
 */
@Mixin(value = ChuteBlockEntity.class, remap = false)
public abstract class CreateChuteCellMixin {

    @Inject(method = "grabCapability", at = @At("HEAD"), cancellable = true, require = 0)
    private void hbm$multiblockCell(Direction side, CallbackInfoReturnable<IItemHandler> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) return;
        BlockPos cell = self.getBlockPos().relative(side);
        if (!CreateMultiblockCells.isNtmCell(level, cell)) return;
        {
            Direction face = side.getOpposite();
            cir.setReturnValue(level.getCapability(Capabilities.ItemHandler.BLOCK, cell, face));
        }
    }
}
