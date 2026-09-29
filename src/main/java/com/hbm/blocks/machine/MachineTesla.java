// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ISectionGeometry;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.BlockEntityTesla;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.Orientation;
import org.jspecify.annotations.Nullable;
import com.hbm.backport.compat.BlockCompat;

// backport: @RefreshesNeighborState(be = BlockEntityTesla.class, calling = "refreshMeteorBattery") woven below
public class MachineTesla extends BlockCompat
        implements ITickingBlock, ICapabilityBlock, ISectionGeometry {
    public MachineTesla(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityTesla(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TESLA_COIL).powerIn().fe();
    }

    // backport: woven by hbm-compiler NeighborEdge (@RefreshesNeighborState)
    @Override
    protected void neighborChanged(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, com.hbm.backport.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(pos);
        if (hbm$be instanceof BlockEntityTesla) {
            ((BlockEntityTesla) hbm$be).refreshMeteorBattery();
        }
    }

    @Override
    protected void onPlace(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(pos);
        if (hbm$be instanceof BlockEntityTesla) {
            ((BlockEntityTesla) hbm$be).refreshMeteorBattery();
        }
    }
}
