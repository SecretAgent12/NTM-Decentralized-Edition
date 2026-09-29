// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import net.minecraft.util.Mth;

/**
 * 26.x net.minecraft.util.ARGB (packed 0xAARRGGBB colours), the functions the tree
 * uses. 1.21.1's FastColor.ARGB32 has only part of them.
 */
public final class ARGB {

    private ARGB() {}

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & 0xFF;
    }

    public static int green(int color) {
        return color >> 8 & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    public static int color(int red, int green, int blue) {
        return color(255, red, green, blue);
    }

    /** The alpha channel replaced, RGB kept. */
    public static int color(int alpha, int rgb) {
        return (alpha & 0xFF) << 24 | rgb & 0xFFFFFF;
    }

    public static int as8BitChannel(float value) {
        return Mth.floor(value * 255.0F);
    }

    public static int colorFromFloat(float alpha, float red, float green, float blue) {
        return color(as8BitChannel(alpha), as8BitChannel(red), as8BitChannel(green), as8BitChannel(blue));
    }

    public static float alphaFloat(int color) {
        return alpha(color) / 255.0F;
    }

    public static float redFloat(int color) {
        return red(color) / 255.0F;
    }

    public static float greenFloat(int color) {
        return green(color) / 255.0F;
    }

    public static float blueFloat(int color) {
        return blue(color) / 255.0F;
    }

    public static int opaque(int color) {
        return color | 0xFF000000;
    }

    public static int transparent(int color) {
        return color & 0x00FFFFFF;
    }

    public static int white(float alpha) {
        return color(as8BitChannel(alpha), 0xFFFFFF);
    }

    public static int white(int alpha) {
        return color(alpha, 0xFFFFFF);
    }

    public static int black(float alpha) {
        return color(as8BitChannel(alpha), 0);
    }

    public static int black(int alpha) {
        return color(alpha, 0);
    }

    public static int multiplyAlpha(int color, float factor) {
        return color(as8BitChannel(alphaFloat(color) * factor), color);
    }

    public static int scaleRGB(int color, float red, float green, float blue) {
        return color(alpha(color),
                Mth.clamp((int) (red(color) * red), 0, 255),
                Mth.clamp((int) (green(color) * green), 0, 255),
                Mth.clamp((int) (blue(color) * blue), 0, 255));
    }

    public static int scaleRGB(int color, float scale) {
        return scaleRGB(color, scale, scale, scale);
    }

    public static int multiply(int a, int b) {
        return color(alpha(a) * alpha(b) / 255, red(a) * red(b) / 255, green(a) * green(b) / 255,
                blue(a) * blue(b) / 255);
    }

    public static int lerp(float delta, int from, int to) {
        return color(Mth.lerpInt(delta, alpha(from), alpha(to)), Mth.lerpInt(delta, red(from), red(to)),
                Mth.lerpInt(delta, green(from), green(to)), Mth.lerpInt(delta, blue(from), blue(to)));
    }
}
