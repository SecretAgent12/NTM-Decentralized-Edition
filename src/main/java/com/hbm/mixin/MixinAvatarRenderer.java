// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.packet.toclient.PlayerAppearancePayload;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport: 26.x AvatarRenderer is 1.21.1 PlayerRenderer. 26.x copied the appearance flags into the
 * AvatarRenderState during extraction and LivingEntityRenderer.submit skipped STEALTH avatars
 * (model, layers and name tag; shadow and fire are drawn by the dispatcher). 1.21.1 renders from the
 * entity, so PlayerRenderer.render reads the flags directly and is skipped as a whole. The
 * ManlyPlayerLayer is added by CoreClientHooks (EntityRenderersEvent.AddLayers); the cape part of
 * the 26.x extraction lives in MixinAvatarRenderState (AbstractClientPlayer#getSkin).
 *
 * <p>backport: the 26.x ARMOR_FLIGHT extraction (render a prone armor flyer as fall flying: isFallFlying,
 * fallFlyingTimeInTicks 10, not crouching, walkAnimationSpeed 0, capeLean 0, pose FALL_FLYING) is
 * applied where 1.21.1 reads those values from the player: PlayerRenderer.setupRotations (fall flying
 * and its ticks), setModelProperties / getRenderOffset (crouching) and LivingEntityRenderer.render
 * (walk animation speed, MixinLivingEntityRenderer). Not ported: HumanoidModel.setupAnim's own
 * getFallFlyingTicks read (limb swing), CapeLayer's lean and the FALL_FLYING pose. The 26.x rider tilt
 * extraction is read from the vehicle in MixinLivingEntityRenderer.
 */
@Mixin(PlayerRenderer.class)
public abstract class MixinAvatarRenderer {

    @Inject(
            method =
                    "render(Lnet/minecraft/client/player/AbstractClientPlayer;FF"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$hideStealthPlayer(
            AbstractClientPlayer player,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo ci) {
        byte appearance =
                player instanceof LocalPlayer local
                        ? PlayerAppearancePayload.fromPlayer(local)
                        : player.hbm$appearance();
        if ((appearance & PlayerAppearance.STEALTH) != 0) ci.cancel();
    }

    @Unique
    private static boolean hbm$armorFlight(AbstractClientPlayer player) {
        byte appearance =
                player instanceof LocalPlayer local
                        ? PlayerAppearancePayload.fromPlayer(local)
                        : player.hbm$appearance();
        return (appearance & PlayerAppearance.ARMOR_FLIGHT) != 0 && !player.isFallFlying();
    }

    @WrapOperation(
            method =
                    "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/AbstractClientPlayer;isFallFlying()Z"))
    private boolean hbm$armorFlightFallFlying(
            AbstractClientPlayer player, Operation<Boolean> original) {
        return original.call(player) || hbm$armorFlight(player);
    }

    @WrapOperation(
            method =
                    "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;"
                            + "Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/AbstractClientPlayer;getFallFlyingTicks()I"))
    private int hbm$armorFlightTicks(AbstractClientPlayer player, Operation<Integer> original) {
        return hbm$armorFlight(player) ? 10 : original.call(player);
    }

    @WrapOperation(
            method = {
                "setModelProperties(Lnet/minecraft/client/player/AbstractClientPlayer;)V",
                "getRenderOffset(Lnet/minecraft/client/player/AbstractClientPlayer;F)Lnet/minecraft/world/phys/Vec3;"
            },
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/AbstractClientPlayer;isCrouching()Z"))
    private boolean hbm$armorFlightCrouching(
            AbstractClientPlayer player, Operation<Boolean> original) {
        return original.call(player) && !hbm$armorFlight(player);
    }
}
