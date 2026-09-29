// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import java.util.Optional;

/**
 * 26.x {@code com.mojang.blaze3d.pipeline.ColorTargetState}: blending and color write mask of a pipeline.
 * backport: 1.21.1 write masks are all-or-nothing for color, so any non-zero mask writes color.
 */
public record ColorTargetState(Optional<BlendFunction> blendFunction, int writeMask) {
    public static final int WRITE_RED = 1;
    public static final int WRITE_GREEN = 2;
    public static final int WRITE_BLUE = 4;
    public static final int WRITE_ALPHA = 8;
    public static final int WRITE_COLOR = WRITE_RED | WRITE_GREEN | WRITE_BLUE;
    public static final int WRITE_ALL = WRITE_COLOR | WRITE_ALPHA;
    public static final int WRITE_NONE = 0;

    public static final ColorTargetState DEFAULT = new ColorTargetState(Optional.empty(), WRITE_ALL);

    public ColorTargetState(BlendFunction blendFunction) {
        this(Optional.of(blendFunction), WRITE_ALL);
    }

    public ColorTargetState(BlendFunction blendFunction, int writeMask) {
        this(Optional.of(blendFunction), writeMask);
    }

    public boolean writeColor() {
        return writeMask != 0;
    }
}
