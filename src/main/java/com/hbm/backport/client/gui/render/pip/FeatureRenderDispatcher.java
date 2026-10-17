// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.gui.render.pip;

/**
 * 26.x {@code net.minecraft.client.renderer.feature.FeatureRenderDispatcher} as far as the GUI
 * picture-in-picture API passes it around ({@code PictureInPictureRenderer.prepare}). 1.21.1
 * renders submitted nodes immediately, so there is nothing to dispatch.
 */
public final class FeatureRenderDispatcher {
    public static final FeatureRenderDispatcher IMMEDIATE = new FeatureRenderDispatcher();

    private FeatureRenderDispatcher() {}
}
