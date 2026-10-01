// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectable;
import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.client.ClientEffects;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.util.ChunkUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import com.hbm.backport.storage.ValueInput;
import com.hbm.backport.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityMissileAntiBallistic extends EntityThrowableInterp
        implements IRadarDetectable, IRadarDetectableNT {

    public static double baseSpeed = 1.5D;

    public Entity tracking;
    public double velocity;
    protected int activationTimer;

    // backport: an airship (Sable sub-level) picked on the radar, and where it was last seen
    private java.util.UUID trackingSubLevel; // null: not chasing a build
    private Vec3 subLevelHint = Vec3.ZERO;

    /** backport: chase this airship instead of looking for missiles. */
    public void trackSubLevel(java.util.UUID subLevel, Vec3 seenAt) {
        this.trackingSubLevel = subLevel;
        this.subLevelHint = seenAt;
    }

    public EntityMissileAntiBallistic(
            EntityType<? extends EntityMissileAntiBallistic> type, Level level) {
        super(type, level);

        this.setDeltaMovement(0.0D, baseSpeed, 0.0D);
    }

    @Override
    protected double motionMult() {
        return this.velocity;
    }

    @Override
    public boolean doesImpactEntities() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {

        Vec3 prePos = this.position();
        super.tick();

        if (!this.level().isClientSide()) {

            if (this.velocity < 6) this.velocity += 0.1;

            if (this.activationTimer < 40) {
                this.activationTimer++;
                this.setDeltaMovement(
                        this.getDeltaMovement().x, baseSpeed, this.getDeltaMovement().z);
            } else if (this.trackingSubLevel != null) {
                // backport: radar-assigned airship; if it's lost, fall back to hunting missiles
                if (!this.chaseSubLevel()) this.trackingSubLevel = null;
            } else {
                Entity prevTracking = this.tracking;

                if (this.tracking == null || !this.tracking.isAlive()) this.targetMissile();

                if (prevTracking == null && this.tracking != null) {
                    ExplosionLarge.spawnShock(
                            this.level(), this.getX(), this.getY(), this.getZ(), 24, 3F);
                }
                if (this.tracking != null && this.tracking.isAlive()) {
                    this.aimAtTarget();
                } else {
                    if (this.tickCount > 600) this.discard();
                }
            }

            this.loadNeighboringChunks(
                    (int) Math.floor(this.getX() / 16D), (int) Math.floor(this.getZ() / 16D));

            if (this.getY() > 2000 && (this.tracking == null || !this.tracking.isAlive()))
                this.discard();

        } else {

            Vec3 step = this.position().subtract(prePos);
            if (step.lengthSqr() > 1.0e-8) {
                Vec3 vec = step.normalize();
                ClientEffects.spawnContrail(
                        this.level(),
                        this.getX() - vec.x,
                        this.getY() - vec.y,
                        this.getZ() - vec.z,
                        ClientEffects.Contrail.ABM);
            }
        }

        this.updateFlightRotation();
    }

    protected void targetMissile() {

        if (!(this.level() instanceof ServerLevel server)) return;

        Entity closest = null;
        double dist = 1_000;

        for (Entity e : server.getAllEntities()) {
            if (!(e instanceof EntityMissileBaseNT)) continue;
            if (e instanceof EntityMissileStealth) continue;

            Vec3 vec =
                    new Vec3(
                            e.getX() - this.getX(), e.getY() - this.getY(), e.getZ() - this.getZ());

            if (vec.length() < dist) {
                closest = e;
                // backport-fix: BF-023 remember the distance, otherwise this picks the last missile
                // within 1000 blocks instead of the closest one (same slip in 1.7.10 and NEXT)
                dist = vec.length();
            }
        }

        this.tracking = closest;
    }

    /**
     * backport: steer at the airship's bounding box with the same lead as for missiles (its speed is
     * the pose change since last tick) and go off when within 6 blocks of its hull box.
     *
     * @return false once the build can't be found any more
     */
    protected boolean chaseSubLevel() {
        dev.ryanhcode.sable.companion.SubLevelAccess sub =
                com.hbm.backport.SubLevelSpace.find(level(), trackingSubLevel, subLevelHint, 256);
        if (sub == null) return false;

        dev.ryanhcode.sable.companion.math.BoundingBox3dc box = sub.boundingBox();
        Vec3 center =
                new Vec3(
                        (box.minX() + box.maxX()) * 0.5D,
                        (box.minY() + box.maxY()) * 0.5D,
                        (box.minZ() + box.maxZ()) * 0.5D);
        this.subLevelHint = center;

        org.joml.Vector3dc now = sub.logicalPose().position();
        org.joml.Vector3dc last = sub.lastPose().position();
        Vec3 shipVelocity = new Vec3(now.x() - last.x(), now.y() - last.y(), now.z() - last.z());

        double dx = Math.max(Math.max(box.minX() - getX(), 0), getX() - box.maxX());
        double dy = Math.max(Math.max(box.minY() - getY(), 0), getY() - box.maxY());
        double dz = Math.max(Math.max(box.minZ() - getZ(), 0), getZ() - box.maxZ());
        if (dx * dx + dy * dy + dz * dz < 36D) {
            this.discard();
            ExplosionLarge.explode(
                    this.level(), this.getX(), this.getY(), this.getZ(), 15F, true, false, false);
            return true;
        }

        double intercept = center.distanceTo(position()) / (baseSpeed * this.velocity);
        Vec3 predicted = center.add(shipVelocity.scale(intercept));
        Vec3 motion = predicted.subtract(position()).normalize();
        this.setDeltaMovement(motion.x * baseSpeed, motion.y * baseSpeed, motion.z * baseSpeed);
        return true;
    }

    protected void aimAtTarget() {

        Vec3 delta =
                new Vec3(
                        tracking.getX() - this.getX(),
                        tracking.getY() - this.getY(),
                        tracking.getZ() - this.getZ());
        double intercept = delta.length() / (baseSpeed * this.velocity);
        Vec3 predicted =
                new Vec3(
                        tracking.getX() + (tracking.getX() - tracking.xOld) * intercept,
                        tracking.getY() + (tracking.getY() - tracking.yOld) * intercept,
                        tracking.getZ() + (tracking.getZ() - tracking.zOld) * intercept);
        Vec3 motion =
                new Vec3(
                                predicted.x - this.getX(),
                                predicted.y - this.getY(),
                                predicted.z - this.getZ())
                        .normalize();

        if (delta.length() < 10 && this.activationTimer >= 40) {
            this.discard();
            ExplosionLarge.explode(
                    this.level(), this.getX(), this.getY(), this.getZ(), 15F, true, false, false);
        }

        this.setDeltaMovement(motion.x * baseSpeed, motion.y * baseSpeed, motion.z * baseSpeed);
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (!this.level().isClientSide() && this.activationTimer >= 40) {
            this.discard();
            ExplosionLarge.explode(
                    this.level(), this.getX(), this.getY(), this.getZ(), 20F, true, false, false);
        }
    }

    @Override
    public double getGravityVelocity() {
        return 0.0D;
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected float getWaterDrag() {
        return 1F;
    }

    private void updateFlightRotation() {
        Vec3 motion = this.getDeltaMovement();

        float f2 = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motion.y, f2) * 180.0D / Math.PI) - 90F;
        for (; pitch - this.xRotO < -180.0F; this.xRotO -= 360.0F)
            ;
        while (pitch - this.xRotO >= 180.0F) this.xRotO += 360.0F;
        while (yaw - this.yRotO < -180.0F) this.yRotO -= 360.0F;
        while (yaw - this.yRotO >= 180.0F) this.yRotO += 360.0F;
        this.xRot = pitch;
        this.yRot = yaw;
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.MISSILE_AB;
    }

    @Override
    public String getRadarName() {
        return "radar.target.abm";
    }

    @Override
    public int getBlipLevel() {
        return IRadarDetectableNT.TIER_AB;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanMissiles;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.velocity = input.getDoubleOr("veloc", this.velocity);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putDouble("veloc", this.velocity);
    }

    public void loadNeighboringChunks(int newChunkX, int newChunkZ) {
        if (this.level() instanceof ServerLevel server) {
            ChunkUtil.holdForEntity(
                    server, new ChunkPos(newChunkX, newChunkZ), ChunkUtil.HOLD_RADIUS + 1);
        }
    }
}
