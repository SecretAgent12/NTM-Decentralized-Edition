// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.item.armor.Equippable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.x Item.use: an item with a swappable Equippable component is equipped on right click. 1.21.1 only
 * does this for Equipable items (ArmorItem, which ArmorItem26 is); this covers the other items carrying
 * the backport component (jetpacks, wings). Subclasses that override use without super bypass it, as in 26.x.
 */
@Mixin(Item.class)
public abstract class MixinBackportEquippableItem {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void backport$equippableUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if ((Object) this instanceof Equipable) return;
        ItemStack stack = player.getItemInHand(hand);
        Equippable equippable = stack.get(Equippable.TYPE);
        if (equippable != null && equippable.swappable())
            cir.setReturnValue(equippable.swapWithEquipmentSlot(stack, level, player, hand));
    }
}
