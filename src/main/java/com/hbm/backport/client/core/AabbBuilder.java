// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import net.minecraft.world.phys.AABB;
import org.joml.Vector3fc;

/** 26.x {@code AABB.Builder}: grows a box around included points. */
public final class AabbBuilder {
    private float minX = Float.POSITIVE_INFINITY;
    private float minY = Float.POSITIVE_INFINITY;
    private float minZ = Float.POSITIVE_INFINITY;
    private float maxX = Float.NEGATIVE_INFINITY;
    private float maxY = Float.NEGATIVE_INFINITY;
    private float maxZ = Float.NEGATIVE_INFINITY;

    public void include(Vector3fc point) {
        include(point.x(), point.y(), point.z());
    }

    public void include(float x, float y, float z) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
    }

    public AABB build() {
        if (minX > maxX) return new AABB(0, 0, 0, 0, 0, 0);
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
