// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.platform.Services;
import com.hbm.tileentity.PendingCoreInvalidation;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * backport-fix: BF-075 — the "chunk became accessible" hooks for chunks of physics contraptions.
 *
 * <p>ntm-next runs them from ChunkMap.onFullChunkStatusChange (MixinChunkMapFullStatus): cable,
 * pipe and tube nodes parked or queued for a chunk wake up, fluid flush registers unpark, assembled
 * members declare their endpoints and folded cores waiting for the chunk re-index. Sable's plot
 * chunks never go through ChunkMap (ServerLevelPlot loads them itself), so after a world reload
 * none of this ran for a contraption. Sable does post NeoForge's ChunkEvent.Load for every plot
 * chunk it loads (onPlotChunkLoaded); for those chunks the same hooks run here. Chunks on the
 * ground keep going through ChunkMap only.
 */
public final class SubLevelChunks {

    private SubLevelChunks() {}

    public static void init() {
        Services.SERVER.onChunkLoad(SubLevelChunks::onChunkLoad);
    }

    private static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        var pos = chunk.getPos();
        if (!SubLevelSpace.inSubLevel(level, pos.getMiddleBlockX(), pos.getMiddleBlockZ())) return;
        AssembledMembers.onChunkFull(level, chunk);
        LevelNodeGraph.onChunkFull(level, chunk);
        FlushIndex.onChunkFull(level, chunk);
        PendingCoreInvalidation.onChunkFull(level, chunk);
    }
}
