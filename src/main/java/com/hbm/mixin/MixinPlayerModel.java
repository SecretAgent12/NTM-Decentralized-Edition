// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.render.ArmorWorldRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// backport: 1.21.1 PlayerModel.setupAnim takes the entity (26.x: AvatarRenderState); the
// equipment comes from its slots. PlayerRenderer sets the part visibility before setupAnim runs.
@Mixin(PlayerModel.class)
public abstract class MixinPlayerModel {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void hbm$hideHatUnderObjHelmet(
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci) {
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        if (ArmorWorldRenderer.hidesSkin(entity.getItemBySlot(EquipmentSlot.HEAD)))
            model.hat.visible = false;
        if (ArmorWorldRenderer.hidesSkin(entity.getItemBySlot(EquipmentSlot.CHEST))) {
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }
        if (ArmorWorldRenderer.hidesSkin(entity.getItemBySlot(EquipmentSlot.LEGS))
                || ArmorWorldRenderer.hidesSkin(entity.getItemBySlot(EquipmentSlot.FEET))) {
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }
    }
}
