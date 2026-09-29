// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * backport: 26.x keeps the FOV on the Camera (calculateHudFov / tickFov); 1.21.1 computes it in
 * GameRenderer: getFov(camera, partialTick, false) is the hand/HUD FOV (26.x calculateHudFov) and
 * tickFov() reads the player's FOV modifier.
 */
@Mixin(GameRenderer.class)
public class MixinCamera {

    @ModifyReturnValue(
            method = "getFov(Lnet/minecraft/client/Camera;FZ)D",
            at = @At("RETURN"))
    private double hbm$gunModelFov(
            double fov, Camera camera, float partialTicks, boolean useFovSetting) {
        return useFovSetting ? fov : ItemRenderWeaponBase.adjustHudFov((float) fov);
    }

    @ModifyExpressionValue(
            method = "tickFov()V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/AbstractClientPlayer;getFieldOfViewModifier()F"))
    private float hbm$gunViewFov(float modifier) {
        return ItemRenderWeaponBase.adjustViewFov(modifier);
    }
}
