// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

/** 26.x {@code com.mojang.blaze3d.platform.BlendFactor}; {@link #gl()} is the OpenGL enum 1.21.1 uses. */
public enum BlendFactor {
    CONSTANT_ALPHA(32771),
    CONSTANT_COLOR(32769),
    DST_ALPHA(772),
    DST_COLOR(774),
    ONE(1),
    ONE_MINUS_CONSTANT_ALPHA(32772),
    ONE_MINUS_CONSTANT_COLOR(32770),
    ONE_MINUS_DST_ALPHA(773),
    ONE_MINUS_DST_COLOR(775),
    ONE_MINUS_SRC_ALPHA(771),
    ONE_MINUS_SRC_COLOR(769),
    SRC_ALPHA(770),
    SRC_ALPHA_SATURATE(776),
    SRC_COLOR(768),
    ZERO(0);

    private final int gl;

    BlendFactor(int gl) {
        this.gl = gl;
    }

    public int gl() {
        return gl;
    }
}
