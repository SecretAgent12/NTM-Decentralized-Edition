// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;

/**
 * Sable compat (not in ntm-next or 1.7.10): designated targets on physics builds (Sable
 * sub-levels). A designator keeps only X and Z; on a build that is a column of its plot, millions
 * of blocks away. Turning it into the world needs a height too, and any height but the build's own
 * moves the point sideways as soon as the build tilts (a build is rarely level: its centre of mass,
 * the ground it rests on). So the height is taken from the build: its top block in that column,
 * the way a missile in the world comes down on the ground there; an empty column takes the build's
 * centre.
 *
 * <p>Designators turn a picked block into the world at once ({@link #designated}), so a target is
 * where the build was when it was marked; {@link #column} still serves designators marked before
 * that and coordinates typed in by hand.
 *
 * <p>Also used for everything measured from a launcher to its target (custom missile scatter,
 * Soyuz cargo fuel): both ends in the world. Goes through Sable Companion (bundled): without Sable
 * every column is a world column and comes back unchanged.
 */
public final class SubLevelTargets {

    /** How far above and below a build's centre its column is searched for the top block. */
    private static final int COLUMN_REACH = 0x40;

    private SubLevelTargets() {}

    /**
     * Where the designated column {@code x, z} is in the world: on a build, its top block there (see
     * the class doc); a world column at height {@code y}, as it is.
     */
    public static Vec3 column(Level level, int x, int z, double y) {
        Vec3 flat = new Vec3(x + 0.5, y, z + 0.5);
        // plots are whole chunk columns: the build is found by the chunk, whatever the height
        SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(level, new ChunkPos(x >> 4, z >> 4));
        // not found: the plain projection BF-019 used, a no-op for a world column
        if (sub == null) return SubLevelSpace.toWorld(level, flat.x, flat.y, flat.z);

        Vector3dc centre = sub.logicalPose().rotationPoint();
        int mid = Mth.floor(centre.y());
        double top = centre.y() + 2.610304E-7;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dy = COLUMN_REACH; dy >= -COLUMN_REACH; dy--) {
            at.set(x, mid + dy, z);
            if (level.getBlockState(at).isAir()) continue;
            top = at.getY() + 1;
            break;
        }
        return sub.logicalPose().transformPosition(new Vec3(flat.x, top, flat.z));
    }

    /**
     * The block a designator picked, as a world position. Designators send things to coordinates,
     * they don't lock on: a block on a build is turned into the world where the build is right now,
     * and stays there when the build flies off (as in the world, a target that moves is missed). A
     * world block comes back as it is.
     */
    public static BlockPos designated(Level level, BlockPos pos) {
        SubLevelAccess sub = SableCompanion.INSTANCE.getContaining(level, pos);
        if (sub == null) return pos;
        return BlockPos.containing(sub.logicalPose().transformPosition(Vec3.atCenterOf(pos)));
    }
}
