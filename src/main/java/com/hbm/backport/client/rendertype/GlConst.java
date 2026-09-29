// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

/**
 * 26.x {@code com.mojang.blaze3d.opengl.GlConst}: the GL constants plus the enum-to-GL conversions. backport:
 * extends 1.21.1 {@code com.mojang.blaze3d.platform.GlConst} (the constants) and adds the conversions for the
 * shimmed enums.
 */
public class GlConst extends com.mojang.blaze3d.platform.GlConst {
    public static int toGl(BlendFactor factor) {
        return factor.gl();
    }

    public static int toGl(BlendOp op) {
        return op.gl();
    }

    public static int toGl(CompareOp op) {
        return op.gl();
    }

    public static int toGl(com.mojang.blaze3d.vertex.VertexFormat.Mode mode) {
        return mode.asGLMode;
    }

    public static int toGl(PrimitiveTopology topology) {
        return topology.mode().asGLMode;
    }
}
