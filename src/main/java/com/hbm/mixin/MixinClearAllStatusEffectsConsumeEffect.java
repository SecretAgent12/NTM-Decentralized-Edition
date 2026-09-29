// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.potion.UncurableEffectInstance;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.neoforged.neoforge.common.EffectCure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// backport: 26.x milk is a consumable with ClearAllStatusEffectsConsumeEffect, which this mixin hooked. 1.21.1 milk is
// MilkBucketItem.finishUsingItem -> LivingEntity.removeEffectsCuredBy(EffectCures.MILK); the same override now wraps
// that call (class name kept so the mixin config entry stays valid).
@Mixin(MilkBucketItem.class)
public abstract class MixinClearAllStatusEffectsConsumeEffect {

    @WrapOperation(
            method = "finishUsingItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;removeEffectsCuredBy(Lnet/neoforged/neoforge/common/EffectCure;)Z"))
    private boolean hbm$preserveUncurableEffects(
            LivingEntity entity,
            EffectCure cure,
            Operation<Boolean> original,
            @Local(argsOnly = true) ItemStack stack) {
        if (!stack.is(Items.MILK_BUCKET)) return original.call(entity, cure);
        if (entity.getActiveEffects().stream().noneMatch(UncurableEffectInstance.class::isInstance))
            return original.call(entity, cure);
        boolean removed = false;
        for (MobEffectInstance effect : List.copyOf(entity.getActiveEffects())) {
            if (!(effect instanceof UncurableEffectInstance))
                removed |= entity.removeEffect(effect.getEffect());
        }
        return removed;
    }
}
