// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.packet;

import com.hbm.packet.toclient.UnitPayload;
import com.hbm.tileentity.Synced;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ChunkTrackerIndex {
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<ObjectArrayList<ServerPlayer>>>
            HOLDERS = new IdentityHashMap<>();
    private static final Map<ServerPlayer, Sent> SENT = new IdentityHashMap<>();
    private static ServerPlayer initialRecipient;

    private ChunkTrackerIndex() {}

    public static List<ServerPlayer> players(ServerLevel level, int chunkX, int chunkZ) {
        var chunks = HOLDERS.get(level);
        if (chunks == null) return List.of();
        var players = chunks.get(ChunkPos.asLong(chunkX, chunkZ));
        return players == null ? List.of() : players;
    }

    public static boolean isTracked(ServerLevel level, int chunkX, int chunkZ) {
        if (!players(level, chunkX, chunkZ).isEmpty()) return true;
        // backport: a chunk no player's view contains may still be watched through a Sable
        // sub-level (its plot chunks are sent by Sable, not by PlayerChunkSender); remembered as
        // a candidate so reconcileForeign() picks it up once a player starts tracking it
        long key = ChunkPos.asLong(chunkX, chunkZ);
        CANDIDATES.computeIfAbsent(level, ignored -> new LongOpenHashSet()).add(key);
        return adopt(level, key);
    }

    // ---- backport: Sable sub-level plot chunks -------------------------------------------------
    // ntm-next learns who watches a chunk from PlayerChunkSender (MixinPlayerChunkSender). Sable
    // sends the chunks of a physics sub-level's plot itself, so machines on a flying build were
    // never "watched" and their GUIs, progress bars and buttons stopped syncing. Sable does make
    // the vanilla ChunkMap.getPlayers(chunk) return the players tracking the sub-level (NTM Reborn
    // synced through that and worked): players it returns for a chunk outside their own view are
    // registered here as if the chunk had been sent to them, and dropped when it stops.
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<ObjectArrayList<ServerPlayer>>>
            FOREIGN = new IdentityHashMap<>();
    private static final Map<ServerLevel, LongOpenHashSet> CANDIDATES = new IdentityHashMap<>();
    private static int reconcileClock;

    public static boolean isForeign(ServerLevel level, int chunkX, int chunkZ) {
        var chunks = FOREIGN.get(level);
        return chunks != null && chunks.containsKey(ChunkPos.asLong(chunkX, chunkZ));
    }

    private static boolean isForeign(ServerLevel level, long key, ServerPlayer player) {
        var chunks = FOREIGN.get(level);
        var players = chunks == null ? null : chunks.get(key);
        return players != null && players.contains(player);
    }

    private static List<ServerPlayer> foreignTrackers(ServerLevel level, long key) {
        int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
        List<ServerPlayer> out = new ObjectArrayList<>(2);
        for (ServerPlayer p : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(key), false)) {
            if (p.serverLevel() == level && !p.getChunkTrackingView().contains(cx, cz)) out.add(p);
        }
        return out;
    }

    private static boolean adopt(ServerLevel level, long key) {
        if (level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) == null)
            return false;
        List<ServerPlayer> trackers = foreignTrackers(level, key);
        if (trackers.isEmpty()) return false;
        var mine = FOREIGN.computeIfAbsent(level, ignored -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(key, ignored -> new ObjectArrayList<>(2));
        for (ServerPlayer p : trackers) {
            if (mine.contains(p)) continue;
            mine.add(p);
            onChunkSent(level, p, key);
        }
        var candidates = CANDIDATES.get(level);
        if (candidates != null) candidates.remove(key);
        return true;
    }

    /** Once a second: adopt newly tracked sub-level chunks, drop the ones no longer tracked. */
    public static void reconcileForeign() {
        if (++reconcileClock < 20) return;
        reconcileClock = 0;
        for (var levelEntry : new ArrayList<>(CANDIDATES.entrySet())) {
            ServerLevel level = levelEntry.getKey();
            var keys = levelEntry.getValue();
            for (long key : keys.toLongArray()) {
                if (!players(level, ChunkPos.getX(key), ChunkPos.getZ(key)).isEmpty()
                        || level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) == null) {
                    keys.remove(key);
                    continue;
                }
                adopt(level, key);
            }
            if (keys.isEmpty()) CANDIDATES.remove(level);
        }
        for (var levelEntry : new ArrayList<>(FOREIGN.entrySet())) {
            ServerLevel level = levelEntry.getKey();
            var chunks = levelEntry.getValue();
            for (long key : chunks.keySet().toLongArray()) {
                var mine = chunks.get(key);
                boolean loaded = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) != null;
                List<ServerPlayer> now = loaded ? foreignTrackers(level, key) : List.of();
                for (ServerPlayer p : new ArrayList<>(mine)) {
                    if (now.contains(p)) continue;
                    mine.remove(p);
                    onChunkDropped(level, p, key);
                }
                for (ServerPlayer p : now) {
                    if (mine.contains(p)) continue;
                    mine.add(p);
                    onChunkSent(level, p, key);
                }
                if (mine.isEmpty()) {
                    chunks.remove(key);
                    if (loaded) CANDIDATES.computeIfAbsent(level, ignored -> new LongOpenHashSet()).add(key);
                }
            }
            if (chunks.isEmpty()) FOREIGN.remove(level);
        }
    }
    // ---------------------------------------------------------------------------------------------

    public static void beginInitial(ServerPlayer player) {
        assert initialRecipient == null;
        initialRecipient = player;
    }

    public static void beginInitial(ServerPlayer player, long chunkKey) {
        beginInitial(player);
        PacketWire.dropSyncChunk(player.serverLevel(), player, chunkKey);
        Sent sent = SENT.computeIfAbsent(player, Sent::new);
        sent.dropReceipts(chunkKey);
    }

    public static void endInitial() {
        initialRecipient = null;
    }

    public static void initialSnapshot(BlockEntity be, UnitPayload payload, boolean shared) {
        if (initialRecipient == null) return;
        SENT.get(initialRecipient)
                .record(
                        be.getBlockPos().asLong(),
                        shared ? payload.revision() : -payload.revision());
    }

    static long revision(ServerPlayer player, long position) {
        Sent sent = SENT.get(player);
        return sent == null ? 0 : sent.revisions.get(position);
    }

    static void dispatched(ServerPlayer player, long position, long revision) {
        Sent sent = SENT.get(player);
        if (sent != null) sent.record(position, revision);
    }

    public static void snapshotSent(ServerPlayer player, BlockEntity be, long revision) {
        dispatched(player, be.getBlockPos().asLong(), revision);
    }

    public static boolean canRepair(ServerPlayer player, long chunkKey) {
        Sent sent = SENT.get(player);
        if (sent == null || !sent.chunks.contains(chunkKey)) return false;
        long tick = player.level().getGameTime();
        if (sent.repairTick != tick) {
            sent.repairTick = tick;
            sent.repairs = 0;
        }
        return sent.repairs++ < 32;
    }

    public static void initialBlob(BlockEntity be, long revision) {
        if (initialRecipient != null)
            blobDispatched(initialRecipient, be.getBlockPos().asLong(), revision);
    }

    static long blobRevision(ServerPlayer player, long position) {
        Sent sent = SENT.get(player);
        return sent == null || sent.blobs == null ? 0 : sent.blobs.get(position);
    }

    static void blobDispatched(ServerPlayer player, long position, long revision) {
        Sent sent = SENT.get(player);
        if (sent == null) return;
        if (sent.blobs == null) sent.blobs = new Long2LongOpenHashMap();
        if (sent.blobs.put(position, revision) == 0 && !sent.revisions.containsKey(position))
            sent.recordPosition(position);
    }

    public static void onChunkSent(ServerLevel level, ServerPlayer player, long chunkKey) {
        PacketWire.dropSyncChunk(level, player, chunkKey);
        Sent sent = SENT.computeIfAbsent(player, Sent::new);
        if (sent.chunks.add(chunkKey)) {
            var players =
                    HOLDERS.computeIfAbsent(level, ignored -> new Long2ObjectOpenHashMap<>())
                            .computeIfAbsent(chunkKey, ignored -> new ObjectArrayList<>(2));
            players.add(player);
            if (players.size() == 1) watching(level, chunkKey, true);
        }

        BeSyncTable.chunkSent(level, player, chunkKey);
    }

    public static void onChunkDropped(ServerLevel level, ServerPlayer player, long chunkKey) {
        Sent sent = SENT.get(player);
        if (sent == null || !sent.chunks.remove(chunkKey)) return;
        dropKnownChunk(level, player, chunkKey, sent);
        if (sent.chunks.isEmpty()) SENT.remove(player);
    }

    private static void dropKnownChunk(
            ServerLevel level, ServerPlayer player, long chunkKey, Sent sent) {
        sent.dropReceipts(chunkKey);
        PacketWire.dropSyncChunk(level, player, chunkKey);
        var chunks = HOLDERS.get(level);
        var players = chunks.get(chunkKey);
        players.remove(player);
        BeSyncTable.chunkDropped(level, player, chunkKey, players.isEmpty());
        if (players.isEmpty()) {
            chunks.remove(chunkKey);
            watching(level, chunkKey, false);
        }
        if (chunks.isEmpty()) HOLDERS.remove(level);
    }

    private static void watching(ServerLevel level, long key, boolean watched) {
        var chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
        if (chunk != null)
            for (var be : chunk.getBlockEntities().values()) {
                if (be instanceof Synced synced) synced.syncWatching(watched);
            }
    }

    static void pumpMovement() {
        SENT.forEach((player, sent) -> sent.move());
    }

    public static void forget(ServerPlayer player, ServerLevel level) {
        for (var chunks : FOREIGN.values()) for (var players : chunks.values()) players.remove(player);
        PacketWire.forget(player);
        Sent sent = SENT.remove(player);
        if (sent == null) return;
        var chunks = sent.chunks.iterator();
        while (chunks.hasNext()) dropKnownChunk(level, player, chunks.nextLong(), sent);
    }

    public static void respawn(ServerPlayer replacement) {
        ServerPlayer previous = null;
        for (ServerPlayer player : SENT.keySet()) {
            if (player != replacement && player.getUUID().equals(replacement.getUUID())) {
                previous = player;
                break;
            }
        }
        if (previous != null) forget(previous, previous.serverLevel());
    }

    public static void clear() {
        HOLDERS.clear();
        SENT.clear();
        FOREIGN.clear();
        CANDIDATES.clear();
        initialRecipient = null;
    }

    private static final class Sent implements LongConsumer {
        final ServerPlayer player;
        final LongOpenHashSet chunks = new LongOpenHashSet();
        final Long2LongOpenHashMap revisions = new Long2LongOpenHashMap();
        Long2LongOpenHashMap blobs;
        final Long2ObjectOpenHashMap<LongArrayList> positions = new Long2ObjectOpenHashMap<>();
        double x = Double.NaN, y, z;
        long repairTick = Long.MIN_VALUE;
        int repairs;

        Sent(ServerPlayer player) {
            this.player = player;
        }

        void dropReceipts(long chunkKey) {
            var members = positions.remove(chunkKey);
            if (members == null) return;
            for (int i = 0; i < members.size(); i++) {
                long pos = members.getLong(i);
                revisions.remove(pos);
                if (blobs != null) blobs.remove(pos);
            }
        }

        void record(long pos, long revision) {
            if (revisions.put(pos, revision) == 0 && (blobs == null || !blobs.containsKey(pos)))
                recordPosition(pos);
        }

        void recordPosition(long pos) {
            long chunk = ChunkPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4);
            positions.computeIfAbsent(chunk, ignored -> new LongArrayList()).add(pos);
        }

        void move() {
            double nextX = player.getX(), nextY = player.getY(), nextZ = player.getZ();
            if (x == nextX && y == nextY && z == nextZ) return;
            x = nextX;
            y = nextY;
            z = nextZ;
            if (PacketWire.extendedChunkCache()) {
                var held = chunks.iterator();
                while (held.hasNext()) {
                    long key = held.nextLong();

                    if (!player.getChunkTrackingView()
                                    .contains(ChunkPos.getX(key), ChunkPos.getZ(key))
                            && !isForeign(player.serverLevel(), key, player)) {
                        held.remove();
                        dropKnownChunk(player.serverLevel(), player, key, this);
                    } else accept(key);
                }
            } else chunks.forEach(this);
        }

        @Override
        public void accept(long chunkKey) {
            BeSyncTable.playerMoved(player.serverLevel(), player, chunkKey);
        }
    }
}
