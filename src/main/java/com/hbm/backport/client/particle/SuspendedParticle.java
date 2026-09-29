// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * 26.x {@code net.minecraft.client.particle.SuspendedParticle} (sprite passed in instead of a
 * SpriteSet). backport: body of the 1.21.1 class on top of the 26.x-shaped {@link SingleQuadParticle}.
 */
public class SuspendedParticle extends SingleQuadParticle {

    protected SuspendedParticle(
            ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y - 0.125, z, sprite);
        this.setSize(0.01F, 0.01F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
        this.lifetime = (int) (16.0 / (Math.random() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
    }

    protected SuspendedParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            TextureAtlasSprite sprite) {
        super(level, x, y - 0.125, z, xd, yd, zd, sprite);
        this.setSize(0.01F, 0.01F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.6F);
        this.lifetime = (int) (16.0 / (Math.random() * 0.8 + 0.2));
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.gravity = 0.0F;
    }

    @Override
    protected Layer getLayer() {
        return Layer.OPAQUE;
    }
}
