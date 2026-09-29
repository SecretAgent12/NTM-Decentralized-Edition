// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.BlockEntityPylonBase;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Sable (Create: Aeronautics) sub-level assembly and disassembly for ntm-next's position-keyed
 * side tables. Called by {@code com.hbm.mixin.sable.MixinSableAssemblyListener} (only applied
 * when Sable is installed) for every hbm block Sable moves.
 *
 * <p>backport: not in ntm-next (26.2 has no Sable). Sable moves blocks with its own chunk writes
 * while onPlace/onRemove are suppressed (SubLevelAssemblyHelper.moveBlocks), so nothing ntm-next
 * keeps outside the block/block entity follows the block: the cable/pipe/tube graph nodes
 * (LevelNodeGraph, keyed by position) stay at the old position and the new position has none,
 * and the per-chunk multiblock core index (cell -> core) is not written for the moved cells.
 * NTM Reborn survived this because its cables re-created their nodes from their block entity
 * every tick; ntm-next only does it in the block hooks. This does what those hooks would have
 * done: move each graph node (data, open faces, self-endpoint) from the old to the new
 * position, unindex the old cells and re-index the moved core's cells once the whole move is
 * done, and invalidate capability / endpoint caches on both sides.
 */
public final class SubLevelMoves {

    private SubLevelMoves() {}

    public static void moved(
            ServerLevel origin, ServerLevel resulting, BlockState state, BlockPos oldPos, BlockPos newPos) {
        BlockPos from = oldPos.immutable();
        BlockPos to = newPos.immutable();
        BlockState moved = resulting.getBlockState(to);
        int turns = quarterTurns(state, moved);

        List<LevelNodeGraph<?>> graphs = origin.hbm$graphs();
        for (int i = 0; i < graphs.size(); i++)
            moveNode(graphs.get(i), origin, from.asLong(), to.asLong(), turns, origin == resulting);

        if (state.getBlock() instanceof BlockMultiblockCell || state.getBlock() instanceof BlockMultiblockCore)
            BlockMultiblockCore.unindexCell(origin, from);

        Services.CAPS.invalidateCaps(origin, from);
        Services.CAPS.invalidateCaps(resulting, to);
        LevelNodeGraph.invalidateEndpointsAround(origin, from);
        LevelNodeGraph.invalidateEndpointsAt(resulting, to);

        schedule(resulting, to, moved.getBlock() instanceof BlockMultiblockCore);
    }

    // one task per level and move: runs after Sable has moved (and notified) every block
    private static final java.util.Map<ServerLevel, Batch> BATCHES = new java.util.IdentityHashMap<>();

    private static final class Batch {
        final java.util.List<BlockPos> moved = new java.util.ArrayList<>();
        final java.util.List<BlockPos> cores = new java.util.ArrayList<>();
    }

    private static void schedule(ServerLevel level, BlockPos to, boolean core) {
        Batch batch = BATCHES.get(level);
        if (batch == null) {
            Batch fresh = new Batch();
            BATCHES.put(level, fresh);
            MinecraftServer server = level.getServer();
            server.tell(server.wrapRunnable(() -> finish(level, fresh)));
            batch = fresh;
        }
        batch.moved.add(to);
        if (core) batch.cores.add(to);
    }

    private static void finish(ServerLevel level, Batch batch) {
        if (BATCHES.get(level) == batch) BATCHES.remove(level);
        // the cells were moved one by one before or after their core: index them now, like
        // FoldedCoreResident.onLoad does for a loaded core
        for (BlockPos core : batch.cores) {
            if (level.getBlockState(core).getBlock() instanceof BlockMultiblockCore block)
                block.reindexLoadedCells(level, core);
        }
        // Sable's neighbour-shape pass ran before the cells were indexed, so cables next to a
        // moved multiblock cell decided "nothing to connect to"; recompute their shapes now
        for (BlockPos pos : batch.moved) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) state.updateNeighbourShapes(level, pos, 3);
        }
    }

    private static <D> void moveNode(
            LevelNodeGraph<D> graph, ServerLevel origin, long from, long to, int turns, boolean sameLevel) {
        if (!graph.containsCell(from)) return;
        GraphNode<D> node = graph.firstClass(from); // promotes a segment interior cell to a node
        if (node == null) return;
        D data = node.data;
        int mask = rotate(node.openConnections, turns);
        boolean self = node.selfEndpoint;
        long[] peers = node.hasRemoteLinks() ? node.remoteLinks.toLongArray() : new long[0];
        graph.removeNode(from);
        // pylon wires link absolute positions; the peers drop the link like on a normal removal
        for (long peer : peers) BlockEntityPylonBase.refreshIfLoaded(origin, peer);
        // unverified: Sable keeps sub-level plots in the level they came from; a cross-level
        // move would need the matching graph of the other level and only removes the node
        if (!sameLevel || graph.containsCell(to)) return;
        graph.addNode(to, data, mask);
        if (self) graph.setSelfEndpoint(to, true);
    }

    /** Clockwise quarter turns about Y between the horizontal facing before and after the move. */
    private static int quarterTurns(BlockState before, BlockState after) {
        Direction a = facing(before), b = facing(after);
        if (a == null || b == null) return 0;
        int turns = 0;
        while (a != b && turns < 4) {
            a = a.getClockWise();
            turns++;
        }
        return turns & 3;
    }

    private static Direction facing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction d = state.getValue(BlockStateProperties.FACING);
            return d.getAxis().isHorizontal() ? d : null;
        }
        return null;
    }

    private static int rotate(int mask, int turns) {
        if (turns == 0) return mask;
        int out = mask & ((1 << Direction.DOWN.ordinal()) | (1 << Direction.UP.ordinal()));
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if ((mask & (1 << d.ordinal())) == 0) continue;
            Direction r = d;
            for (int i = 0; i < turns; i++) r = r.getClockWise();
            out |= 1 << r.ordinal();
        }
        return out;
    }
}
