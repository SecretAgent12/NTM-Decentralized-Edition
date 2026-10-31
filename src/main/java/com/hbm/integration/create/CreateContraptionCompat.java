// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.integration.create;

import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.blocks.machine.BlockSpotlight;
import com.hbm.blocks.network.DroneWaypointBlock;
import com.hbm.blocks.network.RadioTorchBlock;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * backport-fix: BF-127 (GitHub #32) NTM blocks that hang on the block behind them (radio torches,
 * drone waypoints, spotlights, explosive charges) on Create contraptions.
 *
 * <p>Each of them breaks off and drops as soon as the block it hangs on is gone. Create takes a
 * contraption's blocks out of the world one by one, and only blocks it knows to be brittle (vanilla
 * torches, signs...) go first; ours went in any order, so whenever the support went before them
 * they dropped, glued or not. Create also didn't know they hang on that block, so without glue they
 * stayed behind.
 *
 * <p>Here they are brittle (taken out first, put back last) and attached towards their support,
 * like a wall torch. Only called with Create installed.
 */
public final class CreateContraptionCompat {

    private CreateContraptionCompat() {}

    public static void register() {
        BlockMovementChecks.registerBrittleCheck(
                state -> hangsBehind(state) ? CheckResult.SUCCESS : CheckResult.PASS);
        BlockMovementChecks.registerAttachedCheck(CreateContraptionCompat::attachedTowards);
    }

    private static CheckResult attachedTowards(
            BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!hangsBehind(state)) return CheckResult.PASS;
        {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            // the support is behind the block: facing points away from it
            return CheckResult.of(direction == facing.getOpposite());
        }
    }

    private static boolean hangsBehind(BlockState state) {
        Block block = state.getBlock();
        return (block instanceof RadioTorchBlock
                        || block instanceof DroneWaypointBlock
                        || block instanceof BlockSpotlight
                        || block instanceof BlockChargeBase)
                && state.hasProperty(BlockStateProperties.FACING);
    }
}
