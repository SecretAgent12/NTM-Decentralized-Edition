// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sable compat (not in ntm-next): wires and pipes stretched between the world and physics builds
 * (Sable sub-levels), or between two builds. Pylon wires and pipe anchors link absolute block
 * positions; a block on a build sits in a plot millions of blocks away, so lengths, the wiring
 * readout and the drawn wire came out in plot space. Here every end is taken where it is drawn in
 * the world, and brought back into the frame of the block that draws it. Both ends in the same
 * space (world-world, or the same build) keep the plain arithmetic they always had.
 *
 * <p>Goes through Sable Companion (bundled): without Sable every position is a world position and
 * nothing changes. Works on both sides (the client asks its own level).
 */
public final class SubLevelLinks {

    private SubLevelLinks() {}

    /** The build {@code pos} lies on, or null for the world. */
    public static @Nullable SubLevelAccess frameOf(Level level, BlockPos pos) {
        return SableCompanion.INSTANCE.getContaining(level, pos);
    }

    /** True when the two blocks sit in different spaces (world and build, or two builds). */
    public static boolean crosses(Level level, BlockPos a, BlockPos b) {
        SubLevelAccess fa = frameOf(level, a), fb = frameOf(level, b);
        if (fa == null || fb == null) return fa != fb;
        return !Objects.equals(fa.getUniqueId(), fb.getUniqueId());
    }

    /** Where the point {@code p} (in the space of the block it belongs to) is drawn in the world. */
    public static Vec3 toWorld(Level level, Vec3 p) {
        SubLevelAccess frame = SableCompanion.INSTANCE.getContaining(level, p);
        return frame == null ? p : frame.logicalPose().transformPosition(p);
    }

    /** The distance between two points as drawn in the world, each given in its own space. */
    public static double worldDistance(Level level, Vec3 a, Vec3 b) {
        return Math.sqrt(toWorld(level, a).distanceToSqr(toWorld(level, b)));
    }

    /**
     * The point {@code peerPoint} (in the space of block {@code peer}) relative to block {@code self},
     * in the frame {@code self} is drawn in: what a renderer at {@code self} needs to reach it.
     */
    public static Vec3 relative(Level level, BlockPos self, BlockPos peer, Vec3 peerPoint) {
        if (!crosses(level, self, peer))
            return peerPoint.subtract(self.getX(), self.getY(), self.getZ());
        Vec3 world = toWorld(level, peerPoint);
        SubLevelAccess mine = frameOf(level, self);
        Vec3 local = mine == null ? world : mine.logicalPose().transformPositionInverse(world);
        return local.subtract(self.getX(), self.getY(), self.getZ());
    }
}
