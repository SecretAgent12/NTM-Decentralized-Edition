// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.ChunkFullSafetyNet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport-fix: BF-037 (DE 2026-10-04, commit deff40fb). NEXT 1.0.0's version of this mixin reacted to FULL only.
 * ChunkHolder schedules one confirmation per status it climbs through, and each new one cancels the previous; a
 * chunk whose ticket jumps straight to ticking (login, respawn, teleport, a dimension change) only reports
 * BLOCK_TICKING or ENTITY_TICKING, never FULL, so the graph wake-up was skipped: cable nodes and segments parked
 * or dropped while their chunk wasn't readable were never queued again, and generators stopped feeding until the
 * world reloaded. DE's tracking reacts to the first status at or above FULL and re-arms once the chunk drops below.
 *
 * <p>Since NEXT 1.0.3 (2026-10-12) {@code MixinChunkMap} publishes chunks the same way and is the primary path;
 * this mixin now feeds {@link ChunkFullSafetyNet}, which publishes a risen chunk one tick later if NEXT's path
 * missed it.
 */
@Mixin(ChunkMap.class)
public abstract class MixinChunkMapFullStatus {

    @Shadow @Final private ServerLevel level;

    // backport-fix: BF-037 chunks currently at FULL or above, so a rise is reported exactly once
    @Unique private final LongOpenHashSet hbm$accessible = new LongOpenHashSet();

    @Inject(method = "onFullChunkStatusChange", at = @At("TAIL"))
    private void hbm$onRisingFullStatus(ChunkPos pos, FullChunkStatus status, CallbackInfo ci) {
        long key = pos.toLong();
        if (!status.isOrAfter(FullChunkStatus.FULL)) {
            hbm$accessible.remove(key);
            ChunkFullSafetyNet.fell(level, pos);
            return;
        }
        if (!hbm$accessible.add(key)) return;
        ChunkFullSafetyNet.rose(level, pos);
    }
}
