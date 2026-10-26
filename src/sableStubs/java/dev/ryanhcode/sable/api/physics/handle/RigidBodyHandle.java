// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package dev.ryanhcode.sable.api.physics.handle;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Compile-time stand-in for Sable's class of the same name (Sable 2.x, 1.21.1): only the methods
 * NTM calls. Not packaged; at run time the class comes from Sable itself. Speeds are world space.
 */
public class RigidBodyHandle {
    public static RigidBodyHandle of(ServerSubLevel subLevel) {
        throw new UnsupportedOperationException("stub");
    }

    public void applyImpulseAtPoint(Vec3 point, Vec3 impulse) {
        throw new UnsupportedOperationException("stub");
    }

    public Vector3d getLinearVelocity(Vector3d dest) {
        throw new UnsupportedOperationException("stub");
    }

    public Vector3d getAngularVelocity(Vector3d dest) {
        throw new UnsupportedOperationException("stub");
    }

    public void addLinearAndAngularVelocity(Vector3dc linear, Vector3dc angular) {
        throw new UnsupportedOperationException("stub");
    }

    public boolean isValid() {
        throw new UnsupportedOperationException("stub");
    }
}
