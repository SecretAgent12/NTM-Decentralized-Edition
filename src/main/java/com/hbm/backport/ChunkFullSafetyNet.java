// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import com.hbm.blockentity.ChunkPublication;
import com.hbm.platform.NeoForgeServerEvents;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * backport-fix: BF-037 (DE 2026-10-04, commit deff40fb) safety net behind NEXT's chunk publication.
 *
 * <p>NEXT 1.0.0 woke the networks only when a chunk reported exactly FULL; a chunk whose ticket jumps straight to
 * ticking (login, respawn, teleport, a dimension change) never did, and cables and pipes stayed stuck until the
 * world was reloaded. DE fixed it on 2026-10-04 (wake on the first status at or above FULL, re-arm below FULL).
 * NEXT 1.0.3 (2026-10-12) does the same in {@code MixinChunkMap} plus earlier hooks ({@code ChunkPublication}:
 * risen/settle, startTickingChunk, chunk registration) and stays the primary path.
 *
 * <p>DE's tracking ({@code MixinChunkMapFullStatus}) runs beside it: every chunk that rises to FULL or above is
 * checked one server tick later, and if NEXT's path has not published it (e.g. the chunk was not readable yet when
 * the status changed), it is published here. {@link ChunkPublication#promote} publishes each chunk once, so a chunk
 * NEXT already handled is left alone.
 */
public final class ChunkFullSafetyNet {
    private static final Logger LOGGER = LoggerFactory.getLogger("hbm-backport");
    private static final Map<ServerLevel, LongOpenHashSet> RISEN = new WeakHashMap<>();

    private ChunkFullSafetyNet() {}

    public static void init() {
        NeoForgeServerEvents.onServerTickPost(ChunkFullSafetyNet::tick);
        NeoForgeServerEvents.onServerStopping(server -> RISEN.clear());
    }

    /** The chunk reached FULL or above for the first time since it was last below FULL. */
    public static void rose(ServerLevel level, ChunkPos pos) {
        RISEN.computeIfAbsent(level, l -> new LongOpenHashSet()).add(pos.toLong());
    }

    /** The chunk dropped below FULL before the check ran. */
    public static void fell(ServerLevel level, ChunkPos pos) {
        LongOpenHashSet risen = RISEN.get(level);
        if (risen != null) risen.remove(pos.toLong());
    }

    private static void tick(MinecraftServer server) {
        if (RISEN.isEmpty()) return;
        for (Map.Entry<ServerLevel, LongOpenHashSet> entry : RISEN.entrySet()) {
            ServerLevel level = entry.getKey();
            LongOpenHashSet risen = entry.getValue();
            if (risen.isEmpty()) continue;
            LongOpenHashSet published = level.hbm$publishedChunks();
            for (LongIterator it = risen.iterator(); it.hasNext(); ) {
                long key = it.nextLong();
                LevelChunk chunk =
                        level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
                // not readable yet: keep it for the next tick (fell() drops it if it goes below FULL)
                if (chunk == null) continue;
                it.remove();
                if (!chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL)) continue;
                if (published.contains(key)) continue;
                LOGGER.debug("BF-037 safety net published chunk {} in {}", chunk.getPos(), level.dimension().location());
                ChunkPublication.promote(level, chunk);
            }
        }
    }
}
