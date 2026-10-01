// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * World space vs Sable sub-level space. Blocks of a physics build (Create: Aeronautics) live in a
 * "plot" millions of blocks away and are drawn at the build's pose, so any position taken from a
 * block entity on a build is a plot position. Whatever leaves the build (missiles, projectiles)
 * has to start from the matching world position instead.
 *
 * <p>Goes through Sable Companion (bundled): without Sable installed it returns the position as
 * is.
 */
public final class SubLevelSpace {

    private SubLevelSpace() {}

    /** The world position of {@code (x, y, z)}: unchanged unless it lies inside a sub-level plot. */
    public static Vec3 toWorld(Level level, double x, double y, double z) {
        Vector3d out =
                SableCompanion.INSTANCE.projectOutOfSubLevel(
                        level, new Vector3d(x, y, z), new Vector3d());
        return new Vec3(out.x, out.y, out.z);
    }

    /** True when {@code (x, z)} lies inside a sub-level plot. */
    public static boolean inSubLevel(Level level, double x, double z) {
        return SableCompanion.INSTANCE.getContaining(level, x, z) != null;
    }
}
