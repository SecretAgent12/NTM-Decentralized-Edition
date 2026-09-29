// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.hbm.backport.client.rendertype.ChunkSectionLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import org.jspecify.annotations.Nullable;

/**
 * Bridges 26.x per-quad ChunkSectionLayer to 1.21.1's per-model chunk RenderTypes, by constant
 * name so it does not depend on which layers the rendertype shim declares.
 */
public final class Layers {
    private Layers() {}

    private static final ChunkSectionLayer[] ALL = ChunkSectionLayer.values();
    private static final RenderType[] TYPES = new RenderType[ALL.length];

    static {
        for (ChunkSectionLayer layer : ALL) TYPES[layer.ordinal()] = chunkType(layer.name());
    }

    private static RenderType chunkType(String name) {
        return switch (name) {
            case "SOLID" -> RenderType.solid();
            case "CUTOUT_MIPPED" -> RenderType.cutoutMipped();
            case "CUTOUT" -> RenderType.cutout();
            case "TRIPWIRE" -> RenderType.tripwire();
            default -> RenderType.translucent();
        };
    }

    public static ChunkSectionLayer[] all() {
        return ALL;
    }

    /** The 1.21.1 chunk RenderType drawing this layer. */
    public static RenderType renderType(ChunkSectionLayer layer) {
        return TYPES[layer.ordinal()];
    }

    /** The layer drawn by a 1.21.1 chunk RenderType, or null for a non-chunk type. */
    public static @Nullable ChunkSectionLayer layerOrNull(RenderType type) {
        for (int i = 0; i < TYPES.length; i++) if (TYPES[i] == type) return ALL[i];
        return null;
    }

    public static ChunkSectionLayer layer(RenderType type) {
        ChunkSectionLayer layer = layerOrNull(type);
        if (layer != null) return layer;
        return type == Sheets.translucentItemSheet() || type == Sheets.translucentCullBlockSheet()
                ? byName("TRANSLUCENT")
                : byName("CUTOUT");
    }

    public static ChunkSectionLayer byName(String name) {
        for (ChunkSectionLayer layer : ALL) if (layer.name().equals(name)) return layer;
        return ALL[0];
    }

    public static ChunkSectionLayer solid() {
        return byName("SOLID");
    }

    public static ChunkSectionLayer cutout() {
        return byName("CUTOUT");
    }

    public static ChunkSectionLayer translucent() {
        return byName("TRANSLUCENT");
    }

    public static boolean isTranslucent(ChunkSectionLayer layer) {
        return layer.name().equals("TRANSLUCENT");
    }

    /** 26.x ChunkSectionLayer.byTransparency. */
    public static ChunkSectionLayer byTransparency(Transparency transparency) {
        if (transparency.hasTranslucent()) return translucent();
        if (transparency.hasTransparent()) return cutout();
        return solid();
    }

    /** The item RenderType 26.x picks for a quad of this transparency (sheet atlas types). */
    public static RenderType itemType(Transparency transparency) {
        return transparency.hasTranslucent() ? Sheets.translucentItemSheet() : Sheets.cutoutBlockSheet();
    }
}
