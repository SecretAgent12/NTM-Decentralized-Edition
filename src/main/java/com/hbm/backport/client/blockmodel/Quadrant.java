// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.mojang.math.OctahedralGroup;
import com.mojang.serialization.Codec;

/** 26.x com.mojang.math.Quadrant: a quarter turn (face UV rotation, block rotation step). */
public enum Quadrant {
    R0(0),
    R90(1),
    R180(2),
    R270(3);

    public static final Codec<Quadrant> CODEC = Codec.INT.xmap(Quadrant::parseJson, Quadrant::degrees);

    public final int shift;

    Quadrant(int shift) {
        this.shift = shift;
    }

    public int degrees() {
        return shift * 90;
    }

    public static Quadrant parseJson(int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 0 -> R0;
            case 90 -> R90;
            case 180 -> R180;
            case 270 -> R270;
            default -> throw new IllegalArgumentException("Invalid rotation " + degrees + " found, only -270/-180/-90/0/90/180/270 allowed");
        };
    }

    public static Quadrant fromDegrees(int degrees) {
        return parseJson(degrees);
    }

    public Quadrant rotateBy(Quadrant other) {
        return values()[(shift + other.shift) & 3];
    }

    public int rotateVertexIndex(int index) {
        return (index + shift) & 3;
    }

    /** The y rotation as a 1.21.1 octahedral group element (blockstate convention). */
    public OctahedralGroup asYRotation() {
        return shift == 0 ? OctahedralGroup.IDENTITY : com.hbm.backport.Octahedral.blockRot('y', degrees());
    }
}
