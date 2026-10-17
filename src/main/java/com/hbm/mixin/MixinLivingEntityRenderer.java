// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.packet.toclient.PlayerAppearancePayload;
import com.hbm.render.util.RiderTilt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
// The space keeps the backport's import rewriting from redirecting this import to the 26.x-shaped core shim: the mixin
// works on the 1.21.1 vanilla class.
import net.minecraft.client.renderer.entity .LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * backport: 26.x read the appearance from the AvatarRenderState; 1.21.1 renders from the entity
 * (players only; 26.x avatars = players and mannequins). The STEALTH skip of 26.x's submit is in
 * MixinAvatarRenderer (PlayerRenderer.render). The 26.x rider tilt (extracted into the
 * AvatarRenderState, applied after submit's first pushPose) is read from the vehicle here, after
 * 1.21.1 render's first pushPose; the ARMOR_FLIGHT walkAnimationSpeed 0 of the 26.x extraction is
 * applied to render's walk animation speed read.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {

    @Inject(
            method =
                    "render(Lnet/minecraft/world/entity/LivingEntity;FF"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
                            ordinal = 0,
                            shift = At.Shift.AFTER))
    private void hbm$tiltRider(
            LivingEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo ci) {
        if (!(entity instanceof Player)) return;
        Entity vehicle = entity.getVehicle();
        float tilt = vehicle == null ? 0F : RiderTilt.POINT.get().degrees(vehicle, partialTicks);
        if (tilt == 0F) return;
        poseStack.translate(0F, RiderTilt.PIVOT, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(tilt));
        poseStack.translate(0F, -RiderTilt.PIVOT, 0F);
    }

    @WrapOperation(
            method =
                    "render(Lnet/minecraft/world/entity/LivingEntity;FF"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/WalkAnimationState;speed(F)F"))
    private float hbm$armorFlightWalkSpeed(
            WalkAnimationState walk,
            float partialTicks,
            Operation<Float> original,
            @Local(argsOnly = true) LivingEntity entity) {
        if (entity instanceof AbstractClientPlayer player && !player.isFallFlying()) {
            byte appearance =
                    player instanceof LocalPlayer local
                            ? PlayerAppearancePayload.fromPlayer(local)
                            : player.hbm$appearance();
            if ((appearance & PlayerAppearance.ARMOR_FLIGHT) != 0) return 0F;
        }
        return original.call(walk, partialTicks);
    }

    @Inject(
            method =
                    "getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$replaceManlySkin(
            LivingEntity entity,
            boolean bodyVisible,
            boolean forceTransparent,
            boolean glowing,
            CallbackInfoReturnable<RenderType> cir) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        if (player.isSpectator()) return;
        byte appearance =
                player instanceof LocalPlayer local
                        ? PlayerAppearancePayload.fromPlayer(local)
                        : player.hbm$appearance();
        if ((appearance & PlayerAppearance.MANLY) != 0) cir.setReturnValue(null);
    }
}
