// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.particle.ParticleProvider}: {@code createParticle} also receives the
 * engine's random source. backport: the 1.21.1 entry point forwards with the level's random.
 */
@FunctionalInterface
public interface ParticleProvider<T extends ParticleOptions>
        extends net.minecraft.client.particle.ParticleProvider<T> {

    @Nullable Particle createParticle(
            T options,
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            RandomSource random);

    @Override
    default @Nullable Particle createParticle(
            T options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        return createParticle(options, level, x, y, z, xd, yd, zd, level.getRandom());
    }
}
