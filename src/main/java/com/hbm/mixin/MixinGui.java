// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.qmaw.QMAWClient;
import com.hbm.wiaj.CanneryClient;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// backport: 26.x Gui#extractRenderState is 1.21.1 Gui#render(GuiGraphics, DeltaTracker)
@Mixin(Gui.class)
public abstract class MixinGui {

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"))
    private void hbm$beginManualHover(CallbackInfo ci) {
        QMAWClient.beginFrame();
        CanneryClient.beginFrame();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("TAIL"))
    private void hbm$endManualHover(CallbackInfo ci) {
        QMAWClient.endFrame();
        CanneryClient.endFrame();
    }
}
