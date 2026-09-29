// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import java.util.Comparator;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;

/**
 * 26.x TicketType (timeout + flags, registered) over 1.21.1's generic
 * net.minecraft.server.level.TicketType (name + comparator + lifespan, not registered).
 *
 * 1.21.1 has no ticket flags: every region ticket loads its chunks, and the ticket level
 * decides how far they tick (33 - radius, as 26.x's addTicketWithRadius computes it).
 * FLAG_PERSIST has no counterpart -- 1.21.1 does not save tickets with the world.
 */
public final class TicketType {

    public static final long NO_TIMEOUT = 0L;
    public static final int FLAG_PERSIST = 1;
    public static final int FLAG_LOADING = 2;
    public static final int FLAG_SIMULATION = 4;
    public static final int FLAG_KEEP_DIMENSION_ACTIVE = 8;
    public static final int FLAG_CAN_EXPIRE_IF_UNLOADED = 16;

    private final long timeout;
    private final int flags;
    private String name = "hbm:ticket";
    private net.minecraft.server.level.TicketType<ChunkPos> vanilla;

    public TicketType(long timeout, int flags) {
        this.timeout = timeout;
        this.flags = flags;
    }

    /** Called by the registrar: 1.21.1 ticket types carry their name. */
    public TicketType named(String name) {
        this.name = name;
        return this;
    }

    public long timeout() {
        return timeout;
    }

    public int flags() {
        return flags;
    }

    public synchronized net.minecraft.server.level.TicketType<ChunkPos> vanilla() {
        if (vanilla == null) {
            vanilla = net.minecraft.server.level.TicketType.create(
                    name, Comparator.comparingLong(ChunkPos::toLong), (int) timeout);
        }
        return vanilla;
    }

    /** 26.x ServerChunkCache.addTicketWithRadius. */
    public static void addTicketWithRadius(ServerChunkCache cache, TicketType type, ChunkPos pos, int radius) {
        cache.addRegionTicket(type.vanilla(), pos, radius, pos);
    }

    /** 26.x ServerChunkCache.removeTicketWithRadius. */
    public static void removeTicketWithRadius(ServerChunkCache cache, TicketType type, ChunkPos pos, int radius) {
        cache.removeRegionTicket(type.vanilla(), pos, radius, pos);
    }
}
