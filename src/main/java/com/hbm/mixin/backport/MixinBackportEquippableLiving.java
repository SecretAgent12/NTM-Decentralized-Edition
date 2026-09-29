// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.item.armor.Equippable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.x LivingEntity.getEquipmentSlotForItem: the Equippable component decides the slot. With this, items
 * carrying the backport component (armor and non-armor such as jetpacks) go into their slot through the
 * armor slots (IItemExtension.canEquip), shift-click, dispensers and mob equipment.
 */
@Mixin(LivingEntity.class)
public abstract class MixinBackportEquippableLiving {

    @Shadow
    public abstract boolean canUseSlot(EquipmentSlot slot);

    @Inject(method = "getEquipmentSlotForItem", at = @At("HEAD"), cancellable = true)
    private void backport$equippableSlot(ItemStack stack, CallbackInfoReturnable<EquipmentSlot> cir) {
        Equippable equippable = stack.get(Equippable.TYPE);
        if (equippable != null
                && equippable.canBeEquippedBy(((LivingEntity) (Object) this).getType())
                && canUseSlot(equippable.slot())) {
            cir.setReturnValue(equippable.slot());
        }
    }
}
