// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package com.hbm.mixin.sable;

import com.hbm.client.particle.ParticleGasFlame;
import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Sable compat (not in ntm-next): Sable stops every particle that moves into a physics build's
 * blocks, whatever the particle's own physics say. Gas flames are born inside the burner's model
 * (the turbofan's nozzle sits in the build), so on a build they piled up at the nozzle instead of
 * leaving as a jet. Through Sable's own opt-out the flame ignores builds: no collision, no riding
 * along. Client only; applied only when Sable is present (SableMixinPlugin), compiled against
 * stubs (src/sableStubs).
 */
@Mixin(ParticleGasFlame.class)
public abstract class MixinSableGasFlameParticle implements ParticleSubLevelKickable {

    @Override
    public boolean sable$shouldCareAboutIntersectingSubLevels() {
        return false;
    }

    @Override
    public boolean sable$shouldKickFromTracking() {
        return true;
    }

    @Override
    public boolean sable$shouldCollideWithTrackingSubLevel() {
        return false;
    }
}
