// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.fluidmk2.FlushIndex;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.tileentity.PendingCoreInvalidation;
import com.hbm.uninos.graph.LevelNodeGraph;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class MixinChunkMapFullStatus {

    @Shadow @Final private ServerLevel level;

    // backport-fix: BF-037 chunks currently at FULL or above, so the "chunk became accessible" hooks
    // run exactly once per rise (see hbm$onRisingFullStatus)
    @Unique private final LongOpenHashSet hbm$accessible = new LongOpenHashSet();

    // backport-fix: BF-037 (same in ntm-next) ChunkHolder schedules one confirmation per status it
    // climbs through, and each new one cancels the previous; a chunk whose ticket jumps straight to
    // ticking (login, respawn, teleport, a dimension change) only reports BLOCK_TICKING or
    // ENTITY_TICKING, never FULL. Waiting for FULL alone skipped the graph wake-up:
    // cable nodes and segments parked or dropped while their chunk wasn't readable were never queued
    // again, so generators stopped feeding and machines stopped receiving until the world reloaded.
    // Now the hooks run on the first status at or above FULL and re-arm once the chunk drops below.
    @Inject(method = "onFullChunkStatusChange", at = @At("TAIL"))
    private void hbm$onRisingFullStatus(ChunkPos pos, FullChunkStatus status, CallbackInfo ci) {
        long key = pos.toLong();
        if (!status.isOrAfter(FullChunkStatus.FULL)) {
            hbm$accessible.remove(key);
            return;
        }
        if (!hbm$accessible.add(key)) return;
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) {
            hbm$accessible.remove(key);
            return;
        }
        AssembledMembers.onChunkFull(level, chunk);
        LevelNodeGraph.onChunkFull(level, chunk);
        FlushIndex.onChunkFull(level, chunk);
        PendingCoreInvalidation.onChunkFull(level, chunk);
    }
}
