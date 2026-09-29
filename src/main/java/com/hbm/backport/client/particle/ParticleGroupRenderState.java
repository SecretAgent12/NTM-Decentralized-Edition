// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import com.hbm.backport.client.core.CameraRenderState;
import com.hbm.backport.client.core.SubmitNodeCollector;

/**
 * 26.x {@code net.minecraft.client.renderer.state.level.ParticleGroupRenderState}: what a {@link
 * ParticleGroup} extracted for one frame, submitted afterwards. backport: extract and submit run back
 * to back inside the 1.21.1 particle render call ({@link ParticleBridge}).
 */
public interface ParticleGroupRenderState {

    void submit(SubmitNodeCollector collector, CameraRenderState camera);

    default void clear() {}
}
