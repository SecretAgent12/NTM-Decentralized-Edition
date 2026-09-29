// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Backport of 26.x's ScheduledTickAccess, the tick scheduler updateShape
 * receives since 1.21.2. In 1.21.1 the LevelAccessor updateShape gets is itself
 * the scheduler, so this simply forwards to it.
 */
public interface ScheduledTickAccess {
    void scheduleTick(BlockPos pos, Block block, int delay);

    void scheduleTick(BlockPos pos, Fluid fluid, int delay);

    static ScheduledTickAccess of(LevelAccessor level) {
        return new ScheduledTickAccess() {
            @Override
            public void scheduleTick(BlockPos pos, Block block, int delay) {
                level.scheduleTick(pos, block, delay);
            }

            @Override
            public void scheduleTick(BlockPos pos, Fluid fluid, int delay) {
                level.scheduleTick(pos, fluid, delay);
            }
        };
    }
}
