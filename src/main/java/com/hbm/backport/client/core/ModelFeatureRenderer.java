// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 26.x {@code net.minecraft.client.renderer.feature.ModelFeatureRenderer}: the tree only uses its
 * nested {@link CrumblingOverlay}. 1.21.1 draws everything immediately, so there is no feature
 * renderer to back it.
 */
public final class ModelFeatureRenderer {
    private ModelFeatureRenderer() {}

    /**
     * Block-breaking overlay for a submitted model: destroy stage 0..9 and the camera-space pose the
     * decal texture is projected from (the 1.21.1 {@code SheetedDecalTextureGenerator} input).
     */
    public record CrumblingOverlay(int progress, PoseStack.Pose cameraPose) {}
}
