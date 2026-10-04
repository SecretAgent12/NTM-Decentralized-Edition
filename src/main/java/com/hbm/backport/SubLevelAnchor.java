// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * backport-fix: BF-041 — lets a digging machine on a physics build (excavator, mining laser) work
 * the ground under the build.
 *
 * <p>A build's blocks live in a far-away plot of the same level, so the machine's own scan only
 * ever finds the build and then empty plot air down to the bottom of the world. While the build
 * stands still and level, the anchor locks the world block under the machine and {@link #resolve}
 * sends every scanned position that is empty on the build to the world block at the same offset
 * from that lock: the hull under the machine goes first, then the ground. The mapping is a plain
 * offset, so the shaft stays straight and axis-aligned in the world whatever the build's heading.
 *
 * <p>If the build moves (more than {@link #MAX_STEP} per tick, or drifts more than {@link
 * #MAX_DRIFT} from the lock) or tilts past {@link #MAX_TILT_DEG}, the lock is dropped and the
 * machine is expected to shut down.
 */
public final class SubLevelAnchor {

    /** Not on a build: the machine works as usual. */
    public static final int OFF = 0;
    /** On a build that stands still and level: the machine may work the world. */
    public static final int STEADY = 1;
    /** On a build that moves (or the ground under it isn't loaded). */
    public static final int MOVING = 2;
    /** On a build tilted past {@link #MAX_TILT_DEG}. */
    public static final int TILTED = 3;

    public static final double MAX_STEP = 0.02D;
    public static final double MAX_DRIFT = 0.5D;
    public static final double MAX_TILT_DEG = 1D;
    private static final double MIN_UP_Y = Math.cos(Math.toRadians(MAX_TILT_DEG));

    private @Nullable Vec3 center, east, south;
    private Vec3 axisX = new Vec3(1, 0, 0), axisY = new Vec3(0, 1, 0), axisZ = new Vec3(0, 0, 1);
    private @Nullable Vec3 lockCenter;
    private @Nullable BlockPos lock;

    /**
     * Samples the build's pose around {@code pos} (the machine's own block) and returns {@link
     * #OFF}, {@link #STEADY}, {@link #MOVING} or {@link #TILTED}. Call once per server tick.
     */
    public int check(Level level, BlockPos pos) {
        double x = pos.getX() + 0.5D, y = pos.getY() + 0.5D, z = pos.getZ() + 0.5D;
        if (!SubLevelSpace.inSubLevel(level, x, z)) {
            forget();
            return OFF;
        }

        Vec3 c = SubLevelSpace.toWorld(level, x, y, z);
        Vec3 e = SubLevelSpace.toWorld(level, x + 1D, y, z);
        Vec3 s = SubLevelSpace.toWorld(level, x, y, z + 1D);
        Vec3 u = SubLevelSpace.toWorld(level, x, y + 1D, z).subtract(c);

        // the first sample (just loaded) has nothing to compare with: trust it for one tick
        boolean moving =
                center != null
                        && (c.distanceToSqr(center) > MAX_STEP * MAX_STEP
                                || e.distanceToSqr(east) > MAX_STEP * MAX_STEP
                                || s.distanceToSqr(south) > MAX_STEP * MAX_STEP);
        if (lockCenter != null && c.distanceToSqr(lockCenter) > MAX_DRIFT * MAX_DRIFT)
            moving = true;

        center = c;
        east = e;
        south = s;
        axisX = e.subtract(c);
        axisY = u;
        axisZ = s.subtract(c);

        if (moving) {
            unlock();
            return MOVING;
        }
        if (u.lengthSqr() == 0 || u.normalize().y < MIN_UP_Y) {
            unlock();
            return TILTED;
        }
        if (!level.isLoaded(BlockPos.containing(c))) {
            unlock();
            return MOVING;
        }
        return STEADY;
    }

    /** Locks the world block under the machine (keeps an existing lock). Only after STEADY. */
    public BlockPos lock() {
        if (lock == null && center != null) {
            lockCenter = center;
            lock = BlockPos.containing(center);
        }
        return lock;
    }

    public void unlock() {
        lock = null;
        lockCenter = null;
    }

    private void forget() {
        unlock();
        center = east = south = null;
    }

    public boolean locked() {
        return lock != null;
    }

    /**
     * Where to act for the scanned plot position {@code plot} of a machine at {@code origin}: the
     * build's own block if there is one, otherwise the world block at the same offset from the
     * lock. Without a lock (not on a build, or not steady) it's {@code plot} itself.
     */
    public BlockPos resolve(Level level, BlockPos origin, BlockPos plot) {
        if (lock == null) return plot;
        BlockState own = level.getBlockState(plot);
        if (!own.isAir()) return plot;
        return lock.offset(
                plot.getX() - origin.getX(), plot.getY() - origin.getY(), plot.getZ() - origin.getZ());
    }

    /** The world block at {@code plot}'s offset from {@code origin}, or null without a lock. */
    public @Nullable BlockPos project(BlockPos origin, BlockPos plot) {
        if (lock == null) return null;
        return lock.offset(
                plot.getX() - origin.getX(), plot.getY() - origin.getY(), plot.getZ() - origin.getZ());
    }

    /** World Y of plot height {@code plotY} for a machine at {@code origin}, while locked. */
    public int worldY(BlockPos origin, int plotY) {
        return lock == null ? plotY : lock.getY() + plotY - origin.getY();
    }

    /**
     * The plot position that is drawn where the world point {@code world} is, for a machine at
     * {@code origin} (inverse of the build's pose, from the last {@link #check}). Used to aim
     * plot-space visuals (laser beams) at world blocks.
     */
    public Vec3 toPlot(BlockPos origin, Vec3 world) {
        if (center == null) return world;
        Vec3 d = world.subtract(center);
        return new Vec3(
                origin.getX() + 0.5D + d.dot(axisX) / axisX.lengthSqr(),
                origin.getY() + 0.5D + d.dot(axisY) / axisY.lengthSqr(),
                origin.getZ() + 0.5D + d.dot(axisZ) / axisZ.lengthSqr());
    }
}
