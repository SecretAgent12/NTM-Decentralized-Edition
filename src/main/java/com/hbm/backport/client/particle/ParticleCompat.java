// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;

/**
 * 26.x shape of {@code net.minecraft.client.particle.Particle} for tree classes that extend it
 * directly: the particle names its {@link ParticleGroup} key via {@link #getGroup()} and has no
 * per-particle render method (its group extracts it).
 *
 * <p>backport: 1.21.1 {@code getRenderType()} returns the group key (which renders the group, see
 * {@link ParticleRenderType#begin}); {@code render(VertexConsumer, ...)} is a no-op.
 */
public abstract class ParticleCompat extends Particle {

    protected ParticleCompat(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    protected ParticleCompat(
            ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
    }

    public abstract ParticleRenderType getGroup();

    /** 26.x name of {@code getLightColor}. */
    protected int getLightCoords(float partialTicks) {
        return super.getLightColor(partialTicks);
    }

    @Override
    protected int getLightColor(float partialTicks) {
        return getLightCoords(partialTicks);
    }

    @Override
    public net.minecraft.client.particle.ParticleRenderType getRenderType() {
        return getGroup();
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {}
}
