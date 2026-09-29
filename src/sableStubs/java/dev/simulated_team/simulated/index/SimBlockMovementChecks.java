// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package dev.simulated_team.simulated.index;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Compile-time stand-in for the part of Create: Aeronautics' (simulated) SimBlockMovementChecks
 * the backport uses; not packaged, the real class comes from the mod at run time.
 */
public class SimBlockMovementChecks {
    public static synchronized void registerAdditionalBlocks(AdditionalBlocks additionalBlocks) {
        throw new UnsupportedOperationException("stub");
    }

    @FunctionalInterface
    public interface AdditionalBlocks {
        Iterable<BlockPos> addAdditionalBlocks(BlockState state, Level world, BlockPos pos, Set<BlockPos> visited);
    }
}
