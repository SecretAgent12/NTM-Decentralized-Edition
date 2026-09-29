// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.item.food.Consumable;
import com.hbm.backport.item.food.ConsumableRegistry;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sounds and particles while consuming a backport-consumable stack, as 26.x Consumable.emitParticlesAndSounds
 * (the component's sound / animation / hasConsumeParticles instead of 1.21.1's fixed per-UseAnim effects).
 * 1.21.1 also plays the effects once more (16 particles) right before finishUsingItem; in 26.x that burst is
 * part of Consumable.onConsume (so items overriding finishUsingItem decide themselves), hence it is skipped here.
 */
@Mixin(LivingEntity.class)
public abstract class MixinBackportConsumableLiving {

    @Inject(method = "triggerItemUseEffects", at = @At("HEAD"), cancellable = true)
    private void backport$consumableUseEffects(ItemStack stack, int amount, CallbackInfo ci) {
        Consumable consumable = stack.get(ConsumableRegistry.CONSUMABLE);
        if (consumable == null) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!stack.isEmpty() && self.isUsingItem())
            consumable.emitParticlesAndSounds(self.getRandom(), self, stack, amount);
        ci.cancel();
    }

    @WrapWithCondition(
            method = "completeUsingItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;triggerItemUseEffects(Lnet/minecraft/world/item/ItemStack;I)V"))
    private boolean backport$skipFinishBurst(LivingEntity self, ItemStack stack, int amount) {
        return !stack.has(ConsumableRegistry.CONSUMABLE);
    }
}
