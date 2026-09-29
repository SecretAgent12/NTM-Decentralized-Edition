// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jspecify.annotations.Nullable;

/**
 * 26.x {@code net.minecraft.client.particle.ParticleRenderType}: a plain key naming the {@link
 * ParticleGroup} a particle belongs to ({@code record ParticleRenderType(String name, String ...)}).
 *
 * <p>backport: in 1.21.1 {@code ParticleRenderType} is the interface ParticleEngine sorts its particle
 * queues by, with {@code begin(Tesselator, TextureManager)} opening the batch. This key implements it:
 * every particle whose group is this key lands in one 1.21.1 queue, and {@link #begin} renders the whole
 * group itself (extract + submit via {@link ParticleBridge}) and returns {@code null}, so the engine
 * skips its per-particle {@code render(VertexConsumer, ...)} loop.
 */
public class ParticleRenderType implements net.minecraft.client.particle.ParticleRenderType {

    /** 26.x group of all {@link SingleQuadParticle}s (backport: split per layer, see {@link ParticleBridge}). */
    public static final ParticleRenderType SINGLE_QUADS = new ParticleRenderType("SINGLE_QUADS", "SQ");
    public static final ParticleRenderType ITEM_PICKUP = new ParticleRenderType("ITEM_PICKUP", "IP");
    public static final ParticleRenderType ELDER_GUARDIANS = new ParticleRenderType("ELDER_GUARDIANS", "EG");
    public static final ParticleRenderType NO_RENDER = new ParticleRenderType("NO_RENDER", "NR");

    private final String name;
    private final String shortName;

    // backport: unverified: the second component's name (the tree only passes short ids like "HXS")
    public ParticleRenderType(String name, String shortName) {
        this.name = name;
        this.shortName = shortName;
    }

    public ParticleRenderType(String name) {
        this(name, name);
    }

    public String name() {
        return name;
    }

    public String shortName() {
        return shortName;
    }

    @Override
    public @Nullable BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
        if (this != NO_RENDER) ParticleBridge.renderGroup(this);
        return null;
    }

    /** backport: groups render in the translucent particle pass (after translucent terrain). */
    @Override
    public boolean isTranslucent() {
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
}
