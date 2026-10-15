// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.Keybinds;
import com.hbm.inventory.fluid.NTMFluidProperties;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    // backport: 1.21.1 disconnect(Screen, boolean keepResourcePacks); 26.x disconnect(Screen, boolean, boolean)
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("TAIL"))
    private void hbm$releaseFluidProperties(CallbackInfo ci) {
        NTMFluidProperties.onClientDisconnect();
    }

    @Inject(method = "startAttack()Z", at = @At("HEAD"), cancellable = true)
    private void hbm$gunSuppressAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Keybinds.isGunHeld()
                && Keybinds.isGunKey(Minecraft.getInstance().options.keyAttack)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack(Z)V", at = @At("HEAD"), cancellable = true)
    private void hbm$gunSuppressContinueAttack(boolean down, CallbackInfo ci) {
        if (Keybinds.isGunHeld()
                && Keybinds.isGunKey(Minecraft.getInstance().options.keyAttack)) {
            ci.cancel();
        }
    }

    // backport: 26.x pickBlockOrEntity is 1.21.1 pickBlock (middle click, blocks and entities)
    @Inject(method = "pickBlock()V", at = @At("HEAD"), cancellable = true)
    private void hbm$gunSuppressPick(CallbackInfo ci) {
        if (Keybinds.isGunHeld()
                && Keybinds.isGunKey(Minecraft.getInstance().options.keyPickItem)) {
            ci.cancel();
        }
    }
}
