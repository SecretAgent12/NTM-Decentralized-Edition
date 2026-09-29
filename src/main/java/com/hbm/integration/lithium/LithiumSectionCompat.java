// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.lithium;

import net.caffeinemc.mods.lithium.common.tracking.block.ChunkSectionChangeCallback;
import net.caffeinemc.mods.lithium.common.world.LithiumData;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Keeps Lithium's per-section block tracking valid when NTM swaps a whole chunk section
 * (SectionSnapshot.publish: explosions, nukes, bulk block edits).
 *
 * <p>backport: rewritten for Lithium 0.15.x (1.21.1). ntm-next targets Lithium 0.25 (26.x), where
 * the counters live in a LithiumSectionData object that can be copied and the change listener can
 * be moved onto the new section. On 1.21.1 those are private mixin fields of LevelChunkSection:
 * <ul>
 *   <li>copy: nothing to do - the 1.21.1 LevelChunkSection(states, biomes) constructor that
 *       backport.Sections.copy and ChunkUtil use runs recalcBlockCounts(), where Lithium
 *       recounts its flag counters from the palette, and later edits of the copy go through
 *       setBlockState, which Lithium tracks;</li>
 *   <li>replaced: the listener stays on the old section object, so it is dropped the way Lithium
 *       drops it for an unloaded chunk (ChunkSectionChangeCallback.init): removed from the level's
 *       map and invalidated. Its block-change trackers then stop trusting this section and
 *       re-register on the new one the next time they look.</li>
 * </ul>
 * Without this, Lithium's block-change trackers kept their "nothing changed" cache for a section
 * that a nuke had just rewritten.
 */
public final class LithiumSectionCompat {
    private LithiumSectionCompat() {}

    public static void copy(LevelChunkSection source, LevelChunkSection destination) {
        // counters are recomputed by the LevelChunkSection constructor on 1.21.1 (see above)
    }

    public static void replaced(ServerLevel level, long sectionPos, LevelChunkSection replacement) {
        if (!((Object) level instanceof LithiumData data)) return;
        ChunkSectionChangeCallback callback =
                data.lithium$getData().chunkSectionChangeCallbacks().remove(sectionPos);
        if (callback != null) callback.onChunkSectionInvalidated(SectionPos.of(sectionPos));
    }
}
