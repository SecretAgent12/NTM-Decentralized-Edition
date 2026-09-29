// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

/**
 * NeoForge 26 net.neoforged.neoforge.client.model.quad.BakedColors: four per-vertex ARGB colours
 * multiplied into the quad's colour when it is drawn. 1.21.1 carries them in the vertex colour
 * element of BakedQuad#getVertices (the chunk and item renderers read existing colours).
 */
public final class BakedColors {

    public static final int WHITE = 0xFFFFFFFF;
    public static final BakedColors DEFAULT = new BakedColors(WHITE, WHITE, WHITE, WHITE);

    private final int c0, c1, c2, c3;

    private BakedColors(int c0, int c1, int c2, int c3) {
        this.c0 = c0;
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
    }

    public static BakedColors of(int color) {
        return color == WHITE ? DEFAULT : new BakedColors(color, color, color, color);
    }

    public static BakedColors of(int c0, int c1, int c2, int c3) {
        if ((c0 & c1 & c2 & c3) == WHITE) return DEFAULT;
        return new BakedColors(c0, c1, c2, c3);
    }

    public int color(int vertex) {
        return switch (vertex) {
            case 0 -> c0;
            case 1 -> c1;
            case 2 -> c2;
            default -> c3;
        };
    }

    public boolean isDefault() {
        return this == DEFAULT || (c0 & c1 & c2 & c3) == WHITE;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BakedColors b && b.c0 == c0 && b.c1 == c1 && b.c2 == c2 && b.c3 == c3;
    }

    @Override
    public int hashCode() {
        return ((c0 * 31 + c1) * 31 + c2) * 31 + c3;
    }
}
