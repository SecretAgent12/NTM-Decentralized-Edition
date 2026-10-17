// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.BlockEntityLaunchPad;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

// backport: @RefreshesNeighborState(be = BlockEntityLaunchPad.class, calling = "refreshRedstone") woven below
public class LaunchPad extends BlockMultiblockCore
        implements ITickingBlock, IBomb, ICapabilityBlock {
    public LaunchPad(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityLaunchPad(pos, state);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        BlockPos core = ownerOf(level, pos) != null ? ownerOf(level, pos).pos() : pos;
        if (level.getBlockEntity(core) instanceof BlockEntityLaunchPad pad) {
            return pad.launchFromDesignator();
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.LAUNCH1)
                .powerIn()
                .fluidIn()
                .items()
                .fe()
                .faces(BlockEntityLaunchPad.class, (be, side) -> side.getAxis().isHorizontal())
                .fluidFaces(
                        BlockEntityLaunchPad.class,
                        (be, face) -> face.side().getAxis().isHorizontal());
    }

    // backport: woven by hbm-compiler NeighborEdge (@RefreshesNeighborState)
    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos core, net.minecraft.core.BlockPos cell) {
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(core);
        if (hbm$be instanceof BlockEntityLaunchPad) {
            ((BlockEntityLaunchPad) hbm$be).refreshRedstone();
        }
    }
}
