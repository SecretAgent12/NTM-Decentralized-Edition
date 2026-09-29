// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.client.particle.ParticleBridge;
import com.hbm.backport.client.particle.ParticleEngineAccess;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CLIENT-ONLY. Hosts the 26.x particle group pipeline in 1.21.1's ParticleEngine (see {@link
 * ParticleBridge}): exposes the per-render-type queues, records the frame at the head of {@code
 * render}, and forwards particle admission / tick / clear to the 26.x lifecycle hooks.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineBridgeMixin implements ParticleEngineAccess {

    // backport: key type left generic on purpose: this file is outside the shim package, so the
    // pipeline would redirect a vanilla ParticleRenderType reference to the 26.x shim
    @Shadow @Final private Map<Object, Queue<Particle>> particles;

    @Override
    public Map<?, Queue<Particle>> hbm$particles() {
        return particles;
    }

    @Inject(
            method =
                    "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
            at = @At("HEAD"))
    private void hbm$beginFrame(
            LightTexture lightTexture,
            Camera camera,
            float partialTick,
            Frustum frustum,
            Predicate<?> filter,
            CallbackInfo ci) {
        ParticleBridge.beginFrame(
                (ParticleEngine) (Object) this, lightTexture, camera, partialTick, frustum);
    }

    @ModifyArg(
            method = "tick",
            at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
    private Object hbm$admitted(Object particle) {
        ParticleBridge.onAdmitted((Particle) particle);
        return particle;
    }

    @Inject(method = "tickParticle", at = @At("TAIL"))
    private void hbm$ticked(Particle particle, CallbackInfo ci) {
        ParticleBridge.onTicked(particle);
    }

    @Inject(method = "clearParticles", at = @At("HEAD"))
    private void hbm$cleared(CallbackInfo ci) {
        ParticleBridge.onCleared(particles);
    }
}
