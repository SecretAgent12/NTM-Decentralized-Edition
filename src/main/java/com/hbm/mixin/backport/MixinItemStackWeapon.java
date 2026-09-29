// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.backport;

import com.hbm.backport.item.tool.ToolRegistry;
import com.hbm.backport.item.tool.Weapon;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.x Weapon component semantics on 1.21.1 ItemStack (component hbm:backport_weapon):
 * hurtEnemy counts as a hit when the component is present (26.x: {@code has(WEAPON)}), and
 * postHurtEnemy wears the stack by {@code itemDamagePerAttack} after the item's own hook.
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStackWeapon {

    @Inject(method = "hurtEnemy", at = @At("RETURN"), cancellable = true)
    private void backport$weaponHit(LivingEntity target, Player attacker, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        ItemStack self = (ItemStack) (Object) this;
        if (ToolRegistry.get(self, ToolRegistry.WEAPON) != null) {
            attacker.awardStat(Stats.ITEM_USED.get(self.getItem()));
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "postHurtEnemy", at = @At("TAIL"))
    private void backport$weaponWear(LivingEntity target, Player attacker, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        Weapon weapon = ToolRegistry.get(self, ToolRegistry.WEAPON);
        if (weapon != null && weapon.itemDamagePerAttack() > 0) {
            self.hurtAndBreak(weapon.itemDamagePerAttack(), attacker, EquipmentSlot.MAINHAND);
        }
    }
}
