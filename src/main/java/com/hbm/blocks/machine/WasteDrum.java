// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.tileentity.machine.BlockEntityWasteDrum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.Orientation;
import org.jspecify.annotations.Nullable;

// backport: @RefreshesNeighborState(be = BlockEntityWasteDrum.class, calling = "refreshWaterFaces") woven below
public class WasteDrum extends BlockMachineBase {

    public WasteDrum(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityWasteDrum(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {

        for (Direction dir : Direction.VALUES) {
            if (dir == Direction.DOWN || !level.getBlockState(pos.relative(dir)).is(Blocks.WATER))
                continue;
            int ox = dir.getStepX(), oy = dir.getStepY(), oz = dir.getStepZ();
            double ix = pos.getX() + 0.5 + ox + rand.nextDouble() - 0.5;
            double iy = pos.getY() + 0.5 + oy + rand.nextDouble() - 0.5;
            double iz = pos.getZ() + 0.5 + oz + rand.nextDouble() - 0.5;
            if (ox != 0) ix = pos.getX() + 0.5 + ox * 0.5 + rand.nextDouble() * 0.125 * ox;
            if (oy != 0) iy = pos.getY() + 0.5 + oy * 0.5 + rand.nextDouble() * 0.125 * oy;
            if (oz != 0) iz = pos.getZ() + 0.5 + oz * 0.5 + rand.nextDouble() * 0.125 * oz;
            level.addParticle(ParticleTypes.BUBBLE, ix, iy, iz, 0.0, 0.2, 0.0);
        }
    }

    // backport: woven by hbm-compiler NeighborEdge (@RefreshesNeighborState)
    @Override
    protected void neighborChanged(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, com.hbm.backport.Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(pos);
        if (hbm$be instanceof BlockEntityWasteDrum) {
            ((BlockEntityWasteDrum) hbm$be).refreshWaterFaces();
        }
    }

    @Override
    protected void onPlace(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(pos);
        if (hbm$be instanceof BlockEntityWasteDrum) {
            ((BlockEntityWasteDrum) hbm$be).refreshWaterFaces();
        }
    }
}
