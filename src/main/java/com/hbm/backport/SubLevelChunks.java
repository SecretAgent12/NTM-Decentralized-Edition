// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import com.hbm.blockentity.ChunkPublication;
import com.hbm.platform.NeoForgeServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * backport-fix: BF-075 — the "chunk became accessible" hooks for chunks of physics contraptions.
 *
 * <p>ntm-next runs them from ChunkMap.onFullChunkStatusChange (MixinChunkMap, which promotes and
 * demotes the chunk in ChunkPublication): cable, pipe and tube nodes parked or queued for a chunk
 * wake up, fluid flush registers unpark and assembled members declare their endpoints. Sable's plot
 * chunks never go through ChunkMap (ServerLevelPlot loads them itself), so after a world reload
 * none of this ran for a contraption. Sable does post NeoForge's ChunkEvent.Load for every plot
 * chunk it loads (onPlotChunkLoaded); for those chunks the same promotion runs here, and the
 * demotion on ChunkEvent.Unload. Chunks on the ground keep going through ChunkMap only.
 */
public final class SubLevelChunks {

    private SubLevelChunks() {}

    public static void init() {
        NeoForgeServerEvents.onChunkLoad(SubLevelChunks::onChunkLoad);
        NeoForgeServerEvents.onChunkUnload(SubLevelChunks::onChunkUnload);
    }

    private static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        var pos = chunk.getPos();
        if (!SubLevelSpace.inSubLevel(level, pos.getMiddleBlockX(), pos.getMiddleBlockZ())) return;
        ChunkPublication.promote(level, chunk);
    }

    // without this a reloaded plot chunk would still count as published and never promote again
    private static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        var pos = chunk.getPos();
        if (!SubLevelSpace.inSubLevel(level, pos.getMiddleBlockX(), pos.getMiddleBlockZ())) return;
        ChunkPublication.demote(level, pos);
    }
}
