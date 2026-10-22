// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import com.hbm.blockentity.ChunkPublication;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.platform.NeoForgeServerEvents;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 *
 * <p>backport-fix: BF-106 multiblock cells on a contraption. Which cell belongs to which core lives in
 * the per-chunk core index, and Sable saves a plot chunk without it, so after a reload the cells of
 * a press, a furnace etc. had no owner: only a click on the core opened the GUI. NEXT 1.0.2 rebuilt
 * the index whenever a core's block entity loaded (FoldedCoreResident.onLoad, a tick later, waiting
 * for the chunk if needed). NEXT 1.0.3 moved that into ChunkPublication (hbm$blockEntityLoaded),
 * which runs from ChunkMap only, so contraptions lost it. The 1.0.2 scheme comes back here for plot
 * chunks: once a loaded plot chunk is readable, every multiblock core in it and in the chunks
 * around it (a big multiblock reaches into its neighbours, and its cells may load after its core)
 * rebuilds the index of its loaded cells.
 */
public final class SubLevelChunks {

    // backport-fix: BF-106 plot chunks waiting for their multiblock cores to re-index (chunk -> ticks left)
    private static final Map<ServerLevel, Long2IntOpenHashMap> REINDEX = new WeakHashMap<>();
    private static final int REINDEX_WAIT_TICKS = 100;
    private static final int CORE_REACH_CHUNKS = 2;

    private SubLevelChunks() {}

    public static void init() {
        NeoForgeServerEvents.onChunkLoad(SubLevelChunks::onChunkLoad);
        NeoForgeServerEvents.onChunkUnload(SubLevelChunks::onChunkUnload);
        NeoForgeServerEvents.onServerTickPost(SubLevelChunks::reindexPending); // backport-fix: BF-106
    }

    private static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        var pos = chunk.getPos();
        if (!SubLevelSpace.inSubLevel(level, pos.getMiddleBlockX(), pos.getMiddleBlockZ())) return;
        ChunkPublication.promote(level, chunk);
        REINDEX.computeIfAbsent(level, l -> new Long2IntOpenHashMap())
                .put(pos.toLong(), REINDEX_WAIT_TICKS); // backport-fix: BF-106
    }

    // without this a reloaded plot chunk would still count as published and never promote again
    private static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        var pos = chunk.getPos();
        if (!SubLevelSpace.inSubLevel(level, pos.getMiddleBlockX(), pos.getMiddleBlockZ())) return;
        ChunkPublication.demote(level, pos);
        Long2IntOpenHashMap pending = REINDEX.get(level); // backport-fix: BF-106
        if (pending != null) pending.remove(pos.toLong());
    }

    /** backport-fix: BF-106 the multiblock cores around each readable pending plot chunk re-index their cells. */
    private static void reindexPending(MinecraftServer server) {
        if (REINDEX.isEmpty()) return;
        for (var entry : List.copyOf(REINDEX.entrySet())) {
            ServerLevel level = entry.getKey();
            Long2IntOpenHashMap pending = entry.getValue();
            if (pending.isEmpty()) continue;
            LongOpenHashSet cores = new LongOpenHashSet();
            var it = pending.long2IntEntrySet().iterator();
            while (it.hasNext()) {
                Long2IntMap.Entry chunk = it.next();
                int cx = ChunkPos.getX(chunk.getLongKey());
                int cz = ChunkPos.getZ(chunk.getLongKey());
                if (BlockMultiblockCore.readableChunk(level, cx << 4, cz << 4) == null) {
                    if (chunk.getIntValue() <= 1) it.remove();
                    else chunk.setValue(chunk.getIntValue() - 1);
                    continue;
                }
                it.remove();
                for (int dx = -CORE_REACH_CHUNKS; dx <= CORE_REACH_CHUNKS; dx++) {
                    for (int dz = -CORE_REACH_CHUNKS; dz <= CORE_REACH_CHUNKS; dz++) {
                        LevelChunk near = BlockMultiblockCore.readableChunk(level, (cx + dx) << 4, (cz + dz) << 4);
                        if (near == null) continue;
                        for (BlockEntity be : near.getBlockEntities().values()) {
                            if (!be.isRemoved() && be.getBlockState().getBlock() instanceof BlockMultiblockCore)
                                cores.add(be.getBlockPos().asLong());
                        }
                    }
                }
            }
            for (long packed : cores) {
                BlockPos core = BlockPos.of(packed);
                if (level.getBlockState(core).getBlock() instanceof BlockMultiblockCore block)
                    block.reindexLoadedCells(level, core);
            }
        }
    }
}
