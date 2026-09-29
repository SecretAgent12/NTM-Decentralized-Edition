// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

/** 26.x UVPair.pack/unpackU/unpackV: a (u, v) float pair packed into one long. */
public final class Uv {
    private Uv() {}

    public static long pack(float u, float v) {
        return (long) Float.floatToRawIntBits(u) << 32 | Float.floatToRawIntBits(v) & 0xFFFFFFFFL;
    }

    public static float unpackU(long packed) {
        return Float.intBitsToFloat((int) (packed >>> 32));
    }

    public static float unpackV(long packed) {
        return Float.intBitsToFloat((int) packed);
    }
}
