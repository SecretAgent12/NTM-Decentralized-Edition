// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.qmaw.QMAWClient;
import com.hbm.wiaj.CanneryClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import com.hbm.backport.client.gui.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardHandler {

    // backport: 1.21.1 calls Screen#keyPressed(int, int, int) inside keyPress's screen-error lambda
    // (lambda$keyPress$5, between NeoForge's ScreenEvent.KeyPressed Pre and Post), not with a KeyEvent
    @WrapOperation(
            method = "lambda$keyPress$5(ILnet/minecraft/client/gui/screens/Screen;[ZIII)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(III)Z"))
    private static boolean hbm$manualKey(
            Screen screen, int key, int scancode, int modifiers, Operation<Boolean> original) {
        KeyEvent event = new KeyEvent(key, scancode, modifiers);
        return CanneryClient.key(screen, event)
                || QMAWClient.key(screen, event)
                || original.call(screen, key, scancode, modifiers);
    }
}
