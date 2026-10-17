// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.backport.client.particle;

import java.util.Map;
import java.util.Queue;
import net.minecraft.client.particle.Particle;

/** backport: implemented on 1.21.1 ParticleEngine by {@code ParticleEngineBridgeMixin}. */
public interface ParticleEngineAccess {
    Map<?, Queue<Particle>> hbm$particles();
}
