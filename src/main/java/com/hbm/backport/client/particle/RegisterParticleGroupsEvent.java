// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import java.util.function.Function;
import net.minecraft.client.particle.ParticleEngine;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * NeoForge 26 {@code RegisterParticleGroupsEvent}: registers the {@link ParticleGroup} factory of a
 * {@link ParticleRenderType} key. backport: posted on the mod bus by {@link ParticleBridge#register}
 * during client setup; the factories are kept by {@link ParticleBridge}.
 */
public class RegisterParticleGroupsEvent extends Event implements IModBusEvent {

    public RegisterParticleGroupsEvent() {}

    public void register(
            ParticleRenderType key, Function<ParticleEngine, ParticleGroup<?>> factory) {
        ParticleBridge.registerGroup(key, factory);
    }
}
