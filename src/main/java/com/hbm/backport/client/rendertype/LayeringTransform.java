// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import net.minecraft.client.renderer.RenderStateShard;

/**
 * 26.x {@code net.minecraft.client.renderer.rendertype.LayeringTransform}: a per-type model-view offset.
 * backport: wraps the 1.21.1 layering shard.
 */
public final class LayeringTransform {
    public static final LayeringTransform NO_LAYERING = new LayeringTransform(RenderStateShard.NO_LAYERING);
    public static final LayeringTransform VIEW_OFFSET_Z_LAYERING =
            new LayeringTransform(RenderStateShard.VIEW_OFFSET_Z_LAYERING);
    /** backport: 1.21.1 has no forward variant; the regular view offset is the nearest. */
    public static final LayeringTransform VIEW_OFFSET_Z_LAYERING_FORWARD = VIEW_OFFSET_Z_LAYERING;
    /** backport: 1.21.1 polygon offset layering (26.x moved it to the pipeline depth bias). */
    public static final LayeringTransform POLYGON_OFFSET_LAYERING =
            new LayeringTransform(RenderStateShard.POLYGON_OFFSET_LAYERING);

    private final RenderStateShard.LayeringStateShard shard;

    public LayeringTransform(RenderStateShard.LayeringStateShard shard) {
        this.shard = shard;
    }

    public RenderStateShard.LayeringStateShard shard() {
        return shard;
    }
}
