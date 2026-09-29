// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 26.x InterpolationHandler: client-side smoothing of an entity towards the last
 * position the server sent. 1.21.1 does the same through Entity.lerpTo, overridden
 * per entity (boats, minecarts); EntityCompat routes lerpTo into this handler for
 * every entity whose getInterpolation() returns one.
 *
 * Steps towards the target like 1.21.1's lerpPositionAndRotationStep: each tick
 * moves 1/remaining of the way. 26.x additionally carries the entity's own
 * per-tick motion into the target; that refinement is not reproduced.
 */
public class InterpolationHandler {

    private final Entity entity;
    private int length;
    private int steps;
    private Vec3 position = Vec3.ZERO;
    private float yRot;
    private float xRot;

    public InterpolationHandler(Entity entity) {
        this(entity, 3);
    }

    public InterpolationHandler(Entity entity, int length) {
        this.entity = entity;
        this.length = length;
    }

    public void setInterpolationLength(int length) {
        this.length = length;
    }

    public int getInterpolationLength() {
        return length;
    }

    public void interpolateTo(Vec3 position, float yRot, float xRot) {
        if (length == 0) {
            entity.moveTo(position.x, position.y, position.z, yRot, xRot);
            cancel();
            return;
        }
        this.position = position;
        this.yRot = yRot;
        this.xRot = xRot;
        this.steps = length;
    }

    public boolean hasActiveInterpolation() {
        return steps > 0;
    }

    public void interpolate() {
        if (!hasActiveInterpolation()) return;
        double f = 1.0 / steps;
        entity.setPos(
                Mth.lerp(f, entity.getX(), position.x),
                Mth.lerp(f, entity.getY(), position.y),
                Mth.lerp(f, entity.getZ(), position.z));
        entity.setYRot(entity.getYRot() + (float) (Mth.wrapDegrees(yRot - entity.getYRot()) * f));
        entity.setXRot(entity.getXRot() + (float) ((xRot - entity.getXRot()) * f));
        steps--;
    }

    public void cancel() {
        steps = 0;
    }

    public Vec3 position() {
        return position;
    }

    public float yRot() {
        return yRot;
    }

    public float xRot() {
        return xRot;
    }
}
