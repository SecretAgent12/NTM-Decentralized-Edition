// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import com.hbm.capability.NtmContracts;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sable compat (not in ntm-next or 1.7.10): items and packages on NTM conveyors on physics builds
 * (Sable sub-levels). A conveyor object is an entity, and Sable moves every entity that appears in a
 * build's plot out into the world; there no belt was under it, so the item dropped off, landed on
 * the belt again and hopped along it (GitHub #39). Keeping it in the plot (Sable's
 * retain_in_sub_level tag) made it ride, but NTM's renderers draw it in plot coordinates, out of
 * sight.
 *
 * <p>So the object stays in the world, drawn as usual, and only looks at belts in the space of the
 * build under it: {@link Place} is where it is on that build. It keeps its place in the build's
 * coordinates from tick to tick, so a moving or turning build carries it along (its next point is
 * turned into the world with the build's current pose), the way drones follow builds. Without Sable
 * every place is a world place and nothing changes.
 */
public final class SubLevelConveyor {

    /** How far around a world point builds are looked for (a belt is under or around the object). */
    private static final double REACH = 0x1p-1;

    private SubLevelConveyor() {}

    /**
     * Where a conveyor object is: {@code local} in the space of {@code frame} (a build), or in the
     * world when {@code frame} is null.
     */
    public record Place(@Nullable SubLevelAccess frame, Vec3 local) {

        public BlockPos block() {
            return BlockPos.containing(local);
        }

        /** A point of this space, as drawn in the world. */
        public Vec3 toWorld(Vec3 point) {
            return frame == null ? point : frame.logicalPose().transformPosition(point);
        }

        /** The same space at the world point {@code at} (after a move). */
        public Place at(Vec3 at) {
            return frame == null
                    ? new Place(null, at)
                    : new Place(frame, frame.logicalPose().transformPositionInverse(at));
        }

        /** True when the object rides a build. */
        public boolean onBuild() {
            return frame != null;
        }
    }

    /**
     * The place of an object at world point {@code at}: its last place on a build ({@code last},
     * in that build's coordinates) while that build is still there, else a build with a belt or an
     * enterable block at that point, else the world.
     */
    public static Place find(ServerLevel level, Vec3 at, @Nullable Vec3 last) {
        if (last != null) {
            SubLevelAccess kept = SableCompanion.INSTANCE.getContaining(level, last);
            if (kept != null) return new Place(kept, last);
        }
        SubLevelAccess own = SableCompanion.INSTANCE.getContaining(level, at);
        if (own != null) return new Place(own, at);
        BoundingBox3d box =
                new BoundingBox3d(at.x - REACH, at.y - REACH, at.z - REACH, at.x + REACH, at.y + REACH, at.z + REACH);
        for (SubLevelAccess sub : SableCompanion.INSTANCE.getAllIntersecting(level, box)) {
            Vec3 local = sub.logicalPose().transformPositionInverse(at);
            BlockPos block = BlockPos.containing(local);
            if (NtmContracts.CONVEYOR_BELT.at(level, block) != null
                    || NtmContracts.ENTERABLE.at(level, block) != null) return new Place(sub, local);
        }
        return new Place(null, at);
    }
}
