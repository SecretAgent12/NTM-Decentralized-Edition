// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;

/**
 * 26.x {@code net.minecraft.client.particle.BaseAshSmokeParticle}. backport: body of the 1.21.1 class
 * on top of the 26.x-shaped {@link SingleQuadParticle}; default layer as 1.21.1's PARTICLE_SHEET_OPAQUE.
 */
public class BaseAshSmokeParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    protected BaseAshSmokeParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            float xSeedMultiplier,
            float ySpeedMultiplier,
            float zSpeedMultiplier,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            float quadSizeMultiplier,
            SpriteSet sprites,
            float rColMultiplier,
            int lifetime,
            float gravity,
            boolean hasPhysics) {
        super(level, x, y, z, 0.0, 0.0, 0.0, null);
        this.friction = 0.96F;
        this.gravity = gravity;
        this.speedUpWhenYMotionIsBlocked = true;
        this.sprites = sprites;
        this.xd *= xSeedMultiplier;
        this.yd *= ySpeedMultiplier;
        this.zd *= zSpeedMultiplier;
        this.xd += xSpeed;
        this.yd += ySpeed;
        this.zd += zSpeed;
        float f = level.random.nextFloat() * rColMultiplier;
        this.rCol = f;
        this.gCol = f;
        this.bCol = f;
        this.quadSize *= 0.75F * quadSizeMultiplier;
        this.lifetime =
                (int) ((double) lifetime / ((double) level.random.nextFloat() * 0.8 + 0.2)
                        * (double) quadSizeMultiplier);
        this.lifetime = Math.max(this.lifetime, 1);
        this.setSpriteFromAge(sprites);
        this.hasPhysics = hasPhysics;
    }

    @Override
    protected Layer getLayer() {
        return Layer.OPAQUE;
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return this.quadSize
                * Mth.clamp(((float) this.age + partialTicks) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);
    }
}
