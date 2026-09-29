// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.client.VanishedEntities;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
// The space keeps the backport's import rewriting from redirecting this import to the 26.x-shaped core shim.
import net.minecraft.client.renderer.entity .EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * backport: 26.x skips EntityRenderer#submit for a vanished state inside the dispatcher's submit;
 * 1.21.1's dispatcher render calls EntityRenderer#render(entity, ...) at the same point (shadow,
 * fire and hitbox are still drawn around it, as in 26.x).
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {

    @WrapWithCondition(
            method =
                    "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/entity/EntityRenderer;render("
                                            + "Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;"
                                            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private boolean hbm$skipVanished(
            EntityRenderer<?> renderer,
            Entity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        return !VanishedEntities.isVanished(entity);
    }
}
