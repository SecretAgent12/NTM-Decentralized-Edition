// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.rendertype;

import net.minecraft.client.renderer.RenderType;

/**
 * 26.x {@code net.minecraft.client.renderer.chunk.ChunkSectionLayer} (SOLID, CUTOUT, TRANSLUCENT).
 * backport: 1.21.1 identifies chunk layers by their RenderType; {@link #renderType()} gives it and
 * {@link #of(RenderType)} maps back (CUTOUT_MIPPED and TRIPWIRE fold into CUTOUT / TRANSLUCENT).
 */
public enum ChunkSectionLayer {
    SOLID,
    CUTOUT,
    TRANSLUCENT;

    /**
     * The 1.21.1 chunk RenderType. backport: 26.x has one cutout layer; 1.21.1 CUTOUT (no mipmaps) keeps
     * thin cutout textures from eroding at distance.
     */
    public RenderType renderType() {
        return switch (this) {
            case SOLID -> RenderType.solid();
            case CUTOUT -> RenderType.cutout();
            case TRANSLUCENT -> RenderType.translucent();
        };
    }

    /** The 1.21.1 RenderType a moving (piston) block of this layer draws with. */
    public RenderType movingBlockRenderType() {
        return this == TRANSLUCENT ? RenderType.translucentMovingBlock() : renderType();
    }

    public String label() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static ChunkSectionLayer of(RenderType type) {
        if (type == RenderType.solid()) return SOLID;
        if (type == RenderType.translucent() || type == RenderType.tripwire()
                || type == RenderType.translucentMovingBlock()) return TRANSLUCENT;
        return CUTOUT;
    }
}
