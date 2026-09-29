// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;

/** 26.x ScreenRectangle members missing in 1.21.1. */
public final class GuiBounds {

    private GuiBounds() {}

    /** 26.x {@code ScreenRectangle.transformMaxBounds(Matrix3x2fc)}: the axis-aligned box of the posed rectangle. */
    public static ScreenRectangle transformMaxBounds(ScreenRectangle r, Matrix3x2fc pose) {
        Vector2f a = pose.transformPosition(r.left(), r.top(), new Vector2f());
        Vector2f b = pose.transformPosition(r.right(), r.top(), new Vector2f());
        Vector2f c = pose.transformPosition(r.left(), r.bottom(), new Vector2f());
        Vector2f d = pose.transformPosition(r.right(), r.bottom(), new Vector2f());
        float minX = Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x));
        float minY = Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y));
        float maxX = Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x));
        float maxY = Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y));
        int x0 = (int) Math.floor(minX);
        int y0 = (int) Math.floor(minY);
        return new ScreenRectangle(x0, y0, (int) Math.ceil(maxX) - x0, (int) Math.ceil(maxY) - y0);
    }
}
