// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

/**
 * 26.x {@code net.minecraft.client.renderer.state.level.SkyRenderState}: the sky values the tree
 * reads or adjusts. 1.21.1 computes them inline in LevelRenderer.renderSky; a hook there fills one,
 * lets the tree adjust it, and applies the result.
 * backport: unverified: 26.x's {@code skybox} (DimensionType.Skybox) is reduced to {@link #overworldSky}
 * (1.21.1 DimensionSpecialEffects.SkyType.NORMAL).
 */
public class SkyRenderState {
    public boolean overworldSky;
    public float sunAngle;
    public float rainBrightness;
    public int sunriseAndSunsetColor;
    public float starBrightness;
}
