// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

/** 26.x {@code com.mojang.blaze3d.platform.CompareOp}; {@link #gl()} is the OpenGL depth function. */
public enum CompareOp {
    ALWAYS_PASS(519),
    LESS_THAN(513),
    LESS_THAN_OR_EQUAL(515),
    EQUAL(514),
    NOT_EQUAL(517),
    GREATER_THAN_OR_EQUAL(518),
    GREATER_THAN(516),
    NEVER_PASS(512);

    private final int gl;

    CompareOp(int gl) {
        this.gl = gl;
    }

    public int gl() {
        return gl;
    }
}
