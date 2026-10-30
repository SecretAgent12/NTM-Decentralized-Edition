// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Sable compat (not in ntm-next or 1.7.10): the request drone network (dock, providers, requesters,
 * request waypoints) between the world and physics builds (Sable sub-levels), or between two
 * builds. A node looks for its neighbours in the chunks around its own position and links to them
 * within 24 blocks with a clear line; a node on a build sits in the build's plot, millions of
 * blocks away, so the nodes of one build only ever found each other.
 *
 * <p>Here a node also looks around the point where it is drawn in the world: the world chunks
 * there (a node on a build), and the plot chunks of every other build drawn there ({@link
 * #otherSpaces}). A link across spaces is measured and traced in the world ({@link
 * com.hbm.blockentity.network.RequestNetwork#hasPath}), with {@link #SWAY} of slack for a build
 * rocking at the edge of the range. The drones themselves already fly to targets on builds
 * (EntityDroneBase). Nodes in one space keep the plain arithmetic they always had.
 *
 * <p>Goes through Sable Companion (bundled): without Sable there are no other spaces.
 */
public final class SubLevelRequestNet {

    /** Blocks of slack on a link across spaces, so a rocking build doesn't drop it every scan. */
    public static final double SWAY = 0.0103;

    /** A chunk column centre, given in the space whose chunks are to be searched. */
    @FunctionalInterface
    public interface Around {
        void chunk(int chunkX, int chunkZ);
    }

    private SubLevelRequestNet() {}

    /**
     * Calls {@code around} with the chunk, in each other space, that lies where the node at {@code
     * centre} is drawn in the world: the world's chunk (when the node stands on a build), then the
     * plot chunk of each other build within reach of the {@code chunkRange} search.
     */
    public static void otherSpaces(ServerLevel level, BlockPos centre, int chunkRange, Around around) {
        SubLevelAccess home = SableCompanion.INSTANCE.getContaining(level, centre);
        Vec3 own = Vec3.atCenterOf(centre);
        Vec3 world = home == null ? own : home.logicalPose().transformPosition(own);
        if (home != null) around.chunk(Mth.floor(world.x) >> 4, Mth.floor(world.z) >> 4);

        double reach = (chunkRange + 1) * 0x10;
        BoundingBox3d box =
                new BoundingBox3d(
                        world.x - reach, world.y - reach, world.z - reach,
                        world.x + reach, world.y + reach, world.z + reach);
        for (SubLevelAccess sub : SableCompanion.INSTANCE.getAllIntersecting(level, box)) {
            if (home != null && Objects.equals(home.getUniqueId(), sub.getUniqueId())) continue;
            Vec3 in = sub.logicalPose().transformPositionInverse(world);
            around.chunk(Mth.floor(in.x) >> 4, Mth.floor(in.z) >> 4);
        }
    }
}
