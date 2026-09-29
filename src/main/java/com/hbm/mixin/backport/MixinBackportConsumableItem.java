// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.item.food.Consumable;
import com.hbm.backport.item.food.ConsumableRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.x Item behaviour for the consumable component (backport {@code hbm:backport_consumable}): the base
 * implementations of use / finishUsingItem / getUseDuration / getUseAnimation consult it first, as the 26.x
 * vanilla ones do. Only acts on stacks carrying the component; everything else (including plain 1.21.1 food) keeps
 * the 1.21.1 behaviour. Subclasses that override these without calling super bypass it, as in 26.x.
 */
@Mixin(Item.class)
public abstract class MixinBackportConsumableItem {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void backport$consumableUse(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);
        Consumable consumable = stack.get(ConsumableRegistry.CONSUMABLE);
        if (consumable != null) cir.setReturnValue(consumable.startConsuming(player, stack, hand));
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void backport$consumableFinish(
            ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        Consumable consumable = stack.get(ConsumableRegistry.CONSUMABLE);
        if (consumable != null) cir.setReturnValue(consumable.onConsume(level, entity, stack));
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void backport$consumableDuration(
            ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        Consumable consumable = stack.get(ConsumableRegistry.CONSUMABLE);
        if (consumable != null) cir.setReturnValue(consumable.consumeTicks());
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void backport$consumableAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
        Consumable consumable = stack.get(ConsumableRegistry.CONSUMABLE);
        if (consumable != null) cir.setReturnValue(consumable.animation());
    }
}
