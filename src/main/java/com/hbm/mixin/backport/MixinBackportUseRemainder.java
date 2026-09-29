// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.item.food.ConsumableRegistry;
import com.hbm.backport.item.food.UseRemainder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 26.x ItemStack.finishUsingItem: converts a used-up stack into its use remainder (stew -> bowl). */
@Mixin(ItemStack.class)
public abstract class MixinBackportUseRemainder {

    @WrapOperation(
            method = "finishUsingItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/Item;finishUsingItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack backport$useRemainder(
            Item item, ItemStack stack, Level level, LivingEntity entity, Operation<ItemStack> original) {
        if (!stack.has(ConsumableRegistry.USE_REMAINDER)) return original.call(item, stack, level, entity);
        ItemStack before = stack.copy();
        return UseRemainder.applyAfterUse(before, original.call(item, stack, level, entity), entity);
    }
}
