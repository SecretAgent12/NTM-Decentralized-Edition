// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import com.hbm.backport.client.particle.ParticleRenderType;
import net.minecraft.core.particles.ParticleTypes;
import com.hbm.backport.client.particle.ParticleCompat;

public class ParticleAshes extends ParticleCompat {

    public static final ParticleRenderType ASHES_GROUP = new ParticleRenderType("hbm:ashes", "HAS");

    private static int seedSeq;

    private final int id;
    private final float shade;
    private final float scale;
    private float rotationPitch, prevRotationPitch;

    public ParticleAshes(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);
        this.lifetime = 1200 + this.random.nextInt(20);
        this.scale = scale * 0.9F + this.random.nextFloat() * 0.2F;
        this.gravity = 0.01F;
        this.shade = this.random.nextFloat() * 0.1F + 0.1F;
        this.id = seedSeq++;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.yd -= this.gravity;
        this.prevRotationPitch = this.rotationPitch;

        if (!this.onGround) this.rotationPitch += 2F * ((this.id % 2) - 0.5F);

        this.xd *= 0.95D;
        this.yd *= 0.99D;
        this.zd *= 0.95D;

        boolean wasOnGround = this.onGround;
        // backport-fix: BF-005 clear vanilla's stoppedByCollision latch (ashes on ground a nuke removes
        // later hung in mid-air); zero the resting fall speed like 1.7.10 moveEntity did
        this.stoppedByCollision = false;
        this.move(this.xd, this.yd, this.zd);
        if (this.onGround) this.yd = 0D;
        if (!wasOnGround && this.onGround) this.rotationPitch = this.random.nextFloat() * 360F;

        if (this.id % 5 == 0 && this.onGround && this.random.nextInt(15) == 0) {
            this.level.addParticle(
                    ParticleTypes.SMOKE, this.x, this.y + 0.125D, this.z, 0D, 0.05D, 0D);
        }
    }

    public boolean settled() {
        return this.onGround;
    }

    public float shade() {
        return this.shade;
    }

    public float size() {
        return this.scale;
    }

    public float spin(float pt) {
        return this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * pt;
    }

    public float alpha(float pt) {
        float timeLeft = this.lifetime - (this.age + pt);
        return timeLeft < 40F ? timeLeft / 40F : 1F;
    }

    public int light(float pt) {
        return getLightCoords(pt);
    }

    public double interpX(float pt) {
        return this.xo + (this.x - this.xo) * pt;
    }

    public double interpY(float pt) {
        return this.yo + (this.y - this.yo) * pt;
    }

    public double interpZ(float pt) {
        return this.zo + (this.z - this.zo) * pt;
    }

    @Override
    public ParticleRenderType getGroup() {
        return ASHES_GROUP;
    }
}
