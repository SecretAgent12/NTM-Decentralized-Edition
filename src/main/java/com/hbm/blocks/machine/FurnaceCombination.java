// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityFurnaceCombination;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// backport: @RefreshesNeighborState(be = BlockEntityFurnaceCombination.class, calling = "refreshHeatBelow") woven below
public class FurnaceCombination extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {
    public FurnaceCombination(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityFurnaceCombination(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.COMBINATION_OVEN).fluidOut().items().itemsAtCells();
    }

    // backport: woven by hbm-compiler NeighborEdge (@RefreshesNeighborState)
    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos core, net.minecraft.core.BlockPos cell) {
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(core);
        if (hbm$be instanceof BlockEntityFurnaceCombination) {
            ((BlockEntityFurnaceCombination) hbm$be).refreshHeatBelow();
        }
    }
}
