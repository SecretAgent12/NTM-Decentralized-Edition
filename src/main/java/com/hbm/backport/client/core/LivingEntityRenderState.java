// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport.client.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.jspecify.annotations.Nullable;

/** 26.x {@code net.minecraft.client.renderer.entity.state.LivingEntityRenderState}. */
public class LivingEntityRenderState extends EntityRenderState {
    public float bodyRot;
    public float yRot;
    public float xRot;
    public float deathTime;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float scale = 1.0F;
    public float ageScale = 1.0F;
    public boolean isUpsideDown;
    public boolean isFullyFrozen;
    public boolean isBaby;
    public boolean isInWater;
    public boolean isAutoSpinAttack;
    public boolean hasRedOverlay;
    public boolean isInvisibleToPlayer;
    public @Nullable Direction bedOrientation;
    public @Nullable Component customName;
    public Pose pose = Pose.STANDING;

    public boolean hasPose(Pose pose) {
        return this.pose == pose;
    }

    /** The 26.x LivingEntityRenderer extraction of the fields above (partial ticks applied). */
    public static void extractLiving(LivingEntity entity, LivingEntityRenderState state, float pt) {
        float headRot = Mth.rotLerp(pt, entity.yHeadRotO, entity.yHeadRot);
        float bodyRot = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        Entity vehicle = entity.getVehicle();
        if (entity.isPassenger() && vehicle instanceof LivingEntity living) {
            // 1.21.1 LivingEntityRenderer.render: body follows the mount within 85 degrees of the head
            bodyRot = Mth.rotLerp(pt, living.yBodyRotO, living.yBodyRot);
            float diff = Mth.clamp(Mth.wrapDegrees(headRot - bodyRot), -85.0F, 85.0F);
            bodyRot = headRot - diff;
            if (diff * diff > 2500.0F) bodyRot += diff * 0.2F;
        }
        state.bodyRot = bodyRot;
        state.yRot = Mth.wrapDegrees(headRot - bodyRot);
        state.xRot = entity.getViewXRot(pt);
        state.customName = entity.getCustomName();
        state.isUpsideDown = LivingEntityRenderer.isEntityUpsideDown(entity);
        if (state.isUpsideDown) {
            state.xRot *= -1.0F;
            state.yRot *= -1.0F;
        }
        if (!entity.isPassenger() && entity.isAlive()) {
            state.walkAnimationPos = entity.walkAnimation.position(pt);
            state.walkAnimationSpeed = entity.walkAnimation.speed(pt);
        } else {
            state.walkAnimationPos = 0.0F;
            state.walkAnimationSpeed = 0.0F;
        }
        state.scale = entity.getScale();
        state.ageScale = entity.getAgeScale();
        state.pose = entity.getPose();
        state.bedOrientation = entity.getBedOrientation();
        state.deathTime = entity.deathTime > 0 ? entity.deathTime + pt : 0.0F;
        Minecraft mc = Minecraft.getInstance();
        state.isInvisibleToPlayer =
                state.isInvisible && (mc.player == null || entity.isInvisibleTo(mc.player));
        state.hasRedOverlay = entity.hurtTime > 0 || entity.deathTime > 0;
        state.isFullyFrozen = entity.isFullyFrozen();
        state.isBaby = entity.isBaby();
        state.isInWater = entity.isInWater();
        state.isAutoSpinAttack = entity.isAutoSpinAttack();
    }
}
