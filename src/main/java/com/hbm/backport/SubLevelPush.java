// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Sable compat (not in ntm-next): NTM blasts and black holes move the physics builds (Sable
 * sub-levels) around them, not only their blocks. Sable does it for vanilla explosions only (an
 * impulse per block the ray finds); NTM's own engines get one push per build here.
 *
 * <ul>
 *   <li>{@link #blast}: away from the blast centre, falling off with (1 - d / reach)^2, where d
 *       is the distance to the nearest point of the build's bounds. The speed it gives is {@link
 *       #BLAST_DV} x power x falloff / cbrt(mass): a bigger build takes more of the blast (its
 *       face grows as mass^(2/3)) but is moved less. Capped at {@link #MAX_DV}.
 *   <li>{@link #pull}: towards a point, a fixed speed change per tick, like the black hole's pull
 *       on entities (mass doesn't matter, as with gravity).
 * </ul>
 *
 * Positions found through Sable Companion (bundled, a no-op without Sable); the push itself goes
 * through {@link SablePhysics}, loaded only when Sable is installed.
 */
public final class SubLevelPush {

    /** m/s per unit of blast power at the centre, for a one-block build. Tuned in game. */
    public static final double BLAST_DV = 8.0061;

    /** m/s: no blast throws a build faster than this. */
    public static final double MAX_DV = 40.0;

    private static final boolean SABLE =
            net.neoforged.fml.ModList.get() != null && net.neoforged.fml.ModList.get().isLoaded("sable");

    private SubLevelPush() {}

    /** Pushes the builds within {@code reach} of the world point {@code center} away from it. */
    public static void blast(Level level, Vec3 center, double reach, double power) {
        if (!SABLE || !(level instanceof ServerLevel server) || reach <= 0 || power <= 0) return;
        for (SubLevelAccess sub : around(server, center, reach)) {
            BoundingBox3dc b = sub.boundingBox();
            double nx = Math.max(b.minX(), Math.min(center.x, b.maxX()));
            double ny = Math.max(b.minY(), Math.min(center.y, b.maxY()));
            double nz = Math.max(b.minZ(), Math.min(center.z, b.maxZ()));
            double d = Math.sqrt(center.distanceToSqr(nx, ny, nz));
            if (d >= reach) continue;
            double f = 1 - d / reach;
            Vec3 mid = new Vec3((b.minX() + b.maxX()) / 2, (b.minY() + b.maxY()) / 2, (b.minZ() + b.maxZ()) / 2);
            Vec3 away = mid.subtract(center);
            if (away.lengthSqr() < 2.610303E-6) away = new Vec3(0, 1, 0);
            SablePhysics.push(sub, away.normalize(), BLAST_DV * power * f * f, MAX_DV, true);
        }
    }

    /** Changes the speed of the builds within {@code range} of {@code center} by {@code dv} m/s towards it. */
    public static void pull(Level level, Vec3 center, double range, double dv) {
        if (!SABLE || !(level instanceof ServerLevel server) || range <= 0 || dv <= 0) return;
        for (SubLevelAccess sub : around(server, center, range)) {
            BoundingBox3dc b = sub.boundingBox();
            Vec3 mid = new Vec3((b.minX() + b.maxX()) / 2, (b.minY() + b.maxY()) / 2, (b.minZ() + b.maxZ()) / 2);
            Vec3 to = center.subtract(mid);
            if (to.lengthSqr() > range * range || to.lengthSqr() < 1e-6) continue;
            SablePhysics.push(sub, to.normalize(), dv, MAX_DV, false);
        }
    }

    private static Iterable<? extends SubLevelAccess> around(ServerLevel level, Vec3 c, double r) {
        return SableCompanion.INSTANCE.getAllIntersecting(
                level, new BoundingBox3d(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r));
    }
}
