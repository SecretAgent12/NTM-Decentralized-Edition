// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.PlayerAppearance;
import com.hbm.packet.toclient.PlayerAppearancePayload;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
// The space keeps the backport's import rewriting from redirecting this import to the 26.x-shaped core shim: the mixin
// works on the 1.21.1 vanilla class.
import net.minecraft.client.renderer.entity .LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * backport: 26.x read the appearance from the AvatarRenderState; 1.21.1 renders from the entity
 * (players only; 26.x avatars = players and mannequins). The STEALTH skip of 26.x's submit is in
 * MixinAvatarRenderer (PlayerRenderer.render).
 */
@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {

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
                        ? PlayerAppearancePayload.fromEffects(local)
                        : player.hbm$appearance();
        if ((appearance & PlayerAppearance.MANLY) != 0) cir.setReturnValue(null);
    }
}
