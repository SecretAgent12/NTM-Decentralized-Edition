// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.client.core.HumanoidRenderState;
import com.hbm.client.render.MotionRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport: targets the core shim of the 26.x HumanoidRenderState (1.21.1 has none). 26.x filled
 * the motion in EntityRenderer#createRenderState (MixinEntityRenderer); in 1.21.1 every humanoid
 * extraction (bridged renderers and HumanoidRenderState.of for vanilla players' armor layer) goes
 * through the shim's static extractHumanoid, so it is filled there.
 */
@Mixin(HumanoidRenderState.class)
public abstract class MixinHumanoidRenderState implements MotionRenderState {
    @Unique private boolean hbm$onGround;
    @Unique private double hbm$motionY;

    @Override
    public boolean hbm$onGround() {
        return hbm$onGround;
    }

    @Override
    public double hbm$motionY() {
        return hbm$motionY;
    }

    @Override
    public void hbm$setMotion(boolean onGround, double motionY) {
        hbm$onGround = onGround;
        hbm$motionY = motionY;
    }

    @Inject(
            method =
                    "extractHumanoid(Lnet/minecraft/world/entity/LivingEntity;Lcom/hbm/backport/client/core/HumanoidRenderState;F)V",
            at = @At("RETURN"),
            remap = false)
    private static void hbm$extractMotion(
            LivingEntity entity, HumanoidRenderState state, float partialTicks, CallbackInfo ci) {
        // 26.x: entity.isClientAuthoritative() && !entity.isLocalInstanceAuthoritative() -- a
        // client-driven entity (player, or a vehicle a player controls) simulated by another client,
        // whose delta movement is not synced. backport: unverified: 1.21.1 equivalent below
        boolean clientDriven =
                entity instanceof Player || entity.getControllingPassenger() instanceof Player;
        double motionY =
                clientDriven && !entity.isControlledByLocalInstance()
                        ? entity.getY() - entity.yOld
                        : entity.getDeltaMovement().y;
        ((MotionRenderState) state).hbm$setMotion(entity.onGround(), motionY);
    }
}
