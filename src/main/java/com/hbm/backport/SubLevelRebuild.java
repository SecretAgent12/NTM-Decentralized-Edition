// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * backport-fix: BF-074 — assembled multiblocks (PWR, ICF) on physics contraptions.
 *
 * <p>An assembled multiblock is a controller plus member blocks turned into shells. Which shell
 * belongs to which controller lives in the per-chunk multiblock core index, and the controller
 * keeps absolute positions (ports, fuel rods, the members' bounds) in its block entity. Neither
 * survives Sable: a move into or out of a sub-level (SubLevelAssemblyHelper.moveBlocks) copies
 * blocks and block entity NBT only, possibly rotated, and a plot chunk is saved by Sable without
 * NTM's chunk data, so after a reload the index of a contraption is empty. The reactor then kept
 * working with ports nobody could reach, pushed its hot coolant out of the old world positions,
 * melted down at the place it was built and could no longer be disassembled.
 *
 * <p>The controller block rebuilds itself from the shells instead: it walks them the way the
 * assembly did, claims them again and recomputes everything it keeps. This runs after a Sable
 * move (SubLevelMoves) and when the controller loads on a contraption ({@link #onLoad}); on the
 * ground nothing changes.
 */
public final class SubLevelRebuild {

    private SubLevelRebuild() {}

    /** A controller block that can rebuild its assembled structure from the blocks in place. */
    public interface Owner {

        /** Re-claim the members of the assembled controller at {@code pos}, or take it apart. */
        void rebuild(ServerLevel level, BlockPos pos);

        /**
         * backport-fix: BF-077 every block of the assembled structure at {@code pos} (controller
         * included), found the way the assembly walks it; empty when it isn't assembled.
         */
        java.util.List<BlockPos> structure(ServerLevel level, BlockPos pos);
    }

    /**
     * Called when the controller's block entity joins a level (clearRemoved). On a contraption it
     * rebuilds once the current work is done, when the whole plot (and the block entity's NBT) is
     * loaded.
     */
    public static void onLoad(BlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = be.getBlockPos().immutable();
        if (!SubLevelSpace.inSubLevel(level, pos.getX(), pos.getZ())) return;
        MinecraftServer server = level.getServer();
        server.tell(
                server.wrapRunnable(
                        () -> {
                            if (be.isRemoved() || level.getBlockEntity(pos) != be) return;
                            if (level.getBlockState(pos).getBlock() instanceof Owner owner)
                                owner.rebuild(level, pos);
                        }));
    }
}
