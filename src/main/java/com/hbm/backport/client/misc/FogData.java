// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.misc;

import org.joml.Vector4f;

/**
 * 26.x {@code net.minecraft.client.renderer.fog.FogData}: the fog parameters of a frame.
 * backport: unverified: field set inferred from 1.21.6+ vanilla. On 1.21.1 it is filled from
 * and written back to NeoForge's ViewportEvent.RenderFog / ComputeFogColor ({@link SootFogEvents}).
 */
public class FogData {
    public float environmentalStart;
    public float renderDistanceStart;
    public float environmentalEnd;
    public float renderDistanceEnd;
    public float skyEnd;
    public float cloudEnd;
    public Vector4f color = new Vector4f();
}
