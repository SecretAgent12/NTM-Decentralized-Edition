// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.packet.toclient.PlayerAppearancePayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
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
                        ? PlayerAppearancePayload.fromEffects(local)
                        : player.hbm$appearance();
        if ((appearance & PlayerAppearance.STEALTH) != 0) ci.cancel();
    }
}
