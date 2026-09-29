// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

/** 26.x {@code com.mojang.blaze3d.platform.BlendOp}; {@link #gl()} is the OpenGL blend equation. */
public enum BlendOp {
    ADD(32774),
    SUBTRACT(32778),
    REVERSE_SUBTRACT(32779),
    MIN(32775),
    MAX(32776);

    private final int gl;

    BlendOp(int gl) {
        this.gl = gl;
    }

    public int gl() {
        return gl;
    }
}
