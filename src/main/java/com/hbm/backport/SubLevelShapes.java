// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import com.hbm.util.ChunkUtil;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import java.util.ArrayList;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sable compat (not in ntm-next): NTM blasts that eat a whole shape of the world at once (FLEIJA,
 * Solinium, balefire) do the same to the physics builds (Sable sub-levels) in it. The world engines
 * walk world columns, and a build's blocks live in a far-away plot, so the builds stayed untouched.
 *
 * <p>Each build in reach gets one pass on the server thread, sized by the build: every block of
 * its plot is placed where it is drawn in the world and asked {@link Shape#contains} with that
 * world point, so the cut follows the world shape whatever the build's heading. Uses the plot
 * bounds and ownership of {@link SubLevelNuke} (a too big build is skipped). Goes through Sable
 * Companion: without Sable nothing is found and nothing runs.
 */
public final class SubLevelShapes {

    private SubLevelShapes() {}

    /** A world shape, asked for the world point where a build block's centre is drawn. */
    @FunctionalInterface
    public interface Shape {
        boolean contains(double x, double y, double z);
    }

    /** What happens to a build block inside the shape. */
    @FunctionalInterface
    public interface CellAction {
        void apply(ServerLevel level, BlockPos plot, BlockState state, Vec3 world);
    }

    /** Removes every build block inside {@code shape}; {@code keep} blocks are skipped. */
    public static void clear(
            ServerLevel level, Vec3 center, double reach, Shape shape, @Nullable Predicate<BlockState> keep) {
        BlockState air = Blocks.AIR.defaultBlockState();
        forEach(
                level,
                center,
                reach,
                shape,
                (lvl, plot, state, world) -> {
                    if (keep != null && keep.test(state)) return;
                    lvl.setBlock(plot, air, Block.UPDATE_ALL);
                    ChunkUtil.dispatchRemovalHook(lvl, plot, state, air);
                });
    }

    /**
     * Runs {@code action} for every non-air build block inside {@code shape}, builds within {@code
     * reach} of {@code center} (a world point). The blocks are collected first and changed after,
     * so the action may remove or replace them.
     */
    public static void forEach(
            ServerLevel level, Vec3 center, double reach, Shape shape, CellAction action) {
        for (SubLevelAccess sub : SubLevelNuke.intersecting(level, center, reach)) {
            SubLevelNuke.Box box = SubLevelNuke.Box.of(sub);
            if (box == null) continue;
            SubLevelNuke.Owner owner = new SubLevelNuke.Owner(level, sub.getUniqueId());
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            ArrayList<BlockPos> hit = new ArrayList<>();
            ArrayList<Vec3> where = new ArrayList<>();
            int cells = box.sizeX() * box.sizeY() * box.sizeZ();
            if (cells <= 0) continue;
            for (int i = 0; i < cells; i++) {
                box.pos(i, pos);
                if (!owner.owns(pos)) continue;
                Vec3 world = sub.logicalPose().transformPosition(box.center(i));
                if (!shape.contains(world.x, world.y, world.z)) continue;
                if (level.getBlockState(pos).isAir()) continue;
                hit.add(pos.immutable());
                where.add(world);
            }
            for (int k = 0; k < hit.size(); k++) {
                BlockPos p = hit.get(k);
                BlockState state = level.getBlockState(p);
                if (!state.isAir()) action.apply(level, p, state, where.get(k));
            }
        }
    }
}
