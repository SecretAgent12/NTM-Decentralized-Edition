// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.blockmodel;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * 26.x com.mojang.blaze3d.platform.Transparency: whether a sprite has fully transparent texels
 * (cutout) and/or partially transparent ones (translucent). 1.21.1 sprites do not carry it, so
 * {@link #of(TextureAtlasSprite)} scans the sprite's original image once (cached per contents).
 */
public record Transparency(boolean hasTransparent, boolean hasTranslucent) {

    public static final Transparency NONE = new Transparency(false, false);
    public static final Transparency TRANSPARENT = new Transparency(true, false);
    public static final Transparency TRANSLUCENT = new Transparency(true, true);

    private static final Map<SpriteContents, Transparency> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    public boolean isOpaque() {
        return !hasTransparent && !hasTranslucent;
    }

    public Transparency or(Transparency other) {
        return of(hasTransparent || other.hasTransparent, hasTranslucent || other.hasTranslucent);
    }

    public static Transparency of(boolean transparent, boolean translucent) {
        if (translucent) return transparent ? TRANSLUCENT : new Transparency(false, true);
        return transparent ? TRANSPARENT : NONE;
    }

    /** 26.x TextureAtlasSprite#transparency(). */
    public static Transparency of(TextureAtlasSprite sprite) {
        SpriteContents contents = sprite.contents();
        Transparency cached = CACHE.get(contents);
        if (cached != null) return cached;
        Transparency computed = scan(contents);
        CACHE.put(contents, computed);
        return computed;
    }

    private static Transparency scan(SpriteContents contents) {
        NativeImage image;
        try {
            image = contents.getOriginalImage();
        } catch (RuntimeException e) {
            return NONE;
        }
        if (image == null) return NONE;
        boolean transparent = false, translucent = false;
        try {
            int w = image.getWidth(), h = image.getHeight();
            for (int y = 0; y < h && !(transparent && translucent); y++) {
                for (int x = 0; x < w; x++) {
                    int alpha = image.getPixelRGBA(x, y) >>> 24;
                    if (alpha == 0) transparent = true;
                    else if (alpha != 255) translucent = true;
                }
            }
        } catch (RuntimeException e) {
            // backport: image already closed (atlas reloaded) - treat as opaque
            return NONE;
        }
        return of(transparent, translucent);
    }
}
