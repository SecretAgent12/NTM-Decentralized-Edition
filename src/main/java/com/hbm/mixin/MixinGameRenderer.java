// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.qmaw.QMAWClient;
import com.hbm.wiaj.CanneryClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport-fix: BF-057 the manual (QMAW, F1) and the canneries (Cannery) never opened: the key did
 * nothing over an item whose tooltip offered it.
 *
 * <p>Both remember the item under the cursor while its tooltip is built inside a "frame", and the
 * key only works for an item remembered that frame. The frame used to be 1.21.1 {@code
 * Gui#render} (the 26.x port mapped {@code Gui#extractRenderState} onto it), but in 1.21.1 that
 * only draws the HUD: screens and their tooltips are drawn afterwards by {@code
 * GameRenderer#render}, outside it, so nothing was ever remembered. The frame now spans the whole
 * {@code GameRenderer#render}.
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void hbm$beginManualHover(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        QMAWClient.beginFrame();
        CanneryClient.beginFrame();
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("TAIL"))
    private void hbm$endManualHover(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        QMAWClient.endFrame();
        CanneryClient.endFrame();
    }
}
