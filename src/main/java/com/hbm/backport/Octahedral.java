// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import com.mojang.math.OctahedralGroup;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;

/**
 * 26.x OctahedralGroup helpers on 1.21.1.
 *
 * <p>{@link #blockRot}: 26.x OctahedralGroup.BLOCK_ROT_{X,Y,Z}_{90,180,270}, the quarter turns of the
 * block model rotations. 1.21.1 names its elements differently (ROT_90_X_NEG, ...); this picks the
 * element that is that rotation, in the convention of 1.21.1's BlockModelRotation (a blockstate
 * "x": 90 rotates by -90 degrees). Unverified against 26.x itself.
 *
 * <p>{@link #matrix}: the element's 3x3 matrix. Use it instead of 1.21.1's
 * {@code OctahedralGroup.transformation()}, which is wrong for every element that permutes axes:
 * {@code SymmetricGroup3} starts its permutation matrix from {@code new Matrix3f()} (the identity,
 * not zero), so e.g. ROT_90_X_NEG comes out as {@code I + P}. The matrix here is built from
 * {@code rotate(Direction)}, which 1.21.1 computes correctly.
 */
public final class Octahedral {

    private static final Matrix3f[] MATRICES = new Matrix3f[OctahedralGroup.values().length];

    static {
        for (OctahedralGroup g : OctahedralGroup.values()) {
            Matrix3f m = new Matrix3f().zero();
            setColumn(m, 0, g.rotate(Direction.EAST));
            setColumn(m, 1, g.rotate(Direction.UP));
            setColumn(m, 2, g.rotate(Direction.SOUTH));
            MATRICES[g.ordinal()] = m;
        }
    }

    private Octahedral() {}

    private static void setColumn(Matrix3f m, int column, Direction d) {
        m.setColumn(column, d.getStepX(), d.getStepY(), d.getStepZ());
    }

    /** The element's matrix (a copy; see the class comment for why not {@code transformation()}). */
    public static Matrix3f matrix(OctahedralGroup group) {
        return new Matrix3f(MATRICES[group.ordinal()]);
    }

    public static OctahedralGroup blockRot(char axis, int degrees) {
        float a = (float) Math.toRadians(-degrees);
        Matrix3f want = switch (axis) {
            case 'x' -> new Matrix3f().rotationX(a);
            case 'y' -> new Matrix3f().rotationY(a);
            default -> new Matrix3f().rotationZ(a);
        };
        for (OctahedralGroup g : OctahedralGroup.values()) {
            if (same(MATRICES[g.ordinal()], want)) return g;
        }
        throw new IllegalStateException("no octahedral element for " + axis + degrees);
    }

    private static boolean same(Matrix3f a, Matrix3f b) {
        for (int c = 0; c < 3; c++) {
            for (int r = 0; r < 3; r++) {
                if (Math.round(a.get(c, r)) != Math.round(b.get(c, r))) return false;
            }
        }
        return true;
    }
}
