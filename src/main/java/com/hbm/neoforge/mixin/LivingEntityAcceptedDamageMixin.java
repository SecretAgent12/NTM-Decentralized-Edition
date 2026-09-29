// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.mixin;

import com.hbm.util.EntityDamageUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({LivingEntity.class, Player.class})
abstract class LivingEntityAcceptedDamageMixin {

    // backport: 1.21.1 actuallyHurt(DamageSource, float) (26.x adds a leading ServerLevel); both
    // LivingEntity and Player read the amount from the damage container, as in 26.x.
    @ModifyVariable(
            method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private float hbm$acceptedDamage(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isInvulnerableTo(source)) return amount;
        float result = EntityDamageUtil.modifyAcceptedDamage(self, source, amount);
        self.damageContainers.peek().setNewDamage(result);
        return result;
    }
}
