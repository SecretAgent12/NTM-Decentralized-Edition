// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.albion;

import com.hbm.blockentity.machine.albion.BlockEntityPADipole;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// backport: @RefreshesNeighborState(be = BlockEntityPADipole.class, calling = "refreshRedstone") woven below
public class BlockPADipole extends BlockPACooled implements ICapabilityBlock {
    public BlockPADipole(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPADipole(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed()
                .powerIn()
                .fluidIn()
                .fluidOut()
                .fe()
                .faces(
                        BlockEntityPADipole.class,
                        (be, side) -> side == Direction.UP || side == Direction.DOWN)
                .fluidFaces(
                        BlockEntityPADipole.class,
                        (be, face) -> face.side() == Direction.UP || face.side() == Direction.DOWN);
    }

    // backport: woven by hbm-compiler NeighborEdge (@RefreshesNeighborState)
    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos core, net.minecraft.core.BlockPos cell) {
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(core);
        if (hbm$be instanceof BlockEntityPADipole) {
            ((BlockEntityPADipole) hbm$be).refreshRedstone();
        }
    }
}
