// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.items.weapon.sedna.AkimboGhost;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.x MixinLivingEntity#hbm$discardAkimboGhost hooks LivingEntity.drop(ItemStack, boolean,
 * boolean). In 1.21.1 that method is declared on Player (ServerPlayer overrides it and calls
 * super first, returning null when super does), so the same HEAD cancel lives here.
 */
@Mixin(Player.class)
public abstract class MixinsCommonPlayerDrop {

    @Inject(
            method =
                    "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$discardAkimboGhost(
            ItemStack itemStack,
            boolean randomly,
            boolean thrownFromHand,
            CallbackInfoReturnable<ItemEntity> cir) {
        if (AkimboGhost.isGhost(itemStack)) cir.setReturnValue(null);
    }
}
