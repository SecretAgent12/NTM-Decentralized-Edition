// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.backport.client.itemmodel.ClientItems;
import com.hbm.backport.client.itemmodel.GroundLift;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * backport-fix: BF-054 dropped items with a 3D model (machines, weapons, everything drawn through
 * the 26.x item definitions) sank into the ground at the low point of their bob.
 *
 * <p>1.21.1 lifts a dropped item by a fixed {@code 0.25 * ground scale}, fine for flat icons and
 * plain blocks. 26.x (where NEXT's models are made) lifts it by the model's real bounds, so its
 * lowest point sits 1/16 above the ground. For every 3D item (gui-3d model) that has a 26.x item
 * definition (machines as block models, weapons and others through special renderers) the bob is
 * kept and the 1.21.1 lift is swapped for the 26.x one (GroundLift); flat icons stay vanilla.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer {

    @WrapOperation(
            method =
                    "render(Lnet/minecraft/world/entity/item/ItemEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
                            ordinal = 0))
    private void hbm$groundLift(
            PoseStack pose,
            float x,
            float y,
            float z,
            Operation<Void> original,
            @Local(argsOnly = true) ItemEntity entity,
            @Local BakedModel model) {
        ItemStack stack = entity.getItem();
        if (model.isGui3d() && ClientItems.hasDefinition(stack.getItem())) {
            float vanillaLift =
                    0.25F * model.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
            float bob = y - vanillaLift;
            y = bob + GroundLift.of(stack, entity.level(), entity.getId());
        }
        original.call(pose, x, y, z);
    }
}
