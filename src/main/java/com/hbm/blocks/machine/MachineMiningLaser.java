// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineMiningLaser;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.BlockPos;
import com.hbm.backport.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

// backport: @RefreshesNeighborState(be = BlockEntityMachineMiningLaser.class, calling = {"updateRedstonePower", "refreshUnloadTargets"}) woven below
public class MachineMiningLaser extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock {
    public MachineMiningLaser(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineMiningLaser(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.MINING_LASER)
                .powerIn()
                .fluidOut()
                .items()
                .itemsAtCells()
                .fe();
    }

    // backport: woven by hbm-compiler NeighborEdge (@RefreshesNeighborState)
    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos core, net.minecraft.core.BlockPos cell) {
        net.minecraft.world.level.block.entity.BlockEntity hbm$be = level.getBlockEntity(core);
        if (hbm$be instanceof BlockEntityMachineMiningLaser) {
            ((BlockEntityMachineMiningLaser) hbm$be).updateRedstonePower();
            ((BlockEntityMachineMiningLaser) hbm$be).refreshUnloadTargets();
        }
    }
}
