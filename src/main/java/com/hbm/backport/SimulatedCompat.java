// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.hbm.interfaces.RigidPistonStructure;
import dev.simulated_team.simulated.index.SimBlockMovementChecks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * backport: Create: Aeronautics ("simulated") compat, not in ntm-next. Loaded only when the
 * simulated mod is present (NuclearTechNeoForge).
 *
 * <p>The Aeronautics assembler gathers blocks by attachment, so without glue it took a single
 * multiblock cell (dummy) into the physics sub-level: the machine stayed behind without it and
 * the cell flew off as an orphan (NTM Reborn broke the whole multiblock instead). Every member
 * of an NTM multiblock now pulls in the rest of its structure - the same member list the
 * rigid-piston support uses (RigidPistonStructure) - so the machine is always assembled whole.
 */
public final class SimulatedCompat {

    private SimulatedCompat() {}

    public static void register() {
        SimBlockMovementChecks.registerAdditionalBlocks(SimulatedCompat::wholeStructure);
    }

    private static Iterable<BlockPos> wholeStructure(
            BlockState state, Level level, BlockPos pos, java.util.Set<BlockPos> visited) {
        if (!(state.getBlock() instanceof RigidPistonStructure rigid)) return List.of();
        BlockPos core = rigid.rigidStructureCore(level, pos);
        if (core == null) return List.of();
        List<BlockPos> out = new ArrayList<>();
        if (!visited.contains(core)) out.add(core.immutable());
        BlockState coreState = level.getBlockState(core);
        if (!(coreState.getBlock() instanceof RigidPistonStructure owner)) return out;
        for (BlockPos member : owner.rigidStructureBlocks(level, core)) {
            if (!visited.contains(member)) out.add(member.immutable());
        }
        return out;
    }
}
