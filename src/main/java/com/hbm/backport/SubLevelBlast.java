// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-050 — lets an NTM explosion that runs in the world reach the physics builds
 * (Sable sub-levels) around it.
 *
 * <p>A build's blocks live in a far-away plot of the same level, so NTM's explosion rays, which
 * march through world coordinates, only ever see empty air where a build hangs. Sable fixes this
 * for vanilla's {@code Explosion} only. Here an explosion collects the builds whose world bounds
 * reach its radius once, and each ray step that finds air in the world asks {@link #plotBlock}
 * whether a build's block is drawn at that point; the ray then treats that plot block like a world
 * block (resistance, destruction). With no build nearby {@link #isEmpty} is true and nothing else
 * runs.
 */
public final class SubLevelBlast {

    private static final SubLevelBlast NONE = new SubLevelBlast(new SubLevelAccess[0]);

    private final SubLevelAccess[] subs;
    private final Vector3d scratch = new Vector3d();
    private final BlockPos.MutableBlockPos plot = new BlockPos.MutableBlockPos();

    private SubLevelBlast(SubLevelAccess[] subs) {
        this.subs = subs;
    }

    /** The builds whose world bounds come within {@code reach} of {@code (x, y, z)}. */
    public static SubLevelBlast around(Level level, double x, double y, double z, double reach) {
        BoundingBox3d box =
                new BoundingBox3d(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach);
        ArrayList<SubLevelAccess> found = new ArrayList<>();
        for (SubLevelAccess sub : SableCompanion.INSTANCE.getAllIntersecting(level, box))
            found.add(sub);
        return found.isEmpty() ? NONE : new SubLevelBlast(found.toArray(new SubLevelAccess[0]));
    }

    public boolean isEmpty() {
        return subs.length == 0;
    }

    /**
     * The non-air build block drawn at the world point {@code (x, y, z)}, or null. The returned
     * position is reused by the next call: copy it ({@code immutable()}) before keeping it.
     */
    public @Nullable BlockPos plotBlock(Level level, double x, double y, double z) {
        for (SubLevelAccess sub : subs) {
            if (!sub.boundingBox().contains(x, y, z)) continue;
            sub.logicalPose().transformPositionInverse(scratch.set(x, y, z), scratch);
            plot.set(Mth.floor(scratch.x), Mth.floor(scratch.y), Mth.floor(scratch.z));
            if (!level.getBlockState(plot).isAir()) return plot;
        }
        return null;
    }

    /**
     * Where an explosion at {@code (x, y, z)} really is: a point inside a build's plot is moved to
     * the world point where it's drawn, the way Sable does it for vanilla explosions. Then the
     * explosion hits the world around the build too, and the build itself through {@link
     * #plotBlock}.
     */
    public static Vec3 origin(Level level, double x, double y, double z) {
        if (!SubLevelSpace.inSubLevel(level, x, z)) return new Vec3(x, y, z);
        return SubLevelSpace.toWorld(level, x, y, z);
    }
}
