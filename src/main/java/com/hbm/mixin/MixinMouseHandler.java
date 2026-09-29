// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.qmaw.QMAWClient;
import com.hbm.wiaj.CanneryClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import com.hbm.backport.client.gui.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {

    // backport: 1.21.1 calls Screen#mouseClicked(double, double, int) inside onPress's
    // screen-error lambda (lambda$onPress$0 = press; $1 = release); 26.x passed a MouseButtonEvent
    // and a double-click flag (1.21.1 has none: the tree's handlers get a plain click)
    @WrapOperation(
            method = "lambda$onPress$0([ZLnet/minecraft/client/gui/screens/Screen;DDI)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"))
    private static boolean hbm$manualMouse(
            Screen screen, double x, double y, int button, Operation<Boolean> original) {
        MouseButtonEvent event = new MouseButtonEvent(x, y, button);
        return CanneryClient.mouse(screen, event)
                || QMAWClient.mouse(screen, event)
                || original.call(screen, x, y, button);
    }
}
