// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.oil;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.backport.BlockEntityCompat;

public class BlockEntityFractionSpacer extends BlockEntityCompat
        implements GraphResident, FoldedCoreResident {

    public BlockEntityFractionSpacer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRACTION_SPACER.get(), pos, state);
    }

    // backport: hbm-compiler FoldedLifecycle
    @Override
    public void setRemoved() {
        super.setRemoved();
        com.hbm.tileentity.FoldedCoreResident.onRemove(this);
    }

    // backport: hbm-compiler FoldedLifecycle
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        com.hbm.tileentity.FoldedCoreResident.onLoad(this);
    }
}
