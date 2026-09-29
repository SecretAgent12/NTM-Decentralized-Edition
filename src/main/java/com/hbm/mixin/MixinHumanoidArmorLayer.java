// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.client.core.HumanoidRenderState;
import com.hbm.backport.client.core.SubmitNodeCollector;
import com.hbm.backport.item.armor.Equippable;
import com.hbm.client.render.ArmorHeadRenderer;
import com.hbm.client.render.ArmorSocketRenderer;
import com.hbm.client.render.ArmorWorldRenderer;
import com.hbm.items.armor.ModArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * backport: 1.21.1 HumanoidArmorLayer renders each slot through renderArmorPiece(pose, buffers,
 * entity, slot, light, armorModel, limbSwing..headPitch) and only draws ArmorItems; it has no
 * static shouldRender filling the render state's equipment. The 26.x shouldRender extension (OBJ
 * head pieces, bare jetpacks/wings, skin-hiding suits) becomes the {@link #hbm$shouldRender}
 * filter of the stack the HEAD hook sees; the 26.x HumanoidRenderState is extracted from the
 * entity and the submissions go straight into the 1.21.1 buffers.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class MixinHumanoidArmorLayer {

    @Unique
    private static boolean hbm$shouldRender(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return false;
        Equippable equippable = Equippable.get(stack);
        // 26.x vanilla: an equippable with an equipment asset for this slot (1.21.1 ArmorItems carry
        // their asset as the ArmorMaterial, so the derived Equippable has no assetId)
        if (equippable != null
                && equippable.slot() == slot
                && (equippable.assetId().isPresent() || stack.getItem() instanceof ArmorItem))
            return true;
        if (slot == EquipmentSlot.HEAD && ArmorHeadRenderer.draws(stack)) return true;
        if (slot == EquipmentSlot.CHEST && ArmorSocketRenderer.drawsBare(stack)) return true;
        return equippable != null && equippable.slot() == slot && ArmorWorldRenderer.hidesSkin(stack);
    }

    @Unique
    private static ItemStack hbm$renderable(LivingEntity entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        return hbm$shouldRender(stack, slot) ? stack : ItemStack.EMPTY;
    }

    @Inject(
            method =
                    "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
                            + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;I"
                            + "Lnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void hbm$renderObjArmor(
            PoseStack pose,
            MultiBufferSource buffers,
            LivingEntity entity,
            EquipmentSlot slot,
            int light,
            HumanoidModel<?> armorModel,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci) {
        ItemStack stack = hbm$renderable(entity, slot);
        HumanoidModel<?> model =
                (HumanoidModel<?>) ((RenderLayer<?, ?>) (Object) this).getParentModel();
        HumanoidRenderState state = HumanoidRenderState.of(entity, partialTick, light);
        // 26.x states carry only the equipment HumanoidArmorLayer.shouldRender accepts
        state.headEquipment = hbm$renderable(entity, EquipmentSlot.HEAD);
        state.chestEquipment = hbm$renderable(entity, EquipmentSlot.CHEST);
        state.legsEquipment = hbm$renderable(entity, EquipmentSlot.LEGS);
        state.feetEquipment = hbm$renderable(entity, EquipmentSlot.FEET);
        SubmitNodeCollector collector = SubmitNodeCollector.immediate(buffers);
        ArmorSocketRenderer.submit(
                pose, collector, light, model, state, slot, stack, state.outlineColor);
        if (ArmorHeadRenderer.submit(
                pose, collector, light, model, state, slot, stack, state.outlineColor)) {
            ci.cancel();
            return;
        }
        if (!(stack.getItem() instanceof ModArmorItem armor)
                || !ArmorWorldRenderer.hidesSkin(stack)) return;
        ArmorWorldRenderer.submit(pose, collector, light, model, state, slot, armor.suit(), stack);
        ci.cancel();
    }
}
