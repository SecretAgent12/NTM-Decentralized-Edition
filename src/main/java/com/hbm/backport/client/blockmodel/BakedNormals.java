// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * NeoForge 26 net.neoforged.neoforge.client.model.quad.BakedNormals: four per-vertex normals,
 * each packed like the 1.21.1 vertex normal element (x | y << 8 | z << 16, signed bytes). A
 * packed value of 0 means "unspecified" (the renderer uses the face normal).
 */
public final class BakedNormals {

    public static final int UNSPECIFIED = 0;
    public static final BakedNormals EMPTY = new BakedNormals(0, 0, 0, 0);

    private final int n0, n1, n2, n3;

    private BakedNormals(int n0, int n1, int n2, int n3) {
        this.n0 = n0;
        this.n1 = n1;
        this.n2 = n2;
        this.n3 = n3;
    }

    public static BakedNormals of(int n0, int n1, int n2, int n3) {
        if ((n0 | n1 | n2 | n3) == 0) return EMPTY;
        return new BakedNormals(n0, n1, n2, n3);
    }

    public static BakedNormals of(int normal) {
        return of(normal, normal, normal, normal);
    }

    public int normal(int vertex) {
        return switch (vertex) {
            case 0 -> n0;
            case 1 -> n1;
            case 2 -> n2;
            default -> n3;
        };
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public static boolean isUnspecified(int packed) {
        return packed == UNSPECIFIED;
    }

    public static int pack(float x, float y, float z) {
        return (((byte) (x * 127F)) & 0xFF) | (((byte) (y * 127F)) & 0xFF) << 8 | (((byte) (z * 127F)) & 0xFF) << 16;
    }

    public static int pack(Vector3fc normal) {
        return pack(normal.x(), normal.y(), normal.z());
    }

    public static float unpackX(int packed) {
        return ((byte) packed) / 127F;
    }

    public static float unpackY(int packed) {
        return ((byte) (packed >> 8)) / 127F;
    }

    public static float unpackZ(int packed) {
        return ((byte) (packed >> 16)) / 127F;
    }

    public static Vector3f unpack(int packed, Vector3f into) {
        return into.set(unpackX(packed), unpackY(packed), unpackZ(packed));
    }

    public static int computeQuadNormal(Vector3fc p0, Vector3fc p1, Vector3fc p2, Vector3fc p3) {
        Vector3f a = new Vector3f(p2).sub(p0);
        Vector3f b = new Vector3f(p3).sub(p1);
        Vector3f n = a.cross(b);
        if (n.lengthSquared() < 1.0E-12F) {
            n = new Vector3f(p1).sub(p0).cross(new Vector3f(p2).sub(p0));
        }
        if (n.lengthSquared() < 1.0E-12F) return pack(0F, 1F, 0F);
        n.normalize();
        return pack(n);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BakedNormals b && b.n0 == n0 && b.n1 == n1 && b.n2 == n2 && b.n3 == n3;
    }

    @Override
    public int hashCode() {
        return ((n0 * 31 + n1) * 31 + n2) * 31 + n3;
    }
}
