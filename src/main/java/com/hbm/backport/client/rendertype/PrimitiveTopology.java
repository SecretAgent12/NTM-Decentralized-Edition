// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * 26.x {@code com.mojang.blaze3d.PrimitiveTopology}: the primitive assembly of a pipeline. backport: 1.21.1
 * keeps it on the RenderType as a {@link VertexFormat.Mode}; {@link #mode()} gives that mode.
 */
public enum PrimitiveTopology {
    LINES(VertexFormat.Mode.LINES),
    LINE_STRIP(VertexFormat.Mode.LINE_STRIP),
    DEBUG_LINES(VertexFormat.Mode.DEBUG_LINES),
    DEBUG_LINE_STRIP(VertexFormat.Mode.DEBUG_LINE_STRIP),
    TRIANGLES(VertexFormat.Mode.TRIANGLES),
    TRIANGLE_STRIP(VertexFormat.Mode.TRIANGLE_STRIP),
    TRIANGLE_FAN(VertexFormat.Mode.TRIANGLE_FAN),
    QUADS(VertexFormat.Mode.QUADS);

    private final VertexFormat.Mode mode;

    PrimitiveTopology(VertexFormat.Mode mode) {
        this.mode = mode;
    }

    /** The 1.21.1 draw mode. */
    public VertexFormat.Mode mode() {
        return mode;
    }

    public boolean isLines() {
        return this == LINES || this == LINE_STRIP || this == DEBUG_LINES || this == DEBUG_LINE_STRIP;
    }

    public static PrimitiveTopology of(VertexFormat.Mode mode) {
        for (PrimitiveTopology t : values()) if (t.mode == mode) return t;
        throw new IllegalArgumentException(String.valueOf(mode));
    }
}
