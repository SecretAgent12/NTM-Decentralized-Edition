// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.gui.state.pip;

import com.hbm.backport.client.gui.state.ScreenArea;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState}: a
 * rectangle (x0, y0)-(x1, y1) in GUI coordinates whose content a {@code PictureInPictureRenderer}
 * renders in 3D at {@code scale} GUI pixels per unit.
 */
public interface PictureInPictureRenderState extends ScreenArea {

    int x0();

    int x1();

    int y0();

    int y1();

    float scale();

    @Nullable ScreenRectangle scissorArea();

    static @Nullable ScreenRectangle getBounds(
            int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle r = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
        return scissorArea != null ? scissorArea.intersection(r) : r;
    }
}
