// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.misc;

import com.mojang.blaze3d.platform.NativeImage;

public final class NativeImages {

    private NativeImages() {}

    /**
     * 26.x {@code NativeImage.getPixel(x, y)} returns ARGB; 1.21.1 only has
     * {@code getPixelRGBA}, which returns the memory order as an int, i.e. ABGR.
     */
    public static int getPixel(NativeImage image, int x, int y) {
        int abgr = image.getPixelRGBA(x, y);
        return (abgr & 0xFF00FF00) | ((abgr >> 16) & 0xFF) | ((abgr & 0xFF) << 16);
    }
}
