// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * The part of the Sable compat that touches Sable's own physics API (not the bundled Companion).
 * Only reached when Sable is installed (callers check first); compiled against src/sableStubs.
 */
final class SablePhysics {

    private SablePhysics() {}

    /**
     * Gives {@code sub} a speed change along the world direction {@code dir} at its centre of mass.
     * {@code exposed}: the change is {@code dv / cbrt(mass)} (a blast: bigger builds catch more but
     * weigh more); otherwise {@code dv} itself (a pull, like gravity). Capped at {@code max} m/s.
     */
    static void push(SubLevelAccess sub, Vec3 dir, double dv, double max, boolean exposed) {
        if (!(sub instanceof ServerSubLevel server)) return;
        MassData mass = server.getMassTracker();
        if (mass == null || mass.getMass() <= 0) return;
        double m = mass.getMass();
        double speed = Math.min(exposed ? dv / Math.cbrt(m) : dv, max);
        if (speed <= 0) return;

        RigidBodyHandle handle = RigidBodyHandle.of(server);
        if (handle == null || !handle.isValid()) return;

        // Sable takes impulses in the build's own (plot) frame, as its explosion code does
        Vector3d local = sub.logicalPose().transformNormalInverse(new Vector3d(dir.x, dir.y, dir.z));
        // the pose's rotation point is the build's centre of mass in plot space: no spin
        Vector3dc com = sub.logicalPose().rotationPoint();
        handle.applyImpulseAtPoint(
                new Vec3(com.x(), com.y(), com.z()),
                new Vec3(local.x, local.y, local.z).scale(m * speed));
    }
}
